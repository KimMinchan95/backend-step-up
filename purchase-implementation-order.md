# 구매 기능 구현 순서

상품 조회 코드를 활용해서 구매 기능 하나를 완성한다.
구매내역 저장부터 확인하고, 재고 차감을 붙인 다음 Service에서 하나의 트랜잭션으로 묶는다.
Controller를 연결한 뒤 Postman 또는 curl로 요청하고, DB 콘솔에서 결과를 직접 확인한다.
이 가이드에서는 자동화 테스트 코드를 작성하지 않는다.
예외는 Java 기본 예외를 사용하고, 커스텀 예외와 공통 에러 핸들러는 이번 실습에서 다루지 않는다.

## 현재 준비된 것

- [x] `products`: 상품 4개와 샘플 재고.
- [x] `purchases`: 빈 구매내역 테이블.
- [x] `Product`, `ProductMapper`, `ProductMapper.xml`.
- [x] `ProductService.getProducts()`: 상품 목록 조회.
- [x] `ProductService.getProduct(productId)`: 상품 단건 조회.
- [x] `ProductController`: `GET /products`, `GET /products/{productId}`.
- [x] `ProductResponse`: 상품 ID, 이름, 가격, 재고를 담는 조회 응답.

상품 조회는 기존 코드를 사용한다. 이 문서에서 DAO 역할은 MyBatis Mapper 인터페이스와 XML이 맡는다.
앱 실행 후 Postman에서 `GET http://localhost:8080/products`로 상품 목록을,
`GET http://localhost:8080/products/1`로 콜라를 조회할 수 있다. 기본 서버 포트 기준이다.
구매 전후의 재고도 이 조회 API로 확인할 수 있다.

## 패키지 구성

기능별 패키지 안에서 역할별 하위 패키지를 나눈다. 상품 코드는 아래 구조로 준비되어 있고,
구매 코드는 같은 규칙으로 실습하면서 만든다.

```text
src/main/java/dev/training/back/
├── product/
│   ├── model/Product.java
│   ├── mapper/ProductMapper.java
│   ├── service/ProductService.java
│   ├── controller/ProductController.java
│   └── dto/ProductResponse.java
└── purchase/                         ← 아래 단계에서 구현
    ├── model/Purchase.java
    ├── mapper/PurchaseMapper.java
    ├── service/PurchaseService.java
    ├── controller/PurchaseController.java
    └── dto/
        ├── PurchaseRequest.java
        └── PurchaseResponse.java
```

`model`은 DB에서 읽고 저장할 데이터를, `dto`는 HTTP 요청·응답 데이터를 담는다.
Mapper XML은 `src/main/resources/mapper/`에 둔다. Java 패키지가 바뀌면 XML의
namespace와 모델을 가리키는 타입 경로도 함께 맞춘다.

## 완성할 구매 동작

입력은 **상품 ID와 구매 수량**이다. 가격은 요청에서 받지 않고 DB에서 조회한 값을 사용한다.

```text
상품 조회 → 수량·재고 확인 → 재고 차감 → 구매내역 저장 → 저장된 구매내역 조회 → 응답 반환
```

정상 구매에서는 재고 차감과 구매내역 저장이 함께 반영된다.
구매내역 저장에 실패하면 앞서 차감한 재고도 원래대로 돌아와야 한다.
성공 응답에는 저장된 구매내역의 구매 ID, 상품 ID, 수량, 개당 가격, 구매 시각을 담는다.

## 1. 구매내역 객체를 만든다

대상: `src/main/java/dev/training/back/purchase/model/Purchase.java`

- [x] `id`, `productId`, `quantity`, `unitPrice`, `purchasedAt` 필드를 만든다.
- [x] DB의 `BIGINT`는 `Long`, `INT`는 `Integer`, 구매 시각은 `LocalDateTime`으로 잡는다.
- [x] `unitPrice`는 구매 당시 개당 가격이다.
- [x] 구매 ID와 구매 시각은 DB에서 생성하게 한다.

완료 기준: 어떤 상품을 몇 개, 얼마에 구매했는지 담을 수 있다.

## 2. 구매내역 저장·조회 Mapper를 만든다

