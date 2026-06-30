package com.example.orderservice.service;

import com.example.common.event.OrderCreatedEvent;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.kafka.OrderEventProducer;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.validator.CreateOrderValidator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderEventProducer orderEventProducer;
  private final CreateOrderValidator createOrderValidator;

  /**
   * 주문 생성
   * 
   * 1. 입력값 검증 (기본 검증 + 비즈니스 로직 검증)
   * 2. 주문 생성 및 저장
   * 3. Kafka 이벤트 발행
   * 
   * @param request 주문 생성 요청
   * @return 생성된 주문 정보
   * @throws IllegalArgumentException 검증 실패 시
   */
  @Transactional
  public OrderResponse createOrder(CreateOrderRequest request) {
    // 1. 입력값 검증 (기본 검증은 @Valid에서, 비즈니스 검증은 여기서)
    createOrderValidator.validate(request);
    
    log.debug("Creating order with userId: {}, productId: {}, quantity: {}", 
        request.getUserId(), request.getProductId(), request.getQuantity());

    // 2. 주문 생성
    Order order =
        Order.builder()
            .userId(request.getUserId())
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .totalPrice(
                request
                    .getUnitPrice()
                    .multiply(java.math.BigDecimal.valueOf(request.getQuantity())))
            .status(OrderStatus.PENDING)
            .build();

    Order savedOrder = orderRepository.save(order);
    log.info("Order created successfully: id={}, userId={}, productId={}", 
        savedOrder.getId(), savedOrder.getUserId(), savedOrder.getProductId());

    // 3. Kafka 이벤트 발행 (Product Service에서 재고 차감 처리)
    try {
      OrderCreatedEvent event =
          OrderCreatedEvent.builder()
              .orderId(savedOrder.getId())
              .userId(savedOrder.getUserId())
              .productId(savedOrder.getProductId())
              .quantity(savedOrder.getQuantity())
              .createdAt(LocalDateTime.now())
              .build();
      orderEventProducer.sendOrderCreatedEvent(event);
      log.info("Order event published: orderId={}", savedOrder.getId());
    } catch (Exception e) {
      log.error("Failed to publish order event: orderId={}", savedOrder.getId(), e);
      // 주문은 생성되었으므로, 이벤트 발행 실패만 로깅
      // 나중에 DLQ 패턴으로 개선 필요
    }

    return toResponse(savedOrder);
  }

  @Transactional(readOnly = true)
  public OrderResponse getOrderById(Long id) {
    Order order =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + id));
    return toResponse(order);
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> getOrdersByUserId(Long userId) {
    return orderRepository.findByUserId(userId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> getAllOrders() {
    return orderRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
  }

  @Transactional
  public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
    Order order =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + id));

    order.setStatus(status);
    Order updatedOrder = orderRepository.save(order);
    log.info("Order status updated: {} -> {}", id, status);

    return toResponse(updatedOrder);
  }

  @Transactional
  public OrderResponse cancelOrder(Long id) {
    Order order =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + id));

    if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
      throw new IllegalStateException("배송 중이거나 배송 완료된 주문은 취소할 수 없습니다");
    }

    order.setStatus(OrderStatus.CANCELLED);
    Order cancelledOrder = orderRepository.save(order);
    log.info("Order cancelled: {}", id);

    return toResponse(cancelledOrder);
  }

  private OrderResponse toResponse(Order order) {
    return OrderResponse.builder()
        .id(order.getId())
        .userId(order.getUserId())
        .productId(order.getProductId())
        .quantity(order.getQuantity())
        .totalPrice(order.getTotalPrice())
        .status(order.getStatus().toString())
        .createdAt(order.getCreatedAt())
        .build();
  }
}