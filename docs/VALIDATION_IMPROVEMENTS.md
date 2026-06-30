# OrderService 입력값 검증 개선

## 개요
주문 생성 시 입력값 검증을 강화하여 잘못된 데이터로 인한 문제를 사전에 방지합니다.

## 변경 사항

### 1. CreateOrderValidator 추가
- 새로운 Validator 클래스 추가
- 비즈니스 로직 검증 규칙 정의
- 명확한 에러 메시지 제공

#### 검증 규칙
| 필드 | 검증 규칙 | 에러 메시지 |
|------|----------|-------------|
| userId | Null 체크, 양수 필수 | "사용자 ID는 양수여야 합니다" |
| productId | Null 체크, 양수 필수 | "상품 ID는 양수여야 합니다" |
| quantity | Null 체크, 1-1000 범위 | "한 번에 주문할 수 있는 최대 수량은 1000입니다" |
| unitPrice | Null 체크, 양수 필수 | "단가는 양수여야 합니다" |

### 2. OrderService 개선
- `CreateOrderValidator` 의존성 추가
- `createOrder()` 메서드에서 검증 호출
- 로깅 강화 (DEBUG/INFO 레벨)
- Kafka 이벤트 발행 실패 처리 추가

### 3. 검증 계층 구조

```
요청 수신
  ↓
[1] 기본 검증 (@Valid, @NotNull, @Min)
  ↓
[2] 비즈니스 검증 (CreateOrderValidator)
  ↓
[3] 서비스 로직
```

## 기술 스택
- Jakarta Validation API (@NotNull, @Min)
- Spring Component (@Component)
- Lombok (@Slf4j, @RequiredArgsConstructor)

## 테스트 시나리오

### 성공 케이스
```bash
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 100,
    "quantity": 5,
    "unitPrice": 10000
  }'
```

### 실패 케이스

#### 1. 수량이 1000 초과
```bash
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 100,
    "quantity": 1001,
    "unitPrice": 10000
  }'
# 응답: "한 번에 주문할 수 있는 최대 수량은 1000입니다"
```

#### 2. userId가 음수
```bash
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": -1,
    "productId": 100,
    "quantity": 5,
    "unitPrice": 10000
  }'
# 응답: "사용자 ID는 양수여야 합니다"
```

## 관련 이슈
- GitHub Issue #1: OrderService 요청 입력값 검증 누락

## 향후 개선 사항

1. **사용자/상품 존재 여부 확인**
   - UserService/ProductService 호출
   - 존재하지 않는 리소스 주문 방지

2. **재고 확인**
   - 주문 전 재고 확인
   - 재고 부족 시 사전 실패

3. **Dead Letter Queue (DLQ)**
   - Kafka 이벤트 발행 실패 처리
   - Issue #3 참고