대상:

- `src/main/java/dev/training/back/purchase/mapper/PurchaseMapper.java`
- `src/main/resources/mapper/PurchaseMapper.xml`

- [x] Mapper 인터페이스에 `@Mapper`를 붙이고 `int insert(Purchase purchase)`를 선언한다.
- [x] XML의 namespace를 `dev.training.back.purchase.mapper.PurchaseMapper`로 맞춘다.
- [x] `purchases`에 `product_id`, `quantity`, `unit_price`를 INSERT한다.
- [x] 값을 바인딩할 때 Java 필드명인 `productId`, `quantity`, `unitPrice`를 사용한다.
- [x] `useGeneratedKeys`와 `keyProperty="id"`로 생성된 구매 ID를 객체에 받는다.
- [x] `Purchase findById(@Param("id") Long id)`를 선언하고 구매 ID로 한 건을 조회하는 SELECT를 작성한다.
- [x] SELECT의 `resultType`을 `dev.training.back.purchase.model.Purchase`로 지정한다.
- [x] SELECT에서 `product_id AS productId`, `unit_price AS unitPrice`, `purchased_at AS purchasedAt`처럼 Java 필드명에 맞춰 매핑한다.
- [x] DB 콘솔에서 INSERT SQL에 기존 상품 ID, 수량, 가격을 넣어 한 건을 저장해본다.
- [x] 생성된 구매 ID로 SELECT해서 저장된 값과 DB에서 생성한 구매 시각을 확인한다.

완료 기준: DB 콘솔에서 구매내역 1건을 저장하고, 생성된 구매 ID로 구매 시각까지 조회할 수 있다.
이 단계에서는 SQL의 동작을 확인한다. Mapper의 파라미터 바인딩, Java 객체에 구매 ID가
채워지는 동작, 조회 결과 매핑은 Controller 연결 후 실제 구매 요청으로 확인한다.

## 3. 상품 재고 차감 Mapper를 추가한다

대상:

- `src/main/java/dev/training/back/product/mapper/ProductMapper.java`
- `src/main/resources/mapper/ProductMapper.xml`

- [x] 상품 ID와 구매 수량을 받아 재고를 차감하는 메서드를 추가한다.
- [x] 여러 파라미터에 `@Param`을 지정하고 XML의 이름과 맞춘다.
- [x] 재고가 구매 수량 이상일 때만 차감하는 UPDATE를 작성한다.
- [x] 조회한 재고를 Java에서 계산해 덮어쓰지 않고, SQL에서 현재 재고를 차감한다.
- [x] 반환값으로 변경 행 수를 받는다. 1이면 성공, 0이면 상품 없음 또는 재고 부족이다.
- [x] DB 콘솔에서 충분한 재고, 재고와 같은 수량, 부족한 재고로 UPDATE를 직접 실행한다.
- [x] 실행 전후의 재고와 변경 행 수를 확인한다.

완료 기준: 성공하면 요청 수량만큼 줄고, 재고가 부족하면 변경되지 않는다.
구매 수량이 양수인지 확인하는 책임은 다음 단계의 Service에 둔다.

## 4. 구매 Service를 만들어 연결한다

대상: `src/main/java/dev/training/back/purchase/service/PurchaseService.java`

- [x] `@Service`를 붙이고 생성자로 `ProductService`, `ProductMapper`, `PurchaseMapper`를 주입한다.
- [x] 상품 ID와 수량을 받아 `Purchase`를 반환하는 public 구매 메서드를 만든다.
- [x] 구매 메서드에 쓰기 가능한 `@Transactional`을 붙인다.
- [x] 상품 ID가 null 또는 0 이하이거나 수량이 null 또는 0 이하이면 `IllegalArgumentException`을 발생시킨다.
- [x] 기존 `ProductService.getProduct(productId)`로 상품을 조회한다. 상품이 없으면 기존의 `IllegalArgumentException`을 그대로 전달한다.
- [x] 재고 차감 Mapper를 호출한다. 변경 행 수가 0이면 `IllegalStateException`을 발생시킨다.
- [x] 조회한 상품 가격과 구매 수량으로 `Purchase`를 만든다.
- [x] 구매내역을 저장하고 객체에 채워진 구매 ID로 `PurchaseMapper.findById()`를 호출한다.
- [x] DB에서 조회한 `Purchase`를 반환한다. 저장 직후 조회 결과가 없으면 `IllegalStateException`을 발생시킨다.
- [x] DB 저장 예외는 별도 예외로 감싸지 않고 그대로 전달한다. 실패를 잡아서 정상 반환하지 않는다.

