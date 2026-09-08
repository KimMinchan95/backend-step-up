# Spring Backend 교육 커리큘럼

> 대상: 프론트엔드 개발자  
> 목표: Spring 자체의 핵심 개념을 익히면서, 실제 레거시 Spring + MyBatis 프로젝트도 읽을 수 있는 수준까지 연결한다.  
> 방향: 레거시 환경을 그대로 재현하지 않고, 현대적인 Spring Boot 환경에서 핵심 구조를 먼저 익힌다.

---

## 0. 기준 스택

- Java 21
- Spring Boot 4.1.1
- Maven
- Lombok
- Spring Web MVC
- Validation
- MyBatis
- Mapper XML
- MySQL
- Docker Compose
- JUnit / Spring Boot Test
- Cursor

### 프로젝트 구조

```text
Controller
    ↓
Service
    ↓
Mapper Interface
    ↓
Mapper XML
    ↓
MySQL
```

핵심 목표는 각 계층이 왜 존재하고, 요청 하나가 어느 경로를 따라 DB까지 갔다가 응답으로 돌아오는지 이해하는 것.

---

# 1. 개발환경 세팅

## 1-1. JDK 설치

Java 개발에는 JDK가 필요하다는 것만 먼저 이해한다.

프론트 기준으로 비유하면:

```text
Node.js 설치
    ↓
node / npm 사용 가능

JDK 설치
    ↓
java / javac 사용 가능
```

설치 후 확인:

```bash
java -version
javac -version
```

둘 다 Java 21이 잡히는지 확인한다.

### 포인트

- JRE와 JDK 차이는 깊게 들어가지 않는다.
- Spring Boot 개발에는 JDK가 필요하다는 것만 우선 이해한다.
- `JAVA_HOME`은 문제가 생겼을 때 설명한다.

- JVM -> 실제로 앱 띄움
- JRE -> JVM 포함 앱 실행 세트 (최소 JRE까지 있어야 앱 실행 가능)
- JDK -> JRE 포함 컴파일러 + 개발 도구까지 묶어놓은 풀세트 (개발 하려면 JDK가 필수)

---

## 1-2. Cursor Java 환경

### 필수 Extension

```text
Extension Pack for Java
ID: vscjava.vscode-java-pack

Spring Boot Extension Pack
ID: vmware.vscode-boot-dev-pack
```

페어 프로그래밍을 할 경우:

```text
CodeTogether
```

### JDK 확인

Command Palette:

```text
Ctrl + Shift + P
→ Java: Configure Java Runtime
```

### Maven 프로젝트 재로딩

```text
Ctrl + Shift + P
→ Java: Reload Projects
```

Java Language Server 상태가 꼬였을 경우:

```text
Ctrl + Shift + P
→ Java: Clean the Java language server workspace
```

---

# 2. Spring Boot 프로젝트 생성

`start.spring.io`에서 Maven 프로젝트를 생성한다.

## 기본 설정

```text
Project: Maven
Language: Java
Spring Boot: 4.1.1
Java: 21
Packaging: Jar
```

## Dependencies

```text
Spring Web
Validation
Lombok
MySQL Driver
Docker Compose Support
```

### MyBatis

Spring Boot 4.1.1에서는 Initializr UI에서 MyBatis 선택이 제한될 수 있으므로 직접 추가한다. (pom.xml)

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>4.1.0</version>
</dependency>
```

---

# 3. 프로젝트를 일단 실행해보기

가장 먼저 아무 기능도 만들지 않고 애플리케이션부터 실행한다.

```java
@SpringBootApplication
public class StudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyApplication.class, args);
    }
}
```

### 여기서 설명할 것

- Java 프로그램의 시작점은 `main`
- `SpringApplication.run()`이 Spring을 기동
- Embedded Tomcat
- 서버가 뜬다는 것과 Spring Context가 만들어진다는 것
- Boot가 많은 설정을 자동으로 해주고 있다는 것

아직 내부 동작을 전부 파지는 않는다.

---

# 4. 가장 단순한 API

```java
@RestController
@RequestMapping("/hello")
public class HelloController {

    @GetMapping
    public String hello() {
        return "hello";
    }
}
```

브라우저/Postman/curl에서 호출한다.

```bash
curl http://localhost:8080/hello
```

## 여기서 설명할 것

```text
HTTP Request
    ↓
