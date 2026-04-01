# 아키텍처 가이드

## 레이어 의존 방향

```
interfaces → application → domain ← infrastructure
```

화살표는 "~를 알고 있다(의존한다)"는 방향입니다.

- `domain`은 어떤 레이어도 의존하지 않는 순수 Java 코드
- `infrastructure`는 `domain`의 인터페이스를 구현 (의존성 역전)
- `application`은 `domain`의 인터페이스만 주입받고 구현체는 모름
- `interfaces`는 `application`만 호출하고, 엔티티 대신 DTO를 반환

---

## 패키지별 파일 목록

### `global/`

전역 설정과 공통 컴포넌트를 모아둡니다. 특정 도메인에 속하지 않는 코드만 여기에 둡니다.

```
global/
├── config/
│   ├── JpaConfig.java                # QueryDSL 등 JPA 추가 설정
│   ├── SecurityConfig.java           # Spring Security 설정, 인증 필터 등록
│   └── SwaggerConfig.java            # Springdoc OpenAPI 설정
├── common/
│   ├── response/
│   │   └── ApiResponse.java          # 공통 응답 래퍼 { success, data, message }
│   ├── exception/
│   │   ├── AppException.java         # 비즈니스 예외 클래스
│   │   ├── ErrorCode.java            # 에러 코드 enum (HTTP 상태 + 메시지)
│   │   └── GlobalExceptionHandler.java  # @RestControllerAdvice
│   └── entity/
│       └── BaseEntity.java           # createdAt, updatedAt (@MappedSuperclass)
└── auth/
    ├── JwtProvider.java              # 토큰 생성, 검증, 파싱
    └── JwtAuthFilter.java            # OncePerRequestFilter, SecurityContext 등록
```

---

### `user/`

```
user/
├── domain/
│   ├── User.java                     # @Entity, 도메인 로직 포함
│   ├── UserRepository.java           # Repository 인터페이스 (Spring Data JPA 상속)
│   └── UserStatus.java               # enum (ACTIVE, INACTIVE, ...)
├── application/
│   ├── UserService.java              # 유즈케이스 구현, @Transactional
│   └── UserMapper.java               # 엔티티 ↔ DTO 변환 (필요 시)
├── infrastructure/                   # 단순 CRUD만 있으면 생략 가능
│   └── UserRepositoryImpl.java       # QueryDSL 등 복잡한 쿼리 구현
└── interfaces/
    ├── UserController.java           # @RestController, URL 매핑
    └── dto/
        ├── UserResponse.java         # API 응답 DTO
        └── UserUpdateRequest.java    # API 요청 DTO
```

---

### `auth/`

```
auth/
├── domain/
│   └── AuthToken.java                # 토큰 도메인 객체 (accessToken, refreshToken)
├── application/
│   └── AuthService.java             # OAuth 로그인 처리, 토큰 발급
├── infrastructure/
│   └── GoogleOAuthClient.java        # Google API 호출 (RestClient 등)
└── interfaces/
    ├── AuthController.java           # /auth/google/callback 등
    └── dto/
        ├── GoogleAuthRequest.java    # code, state 수신
        └── TokenResponse.java        # accessToken, refreshToken 반환
```

---

### `cohort/`

```
cohort/
├── domain/
│   ├── Cohort.java                   # @Entity
│   ├── CohortPart.java               # @Entity (기수 내 파트)
│   ├── CohortRepository.java         # Repository 인터페이스
│   └── CohortStatus.java             # enum
├── application/
│   └── CohortService.java
├── infrastructure/                   # 필요 시
│   └── CohortRepositoryImpl.java
└── interfaces/
    ├── CohortController.java
    └── dto/
        ├── CohortResponse.java
        └── CohortCreateRequest.java
```

---

## 예시 코드 (user 모듈 기준)

### 1. domain — `User.java`

JPA 엔티티와 도메인 엔티티를 분리하지 않고 하나로 사용합니다.
도메인 로직(상태 변경 메서드)도 엔티티 안에 둡니다.

```java
// user/domain/User.java
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String nickname;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    public static User create(String email, String nickname) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
```

### 2. domain — `UserRepository.java`

인터페이스를 `domain`에 선언합니다. JPA 기술이 노출되더라도 인터페이스를 여기 두는 것이 핵심입니다. `application`이 구현체(infrastructure)를 직접 몰라도 되기 때문입니다.

