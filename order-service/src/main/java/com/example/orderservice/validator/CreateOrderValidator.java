package com.example.orderservice.validator;

import com.example.orderservice.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 주문 생성 요청에 대한 비즈니스 로직 검증
 * 
 * 기본 검증(@NotNull, @Min)을 통과한 후 적용할 추가 검증 규칙
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrderValidator {

  /**
   * 주문 생성 요청 검증
   * 
   * @param request 검증할 주문 생성 요청
   * @throws IllegalArgumentException 검증 실패 시
   */
  public void validate(CreateOrderRequest request) {
    validateUserId(request.getUserId());
    validateProductId(request.getProductId());
    validateQuantity(request.getQuantity());
    validateUnitPrice(request.getUnitPrice());
  }

  /**
   * 사용자 ID 검증
   * - Null 체크 (이미 @NotNull에서 처리되지만 명시적 검증)
   * - 양수 체크
   */
  private void validateUserId(Long userId) {
    if (userId == null) {
      throw new IllegalArgumentException("사용자 ID는 필수입니다");
    }
    if (userId <= 0) {
      throw new IllegalArgumentException("사용자 ID는 양수여야 합니다");
    }
  }

  /**
   * 상품 ID 검증
   * - Null 체크
   * - 양수 체크
   */
  private void validateProductId(Long productId) {
    if (productId == null) {
      throw new IllegalArgumentException("상품 ID는 필수입니다");
    }
    if (productId <= 0) {
      throw new IllegalArgumentException("상품 ID는 양수여야 합니다");
    }
  }

  /**
   * 수량 검증
   * - Null 체크
   * - 최소값 체크 (이미 @Min(1)에서 처리되지만 명시적 검증)
   * - 최대값 체크 (한 번에 1000개 이상 주문 불가)
   */
  private void validateQuantity(Integer quantity) {
    if (quantity == null) {
      throw new IllegalArgumentException("수량은 필수입니다");
    }
    if (quantity < 1) {
      throw new IllegalArgumentException("수량은 1 이상이어야 합니다");
    }
    if (quantity > 1000) {
      throw new IllegalArgumentException("한 번에 주문할 수 있는 최대 수량은 1000입니다");
    }
  }

  /**
   * 단가 검증
   * - Null 체크
   * - 양수 체크
   */
  private void validateUnitPrice(java.math.BigDecimal unitPrice) {
    if (unitPrice == null) {
      throw new IllegalArgumentException("단가는 필수입니다");
    }
    if (unitPrice.signum() <= 0) {
      throw new IllegalArgumentException("단가는 양수여야 합니다");
    }
  }
}