Tomcat
    ↓
DispatcherServlet
    ↓
Controller
    ↓
Response
```

### 프론트와 연결

```text
fetch / axios
      ↓
HTTP
      ↓
Spring Controller
      ↓
JSON
      ↓
Frontend
```

프론트 개발자가 이미 알고 있는 HTTP 요청의 반대편을 보여주는 데 집중한다.

---

# 5. Controller / Service 분리

처음부터 계층을 과하게 만들지 않는다.

Controller 하나를 만든 뒤 Service를 분리하면서 이유를 설명한다.

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
}
```

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
}
```

## 여기서 설명할 것

- Controller는 HTTP를 담당
- Service는 업무 로직을 담당
- DB 접근을 Controller에서 바로 하지 않는 이유
- 객체가 직접 다른 객체를 생성하지 않는 이유

---

# 6. IoC / DI / Bean

Spring에서 가장 중요하게 잡을 개념.

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
}
```

### 설명 흐름

일반 Java:

```java
UserMapper mapper = new UserMapper();
```

Spring:

```text
Spring Container
    ↓
Bean 생성
    ↓
필요한 객체를 찾아서 주입
```

## 반드시 이해할 것

- Bean
- IoC Container
- Dependency Injection
- Component Scan
- Constructor Injection
- `@Component`
- `@Service`
- `@Repository`
- `@Controller`
- `@RestController`

### Lombok

`@RequiredArgsConstructor`가 생성자 자체를 없애는 마법이 아니라 생성자 코드를 생성해준다는 걸 설명한다.

자주 사용할 것:

```text
@RequiredArgsConstructor
@Slf4j
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
```

### 기본 원칙

Lombok은 보일러플레이트를 줄인다.

클래스의 의미나 설계를 Lombok에게 전부 맡기지는 않는다.

특히 `@Data`는 이유 없이 기본값처럼 사용하지 않는다.

---

# 7. Docker MySQL 연결

Docker 자체는 깊게 설명하지 않는다.

목표:

```text
Spring Application
       ↓
localhost:3306
       ↓
Docker MySQL
```

Docker Compose Support로 제공되는 `compose.yaml`을 사용한다.

확인할 것:

```bash
docker compose ps
```

필요하면:

```bash
docker compose up -d
docker compose down
```

## 설명 범위

- Docker가 MySQL 실행 환경을 제공
- Spring 애플리케이션은 DB에 접속
- Docker와 Spring은 별개의 프로세스
- Container 내부 DB라고 SQL 사용법이 달라지는 것은 아님

여기 이상으로 Docker 내부 구조는 이번 교육에서 다루지 않는다.

---

# 8. MyBatis 기본

## Mapper Interface

```java
@Mapper
public interface UserMapper {

    User findById(Long id);
}
```

## Mapper XML

```xml
<mapper namespace="com.example.study.user.UserMapper">

    <select id="findById"
            parameterType="long"
            resultType="com.example.study.user.User">
        SELECT
            user_id,
            user_name
        FROM users
        WHERE user_id = #{id}
    </select>

</mapper>
```

## application.yml

```yaml
mybatis:
  mapper-locations: classpath:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

## 여기서 이해할 것

```text
UserMapper.findById()
        ↓
Mapper XML의 findById
        ↓
SQL 실행
        ↓
ResultSet
        ↓
Java Object
```

MyBatis가 SQL을 대신 만들어주는 ORM이라고 이해시키지 않는다.

핵심은:

> SQL은 개발자가 작성하고, MyBatis는 Java 객체와 SQL 실행 사이의 연결을 담당한다.

---

# 9. CRUD 한 바퀴

`users` 도메인 하나로 끝까지 구현한다.

## API

```text
POST   /users
GET    /users/{id}
GET    /users
PUT    /users/{id}
DELETE /users/{id}
```

## 구현 순서

```text
Request DTO
    ↓
Controller
    ↓
Service
    ↓
Mapper
    ↓
Mapper XML
    ↓
MySQL
```

그리고 반대로:

```text
MySQL
    ↓
Mapper
    ↓
Service
    ↓
Response DTO
    ↓