```java
// user/domain/UserRepository.java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

> 복잡한 쿼리가 없으면 `infrastructure/` 폴더와 구현체 없이 이 인터페이스만으로 충분합니다.

### 3. application — `UserService.java`

`UserRepository` 인터페이스(domain)만 주입받습니다. `UserRepositoryImpl`을 직접 참조하지 않습니다.

```java
// user/application/UserService.java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;  // 구현체가 아닌 인터페이스 주입

    public User getUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public User updateNickname(Long id, String nickname) {
        User user = getUser(id);
        user.updateNickname(nickname);
        return user;
    }
}
```

### 4. interfaces — `UserController.java`

`UserService`(application)만 호출합니다. 엔티티를 그대로 반환하지 않고 DTO로 변환합니다.

```java
// user/interfaces/UserController.java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;  // application 레이어만 의존

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        User user = userService.getUser(id);
        return ApiResponse.ok(UserResponse.from(user));  // 엔티티 → DTO 변환
    }

    @PatchMapping("/{id}/nickname")
    public ApiResponse<UserResponse> updateNickname(
        @PathVariable Long id,
        @RequestBody @Valid UserUpdateRequest request
    ) {
        User user = userService.updateNickname(id, request.nickname());
        return ApiResponse.ok(UserResponse.from(user));
    }
}
```

### 5. interfaces/dto — `UserResponse.java`

엔티티를 직접 노출하지 않습니다. DB 컬럼이 바뀌어도 API 스펙이 깨지지 않습니다.

```java
// user/interfaces/dto/UserResponse.java
public record UserResponse(
    Long id,
    String email,
    String nickname,
    String status
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getStatus().name()
        );
    }
}
```

---

## 도메인 간 호출

한 도메인의 Service가 다른 도메인의 데이터를 필요로 할 때의 규칙입니다.

### 핵심 규칙

- **다른 도메인의 `Service`(application)를 주입받는다**
- **다른 도메인의 `Repository`(domain)를 직접 주입받지 않는다**

```
CohortService → UserService (O)
CohortService → UserRepository (X)
```

### 올바른 예시 — 다른 도메인 Service 주입

기수(Cohort)에 유저를 등록할 때, `UserService`를 통해 유저를 조회합니다.

```java
// cohort/application/CohortService.java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CohortService {

    private final CohortRepository cohortRepository;
    private final UserService userService;  // 다른 도메인의 Service 주입 (O)

    @Transactional
    public void registerMember(Long cohortId, Long userId) {
        Cohort cohort = cohortRepository.findById(cohortId)
            .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));

        User user = userService.getUser(userId);  // UserRepository 직접 호출 아님 (O)

        cohort.addMember(user);
    }
}
```

### 잘못된 예시 — 다른 도메인 Repository 직접 주입

```java
// cohort/application/CohortService.java
@Service
@RequiredArgsConstructor
public class CohortService {

    private final CohortRepository cohortRepository;
    private final UserRepository userRepository;  // 다른 도메인 Repository 직접 주입 (X)

    public void registerMember(Long cohortId, Long userId) {
        User user = userRepository.findById(userId)  // user 도메인 내부에 직접 접근 (X)
            .orElseThrow(...);
    }
}
```

`UserRepository`를 직접 주입하면 `user` 도메인의 내부 구현에 결합됩니다.
나중에 `UserRepository`의 메서드 시그니처가 바뀌면 `CohortService`도 함께 수정해야 합니다.

### 순환 의존이 생기는 경우

A 도메인 Service가 B를 호출하고, B 도메인 Service도 A를 호출하면 순환 의존이 발생합니다.

```
UserService → CohortService → UserService  (순환 의존, X)
```

이 경우 공통 로직을 별도 도메인으로 분리하거나, 조회 책임을 한쪽으로 통일합니다.

```java
// 한쪽 방향만 의존하도록 정리
CohortService → UserService  (O)
UserService는 CohortService를 모름  (O)
```

---

## 의존 방향을 어기면 생기는 문제

| 위반 사례 | 문제 |
|---|---|
| `Controller`에서 `Repository` 직접 호출 | 비즈니스 로직이 분산되어 테스트 불가 |
| 엔티티를 API 응답으로 직접 반환 | DB 컬럼 변경이 API 스펙을 깨뜨림 |
| `Service`에서 다른 도메인 `Repository` 직접 주입 | 도메인 간 강결합 발생, 순환 의존 위험 |
| `domain`이 `application`의 클래스를 `import` | 레이어 역전, 도메인 로직 테스트 불가 |
