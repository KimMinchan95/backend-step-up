# 구매 취소 기능 과제

## 과제 목표

구매 기능을 완성한 코드에 **구매 취소 기능**을 추가한다.

이번 턴은 다음 내용을 직접 연결한다.

- Docker로 실행되는 MySQL의 초기화 SQL 변경
- `ALTER TABLE`을 사용한 테이블 구조 변경
- 구매 상태 변경
- 취소한 수량만큼 상품 재고 복구
- 두 데이터 변경을 하나의 트랜잭션으로 처리
- 동일한 구매를 두 번 취소하지 못하도록 제어

커스텀 예외, `@RestControllerAdvice`, 자동화 테스트 코드는 이번 과제 범위에 포함하지 않는다.

## 시작 조건

이전 구매 기능이 완성되어 있어야 한다.

- `POST /purchases`로 상품을 구매할 수 있다.
- 구매하면 상품 재고가 감소한다.
- `purchases`에 구매내역이 저장된다.
- 저장된 구매내역이 HTTP 응답으로 반환된다.
- 재고 차감과 구매내역 저장은 하나의 트랜잭션으로 처리된다.

## 완성할 동작

```text
구매내역 조회
    ↓
현재 상태 확인
    ↓
구매 상태를 CANCELED로 변경
    ↓
구매 수량만큼 상품 재고 복구
    ↓
변경된 구매내역 조회
    ↓
응답 반환
```

허용하는 상태 변경은 하나뿐이다.

```text
COMPLETED → CANCELED
```

이미 취소된 구매는 다시 취소할 수 없다.

---

## 1. MySQL 초기화 SQL에서 테이블 구조를 변경한다

대상: `docker/mysql/init.sql`

현재 프로젝트는 MySQL 컨테이너가 시작될 때 다음 순서로 실습 데이터를 초기화한다.

```text
기존 테이블 삭제
→ 테이블 생성
→ 초기 데이터 입력
```

`CREATE TABLE purchases` 아래에 `ALTER TABLE` 문을 추가하여 구매 상태와 취소 시각을 저장할 수 있게 한다.

> [!IMPORTANT]
> 작성된 `CREATE TABLE` 문을 수정하기보다 `ALTER TABLE` 문을 사용합니다.<br>
> 이미 생성된 테이블의 구조는 일반적으로 `ALTER TABLE`로 변경합니다.

추가할 컬럼:

| DB 컬럼       | 타입          | 조건                           | 의미           |
| ------------- | ------------- | ------------------------------ | -------------- |
| `status`      | `VARCHAR(20)` | `NOT NULL`, 기본값 `COMPLETED` | 현재 구매 상태 |
| `canceled_at` | `TIMESTAMP`   | null 허용                      | 구매 취소 시각 |

허용할 상태 값은 `COMPLETED`, `CANCELED` 두 개다.

> [!IMPORTANT]
> 요구에 따라 `TIMESTAMP`를 `BIGINT`로 사용할 수도 있습니다.<br>
> 통상 `TIMESTAMP`는 `LocalDateTime`, `BIGINT`는 원시 타입 `long` 또는 참조 타입 `Long`을 사용합니다.<br>
> 시스템 내에서 모든 시간 관리는 통일된 타입을 사용하는 것이 권장됩니다.

`ALTER TABLE`, `ADD COLUMN`, `ADD CONSTRAINT`를 사용하여 직접 DDL을 작성한다.
기존 구매 INSERT SQL에는 `status`를 직접 넣지 않는다. 새 구매는 DB 기본값에 의해 `COMPLETED` 상태로 저장되게 한다.

### 테이블 초기화와 구조 확인

애플리케이션을 완전히 종료한 뒤 다시 실행한다.
현재 프로젝트는 Spring Boot의 Docker Compose Support를 사용하므로, 애플리케이션을 정상 종료하면
Compose 서비스를 멈추고 다음 실행 시 MySQL 컨테이너를 다시 시작한다. 이때 초기화 SQL이 실행된다.

이미 MySQL 컨테이너를 별도로 실행해둬서 테이블 구조가 변경되지 않는 경우에만 직접 재시작한다.

```bash
docker compose restart mysql
```

DB 콘솔에서 변경된 구조를 확인한다.

```sql
DESCRIBE purchases;
SHOW CREATE TABLE purchases;
```

확인할 것:

- [x] MySQL 재시작 과정에서 테이블이 삭제되고 다시 생성된다.
- [x] `status`의 기본값이 `COMPLETED`다.
- [x] `canceled_at`은 null을 허용한다.
- [x] 구매 상태를 제한하는 CHECK 제약조건이 생성됐다.
- [x] 초기화 후 `purchases` 테이블은 비어 있다.

완료 기준: MySQL을 재시작할 때마다 변경된 구조의 빈 구매내역 테이블이 만들어진다.

---

## 2. 구매 모델과 응답에 취소 정보를 추가한다

대상:

- `src/main/resources/application.yaml`
- `purchase/model/Purchase.java`
- `purchase/dto/PurchaseResponse.java`
- `PurchaseMapper.xml`의 구매내역 조회 SQL

추가할 Java 필드:

| Java 필드    | 타입            | DB 컬럼       |
| ------------ | --------------- | ------------- |
| `status`     | `String`        | `status`      |
| `canceledAt` | `LocalDateTime` | `canceled_at` |

이번 과제에서는 상태를 위한 Java enum을 필수로 만들지 않는다.

`application.yaml`에서 MyBatis의 `map-underscore-to-camel-case` 옵션을 직접 찾아 활성화한다.
이 설정을 사용하면 snake_case 형식의 DB 컬럼명이 camelCase 형식의 Java 필드명으로 자동 변환된다.

자동 매핑 예시:

| DB 컬럼        | Java 필드     |
| -------------- | ------------- |
| `product_id`   | `productId`   |
| `unit_price`   | `unitPrice`   |
| `purchased_at` | `purchasedAt` |
| `canceled_at`  | `canceledAt`  |

설정을 활성화한 뒤 기존 구매내역 조회 SQL에 작성했던 `AS` 별칭을 제거한다.
SQL에는 실제 DB 컬럼명을 사용하고, 조회 결과가 Java 필드에 정상적으로 들어오는지 확인한다.

기존 구매 API의 성공 응답에도 상태와 취소 시각이 포함되어야 한다.

```json
{
    "id": 1,
    "productId": 3,
    "quantity": 1,
    "unitPrice": 800,
    "status": "COMPLETED",
    "purchasedAt": "2026-09-17T10:00:00",
    "canceledAt": null
}
```

완료 기준: 새 구매를 만들면 상태는 `COMPLETED`, 취소 시각은 null로 응답된다.

---

## 3. 구매 상태 변경 Mapper를 추가한다

대상:

- `purchase/mapper/PurchaseMapper.java`
- `src/main/resources/mapper/PurchaseMapper.xml`

구매 ID를 받아 아직 완료 상태인 구매만 취소하는 메서드를 만든다.
상태 변경과 취소 시각 기록을 하나의 UPDATE로 처리한다.

Mapper는 변경된 행 수를 반환해야 한다.

- 반환값 `1`: 취소 상태 변경 성공
- 반환값 `0`: 이미 취소됐거나 상태를 변경할 수 없음

단순히 ID만 조건으로 사용하지 않는다. `status = 'COMPLETED'` 조건을 SQL에 포함하여 같은 구매가 두 번 취소되지 않게 한다.

완료 기준: 완료 상태의 구매는 한 번만 취소 상태로 변경되고 취소 시각이 기록된다.

---

## 4. 상품 재고 복구 Mapper를 추가한다

대상:

- `product/mapper/ProductMapper.java`
- `src/main/resources/mapper/ProductMapper.xml`

상품 ID와 복구 수량을 받아 현재 재고에 더하는 메서드를 만든다.

Java에서 기존 재고를 계산하여 새로운 값으로 덮어쓰지 않는다. DB의 현재 재고를 기준으로 수량을 더한다.

> [!IMPORTANT]
> 이번 재고 복구에는 현재 재고를 이용한 추가 판단이나 조정 업무가 없으므로, SQL 안에서 증감 연산을 수행한다.<br>
> 현재 재고를 조회한 뒤 여러 조건을 판단하거나 다른 값을 함께 조정해야 한다면 일반 SELECT로 읽어서 계산하지 않는다. 같은 트랜잭션 안에서 `SELECT ... FOR UPDATE`로 대상 행을 잠근 뒤 Service에서 필요한 업무를 처리하고 UPDATE해야 한다.

Mapper는 변경된 행 수를 반환해야 한다. 상품 한 건이 변경되지 않으면 취소 처리를 계속하지 않는다.

완료 기준: 상품 재고가 구매내역의 수량만큼 정확하게 증가한다.

---

## 5. 구매 취소 Service를 구현한다

대상: `purchase/service/PurchaseService.java`

기존 구매 Service에 구매 ID를 받아 취소하는 public 메서드를 추가한다.

메서드가 처리할 순서:

1. 구매 ID가 null이거나 0 이하이면 `IllegalArgumentException`을 발생시킨다.
2. 구매내역을 조회한다.
3. 구매내역이 없으면 `IllegalArgumentException`을 발생시킨다.
4. 현재 상태가 `COMPLETED`가 아니면 `IllegalStateException`을 발생시킨다.
5. 조건부 UPDATE로 구매 상태를 `CANCELED`로 변경한다.
6. 변경 행 수가 0이면 `IllegalStateException`을 발생시킨다.
7. 구매내역의 상품 ID와 수량으로 상품 재고를 복구한다.
8. 상품 변경 행 수가 1이 아니면 `IllegalStateException`을 발생시킨다.
9. 변경된 구매내역을 다시 조회하여 반환한다.