JSON
```

여기서 처음으로 전체 요청 흐름을 완성한다.

---

# 10. DTO / Request / Response

DB 객체와 HTTP 객체를 무조건 하나로 쓰지 않는 이유를 설명한다.

예:

```java
@Getter
@Setter
@NoArgsConstructor
public class UserCreateRequest {

    private String name;
    private Integer age;
}
```

```java
@Getter
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private Integer age;
}
```

## 포인트

```text
Request DTO
→ 외부에서 서버로 들어오는 데이터

Response DTO
→ 서버에서 외부로 내보내는 데이터
```

DTO를 만드는 행위 자체보다 경계를 분리하는 이유를 이해시키는 게 목표.

---

# 11. Validation

```java
@Getter
@Setter
public class UserCreateRequest {

    @NotBlank
    private String name;

    @Min(1)
    private Integer age;
}
```

```java
@PostMapping
public UserResponse create(
        @Valid @RequestBody UserCreateRequest request
) {
    return userService.create(request);
}
```

## 설명할 것

- Frontend validation과 Backend validation은 목적이 다름
- 클라이언트가 보내는 값은 신뢰하면 안 됨
- HTTP 요청 데이터 검증
- `@Valid`
- Bean Validation

---

# 12. 예외 처리

처음에는 Service에서 예외를 직접 던져본다.

```java
throw new IllegalArgumentException("User not found");
```

그 다음 전역 처리로 이동한다.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

## 설명할 것

```text
Exception
    ↓
Controller Advice
    ↓
HTTP Status
    ↓
Error Response
```

프론트에서 받는 HTTP Error Response가 백엔드에서 어떻게 생성되는지 연결한다.

---

# 13. Transaction

MyBatis를 사용하는 가장 중요한 이유 중 하나.

예:

```java
@Transactional
public void transfer(...) {
    accountMapper.withdraw(...);
    accountMapper.deposit(...);
}
```

일부러 두 번째 SQL을 실패시킨다.

```text
UPDATE A 성공
UPDATE B 실패
      ↓
Rollback
      ↓
A도 원상복구
```

## 반드시 설명할 것

- Transaction
- Commit
- Rollback
- ACID는 개념 수준
- `@Transactional`
- RuntimeException rollback
- 여러 Mapper 호출이 하나의 Transaction으로 묶이는 과정

이후 필요하면:

- Propagation
- Isolation
- `REQUIRED`
- `REQUIRES_NEW`
- `READ_COMMITTED`

까지 확장한다.

---

# 14. MyBatis XML 실전 기능

CRUD가 익숙해진 뒤 추가한다.

```text
resultMap
if
choose / when
foreach
sql / include
```

## 연습

### 조건 검색

```xml
<if test="name != null">
    AND user_name = #{name}
</if>
```

### IN

```xml
<foreach collection="ids"
         item="id"
         open="("
         separator=","
         close=")">
    #{id}
</foreach>
```

여기서 XML 동적 쿼리가 왜 필요한지 실제 요구사항으로 보여준다.

---

# 15. Logging

Lombok:

```java
@Slf4j
@Service
public class UserService {
}
```

사용:

```java
log.info("Create user. name={}", request.getName());
```

## 설명할 것

- `System.out.println()` 대신 Logger를 쓰는 이유
- 로그 레벨
  - TRACE
  - DEBUG
  - INFO
  - WARN
  - ERROR
- 문자열 연결 대신 `{}` placeholder 사용

로그 시스템 자체의 심화 설정은 나중에 한다.

---

# 16. Test

처음부터 모든 테스트 방법을 가르치지 않는다.

순서:

### Service Unit Test

```text
Service
+
Mock Mapper
```

### Controller Test

HTTP 요청/응답 검증.

### Integration Test

필요한 경우 전체 Spring Context + DB까지 연결.

## 핵심

테스트 문법보다:

> 무엇을 어디까지 검증하려는 테스트인가?

를 먼저 잡는다.

---

# 17. 설정과 Profile

`application.yml`

```yaml
spring:
  application:
    name: study
```

필요한 시점에:

```text
application.yml
application-local.yml
application-dev.yml
application-prod.yml
```

설명한다.

## 이해할 것

- 코드와 환경설정 분리
- 환경마다 달라지는 값
- DB URL
- 계정
- 외부 API 주소
- 로그 레벨

환경변수와 Secret 관리 심화는 필요할 때 추가한다.

---

# 18. 최종 요청 흐름

교육 마지막에는 코드를 보지 않고 이 흐름을 설명할 수 있어야 한다.

```text
Frontend
   │
   │ HTTP / JSON
   ▼