완료 기준: 구매 메서드의 상품 조회, 재고 차감, 구매내역 저장·조회가 연결되고 빌드가 된다.
실제 구매 성공과 롤백은 다음 단계에서 Controller를 연결한 뒤 확인한다.

`useGeneratedKeys`로 받는 값은 구매 ID다. DB가 생성한 구매 시각까지 응답하려면
저장된 행을 다시 조회한다. 구매내역 저장·조회는 같은 구매 Service에서 처리한다.

기존 `ProductService`는 `@Transactional(readOnly = true)`가 붙은 조회 서비스다.
구매 메서드는 별도의 `PurchaseService`에 두고, 재고 차감과 구매내역 저장이 같은 트랜잭션을
사용하도록 한다. 호출 경로는 외부 호출자 → Spring이 관리하는 `PurchaseService` → 각 Mapper다.

`IllegalArgumentException`, `IllegalStateException`은 런타임 예외다.
이런 예외가 구매 메서드 밖으로 전달되면 기본 `@Transactional` 설정에서 롤백된다.
이번 실습에서는 예외별 응답 형식보다 **실패 시 함께 변경한 데이터가 취소되는지**에 집중한다.

## 5. 구매 Controller를 붙인다

대상:

- `src/main/java/dev/training/back/purchase/controller/PurchaseController.java`
- `src/main/java/dev/training/back/purchase/dto/PurchaseRequest.java`
- `src/main/java/dev/training/back/purchase/dto/PurchaseResponse.java`

- [ ] `PurchaseRequest`에 상품 ID와 수량을 받는다.
- [ ] 필수값과 양수 검증을 붙이고 Controller에서 `@Valid`로 요청을 검증한다.
- [ ] `POST /purchases`에서 요청을 받아 `PurchaseService`에 전달한다.
- [ ] Service가 반환한 `Purchase`를 `PurchaseResponse`로 변환한다.
- [ ] `PurchaseResponse`에 `id`, `productId`, `quantity`, `unitPrice`, `purchasedAt`을 담아 201 응답을 반환한다.
- [ ] Controller는 요청·응답을 담당하고, 재고 변경과 구매내역 저장은 Service에 맡긴다.
- [ ] Service에서 전달된 예외는 Controller에서 잡지 않고 Spring Boot의 기본 오류 응답을 사용한다.

완료 기준: HTTP 요청이 구매 Service까지 전달되고, 저장된 구매내역이 담긴 응답을 받을 수 있다.

커스텀 예외, `@RestControllerAdvice`, 별도의 오류 응답 DTO는 만들지 않는다.
`IllegalArgumentException`을 던진다고 자동으로 400 응답이 되는 것은 아니다.
별도 매핑이 없는 Service의 기본 예외는 500 응답으로 확인하고, `@Valid` 요청 검증 실패는
기본 400 응답으로 확인한다. 상품 없음·재고 부족에 각각 상태 코드를 지정하는 작업은 이번 범위에서 제외한다.

## 6. 직접 구매를 요청하고 DB에서 성공·롤백을 확인한다

사용 도구: Postman 또는 curl, IDE의 DB 콘솔.

### 실습 데이터를 초기 상태로 되돌린다

앞 단계에서 SQL을 직접 실행하면서 바뀐 데이터를 초기화한다.

```bash
docker compose restart mysql
docker compose up -d --wait
```

이 프로젝트는 MySQL 시작 시 `products`, `purchases`를 다시 만든다.
상품이 샘플 재고로 돌아가고 구매내역은 비워진다.

### 정상 구매를 확인한다