구매 취소 메서드 전체에 쓰기 가능한 `@Transactional`을 적용한다.

```text
구매 상태 변경 성공
→ 재고 복구 실패
→ 예외 발생
→ 구매 상태 변경도 롤백
```

상태 변경에 성공한 요청만 재고를 복구해야 한다. 그래야 같은 구매에 취소 요청이 반복되어도 재고가 여러 번 증가하지 않는다.

완료 기준: 구매 상태 변경과 상품 재고 복구가 하나의 트랜잭션으로 처리된다.

---

## 6. 구매 취소 API를 추가한다

대상: `purchase/controller/PurchaseController.java`

추가할 API:

```text
POST /purchases/{purchaseId}/cancel
```

요청 본문은 사용하지 않는다. 취소할 구매 ID는 경로에서 받는다.

Controller가 할 일:

1. 경로의 구매 ID를 받는다.
2. 구매 Service의 취소 메서드를 호출한다.
3. 반환받은 구매내역을 `PurchaseResponse`로 변환한다.
4. `ResponseEntity.ok(...)`로 200 응답을 반환한다.

성공 응답 예시:

```json
{
    "id": 1,
    "productId": 3,
    "quantity": 1,
    "unitPrice": 800,
    "status": "CANCELED",
    "purchasedAt": "2026-09-17T10:00:00",
    "canceledAt": "2026-09-17T10:05:00"
}
```

Controller에서 예외를 잡아 정상 응답으로 바꾸지 않는다. 이번 과제에서는 Spring Boot의 기본 오류 응답을 사용한다.

완료 기준: 구매 취소 요청으로 상태가 변경되고 복구된 결과가 응답된다.

---

## 7. Postman과 DB에서 동작을 확인한다

### 정상 취소

1. MySQL을 재시작하여 초기화한다.
2. 재고가 1개인 생수를 1개 구매한다.
3. 상품 조회 API로 생수 재고가 0인지 확인한다.
4. 생성된 구매 ID로 취소 API를 호출한다.
5. 응답 상태가 `CANCELED`이고 취소 시각이 있는지 확인한다.
6. 상품 조회 API 또는 DB에서 생수 재고가 다시 1인지 확인한다.

### 중복 취소

1. 같은 구매 ID로 취소 API를 다시 호출한다.
2. 요청이 실패하는지 확인한다.
3. 상품 재고가 1에서 더 증가하지 않았는지 확인한다.
4. 구매내역의 취소 시각이 다시 변경되지 않았는지 확인한다.

### 존재하지 않는 구매 취소

1. 존재하지 않는 구매 ID로 취소 API를 호출한다.
2. 요청이 실패하는지 확인한다.
3. 어떤 상품의 재고도 바뀌지 않았는지 확인한다.

### 트랜잭션 롤백 확인

로컬 실습 중에만 재고 복구 후 `IllegalStateException`을 강제로 발생시킨다.

```text
구매 상태를 CANCELED로 변경
→ 상품 재고 복구
→ 강제 예외 발생
```

새로운 DB 조회로 다음 결과를 확인한다.

- [ ] 구매 상태가 `COMPLETED`로 돌아왔다.
- [ ] `canceled_at`이 null로 돌아왔다.
- [ ] 상품 재고도 취소 요청 전 수량으로 돌아왔다.

확인이 끝나면 강제 예외 코드를 제거한다.

---

## 완료 기준

- [ ] Docker MySQL 초기화 SQL에 `ALTER TABLE`을 추가했다.
- [ ] MySQL 재시작 후 변경된 테이블 구조를 확인했다.
- [ ] 새 구매가 `COMPLETED` 상태로 저장된다.
- [ ] 구매 취소 API가 구매 상태를 `CANCELED`로 변경한다.
- [ ] 취소 시각이 DB에서 생성된다.
- [ ] 구매 수량만큼 상품 재고가 복구된다.
- [ ] 같은 구매를 다시 취소해도 재고가 추가로 증가하지 않는다.
- [ ] 존재하지 않는 구매를 취소하면 데이터가 변경되지 않는다.
- [ ] 취소 처리 중 예외가 발생하면 상태 변경과 재고 복구가 모두 롤백된다.

## 과제 완료 후 설명할 수 있어야 하는 것

1. 구매내역을 DELETE하지 않고 상태를 변경한 이유는 무엇인가?
2. `status`에 DB 기본값을 둔 이유는 무엇인가?
3. 상태 변경 UPDATE에 `status = 'COMPLETED'` 조건을 넣은 이유는 무엇인가?
4. 재고를 Java에서 계산하지 않고 SQL에서 더한 이유는 무엇인가?
5. 구매 상태 변경과 재고 복구를 하나의 트랜잭션으로 묶어야 하는 이유는 무엇인가?
6. 런타임 예외가 Service 밖으로 전달될 때 데이터가 어떻게 처리되는가?