Controller
   │
   │ Request DTO
   ▼
Service
   │
   │ Business Logic
   ▼
Mapper Interface
   │
   ▼
Mapper XML
   │
   │ SQL
   ▼
MySQL
```

응답:

```text
MySQL
   ↓
Mapper
   ↓
Service
   ↓
Response DTO
   ↓
Controller
   ↓
JSON
   ↓
Frontend
```

---

# 19. Spring Framework 4 레거시 연결

현대적인 구조를 이해한 뒤 실제 레거시를 보여준다.

목표는 레거시 기술을 새로 외우게 하는 게 아니라:

> Spring Boot가 자동으로 해주던 역할이 옛날 프로젝트에서는 어디에 명시되어 있는가?

를 찾게 하는 것.

비교:

```text
Spring Boot

@SpringBootApplication
Auto Configuration
Embedded Tomcat
application.yml
Bean 기반 Configuration
```

```text
Legacy Spring

web.xml
DispatcherServlet
ContextLoaderListener
applicationContext.xml
servlet-context.xml
DataSource Bean
SqlSessionFactoryBean
MapperScannerConfigurer
TransactionManager
외부 Tomcat
```

## 핵심 메시지

```text
Spring의 핵심 원리는 동일하다.

Boot:
설정을 많이 자동화한다.

Legacy:
그 설정이 프로젝트 밖으로 드러나 있다.
```

---

# 20. 초반에 굳이 안 가르칠 것

처음부터 아래까지 들어가면 Spring보다 주변 기술 학습량이 더 커진다.

```text
JPA / Hibernate
Spring Security
OAuth
Kafka
Redis
Kubernetes
Docker 내부 구조
Spring Cloud
WebFlux
AOP 직접 구현
Custom Annotation
Custom Starter
Native Image
```

필요해지는 시점에 추가한다.

---

# 실습 프로젝트 추천

## 간단한 주문 시스템

도메인:

```text
User
Product
Order
OrderItem
```

가능한 실습:

```text
회원 생성
상품 조회
주문 생성
주문 조회
재고 차감
주문 취소
```

이 하나로:

```text
REST
DTO
Validation
MyBatis
JOIN
동적 Query
Transaction
Exception Handling
Logging
Test
```

까지 대부분 연결할 수 있다.

특히 주문 생성:

```text
Order INSERT
OrderItem INSERT
Stock UPDATE
```

를 하나의 Transaction으로 묶으면 `@Transactional` 실습하기 좋다.

---

# 진행 체크리스트

- [ ] JDK 21 설치
- [ ] Cursor Java 환경 구성
- [ ] Spring Boot 프로젝트 생성
- [ ] 애플리케이션 실행
- [ ] Hello API
- [ ] Controller / Service 분리
- [ ] Bean / IoC / DI
- [ ] Lombok
- [ ] Docker MySQL
- [ ] MyBatis Mapper
- [ ] Mapper XML
- [ ] CRUD
- [ ] DTO
- [ ] Validation
- [ ] Exception Handling
- [ ] Transaction
- [ ] MyBatis 동적 Query
- [ ] Logging
- [ ] Test
- [ ] Profile / 설정
- [ ] 전체 요청 흐름 복습
- [ ] Spring 4 Legacy 비교

---

# 교육 방향 요약

```text
1. 일단 서버를 띄운다.
2. HTTP 요청 하나를 받는다.
3. Controller와 Service를 나눈다.
4. Spring이 객체를 왜 대신 관리하는지 이해한다.
5. DB를 붙인다.
6. MyBatis로 SQL을 직접 실행한다.
7. CRUD 전체 흐름을 완성한다.
8. Validation / Exception / Transaction을 붙인다.
9. 테스트한다.
10. 마지막에 레거시 Spring과 비교한다.
```

레거시부터 시작하지 않는다.

현대 Spring을 먼저 이해하고, 마지막에:

```text
"Boot가 해주던 게 여기서는 수동 설정으로 나와 있구나."
```

라고 연결하는 걸 목표로 한다.