- [ ] 콜라의 재고가 10개이고 구매내역이 0건인지 확인한다.
- [ ] `POST /purchases`에 다음 요청을 보낸다.

```json
{ "productId": 1, "quantity": 2 }
```

- [ ] 201 성공 응답에 구매 ID, 상품 ID 1, 수량 2, 개당 가격 1500원, 구매 시각이 있는지 확인한다.
- [ ] DB에서 콜라 재고가 8개인지 확인한다.
- [ ] 구매내역이 1건이고, 상품 ID는 1, 수량은 2, 개당 가격은 1500원인지 확인한다.
- [ ] 응답의 구매 ID로 DB를 조회해서 응답 값이 저장된 구매내역과 일치하는지 확인한다.

### 구매내역 저장 실패와 롤백을 확인한다

- [ ] MySQL을 다시 초기화해서 콜라 재고 10개, 구매내역 0건으로 시작한다.
- [ ] 로컬 실습 중에만, 재고 차감 후 구매내역 객체에 넣는 수량을 잠시 0으로 바꾼다.
- [ ] 앱에 변경 코드를 반영하고, 위와 같은 정상 구매 수량 2의 HTTP 요청을 보낸다.
- [ ] 재고 차감 뒤 구매내역 INSERT가 `CHECK (quantity > 0)` 제약조건으로 실패하는지 확인한다.
- [ ] 저장 예외를 잡아서 성공으로 처리하지 않고, 구매 Service 밖으로 전달한다.
- [ ] 구매 요청에 기본 500 오류 응답이 오는지 확인한다. 구체적인 실패 원인은 서버 로그에서 확인한다.
- [ ] DB 콘솔에서 새로 SELECT해서 콜라 재고가 10개이고 구매내역이 0건인지 확인한다.
- [ ] 확인이 끝나면 구매내역에 정상 수량을 넣도록 코드를 원래대로 돌린다.

요청 자체의 수량을 0으로 보내면 입력 검증에서 먼저 막히므로, 재고 차감 이후의 롤백을
확인할 수 없다. **재고 차감에는 정상 수량을 쓰고 구매내역 저장만 실패**시킨다.

완료 기준: 정상 구매는 재고와 구매내역이 함께 반영되고, 저장 실패는 재고 차감까지 취소된다.
롤백은 HTTP 요청으로 실행된 **구매 Service의 트랜잭션**이 담당한다.

## 7. 기본 구매 완성 후 동시 구매를 수동으로 살펴본다 — 추가 실습

- [ ] 데이터를 초기화하고 재고 1개인 생수를 대상으로 한다.
- [ ] 두 개의 클라이언트에서 각각 수량 1의 구매 요청을 최대한 동시에 보낸다.
- [ ] 요청마다 별도의 트랜잭션과 DB 연결을 사용한다.
- [ ] 한 요청만 성공하고 다른 요청은 재고 부족으로 실패하는지 확인한다.
- [ ] DB 콘솔에서 최종 재고 0개, 성공한 구매내역 1건인지 확인한다.

수동 요청은 실제 실행 시점이 겹쳤다고 보장할 수 없다. 이 단계는 동작을 관찰하는
추가 실습이며, 한 번의 결과만으로 동시성 검증이 끝났다고 판단하지 않는다.

3단계의 조건부 UPDATE가 같은 상품의 재고 변경을 어떻게 처리하는지 확인한다.
이 UPDATE도 InnoDB의 행 잠금을 사용한다. 조회 시 잠그는 `SELECT ... FOR UPDATE`나
낙관적 잠금과의 비교는 기본 실습을 마친 뒤 확장한다.

## 진행 순서 체크

- [ ] 구매내역 객체
- [ ] 구매내역 저장·조회 Mapper + DB 콘솔에서 SQL 확인
- [ ] 상품 재고 차감 Mapper + DB 콘솔에서 SQL 확인
- [ ] 구매 Service 연결 + 트랜잭션
- [ ] 구매 Controller + 요청 검증·구매내역 응답 + 기본 오류 응답 사용
- [ ] HTTP 요청과 DB 조회로 정상 구매·저장 실패·롤백 확인
- [ ] 동시 구매 수동 관찰 — 추가 실습
