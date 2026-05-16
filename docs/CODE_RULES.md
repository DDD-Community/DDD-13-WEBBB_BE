# 코딩 원칙 7가지

이 프로젝트에서 코드를 작성할 때 반드시 지켜야 하는 핵심 원칙입니다.

---

## 1. 가독성 — 의도를 이름으로 드러낸다

코드는 실행보다 읽히는 시간이 더 많다. 이름만으로 의도를 전달한다.

- 변수·메서드는 역할을 설명하는 이름을 사용한다. 약어, 한 글자 변수는 지역 루프 변수(`i`, `e`)에만 허용한다.
- 메서드는 하나의 역할만 수행한다. 부수효과가 있으면 이름에 드러낸다 (`moveAndKill()`).
- 객체 이름을 메서드 이름에 중복하지 않는다 (`line.getLength()` ✅ / `line.getLineLength()` ❌).
- 컬렉션은 복수형으로 쓰고 타입명을 포함하지 않는다 (`List<Long> ids` ✅ / `List<Long> idList` ❌).
- 메서드 작성 순서: RCUD (read → create → update → delete). public 메서드 바로 아래에 관련 private 메서드 배치.

---

## 2. 책임 분리 — 한 클래스는 한 가지 이유로만 바뀐다

- Controller: HTTP ↔ DTO 변환만. 비즈니스 로직 없음. Repository 직접 호출 금지.
- Service(application): 유스케이스 조합 + `@Transactional` 경계. 기본값 `@Transactional(readOnly = true)`, 변경 메서드에만 `@Transactional`.
- Domain: 순수 Java. 외부 SDK·HTTP 의존 금지. 도메인 규칙과 상태 변경 메서드를 여기에 둔다.
- Infrastructure: QueryDSL 복잡 쿼리, 외부 API 클라이언트 등 기술 세부사항만.
- 엔티티를 API 응답으로 직접 반환하지 않는다. 반드시 record DTO로 변환한다.

---

## 3. 불변성 선호 — 상태 변경은 명시적으로 한다

- `@Setter`, `@Data` 사용 금지. 상태 변경은 의미 있는 도메인 메서드로만 한다.
- DTO는 `record`를 사용한다. 불변이고 컴팩트하다.
- 엔티티는 `@Getter + @NoArgsConstructor(access = PROTECTED) + 정적 팩토리 메서드` 패턴을 사용한다.
- `equals & hashCode`는 id만 비교한다 (`instanceof`로 구현).

---

## 4. 명시적 의존 — 주입받는 것만 의존한다

- 의존성 주입은 생성자 주입(`@RequiredArgsConstructor`)만 사용한다. `@Autowired` 필드 주입 금지.
- 다른 도메인 데이터가 필요하면 해당 도메인의 Service를 주입받는다. Repository 직접 주입 금지.
- 인터페이스(포트)를 주입받고 구현체를 직접 참조하지 않는다.
- 환경변수는 `application.yml + @ConfigurationProperties`로 접근하고 하드코딩하지 않는다.

---

## 5. 경계에서만 검증한다 — 도메인 규칙은 도메인 안에서

- Request DTO는 null·빈값만 검증한다 (`@NotBlank`, `@NotNull`). 도메인 규칙 검증은 도메인 안에서만.
- 생성자·정적 팩토리에서 null 검사와 불변식 검사를 실시한다.
- Custom 예외(`AppException`)를 사용하고, 에러 코드(`ErrorCode`)에 HTTP 상태와 메시지를 함께 정의한다.
- 로그에 개인정보·토큰·비밀값을 남기지 않는다.

---

## 6. 테스트 가능성 — 외부 의존을 인터페이스 뒤에 숨긴다

- 도메인 로직은 Spring 없이 단위 테스트가 가능해야 한다.
- Service 테스트는 H2 DB 통합 테스트를 사용한다. 실제 DB 흐름을 검증해 리팩터링 내성을 확보한다.
- Controller 테스트는 RestAssured 인수 테스트. 해피케이스 + 대표 에러 케이스(401, 403, 404) 포함.
- AI 서비스 테스트는 `AiGateway`를 목으로 주입해 실제 LLM 호출 없이 테스트한다.
- `@Query`를 직접 작성한 Repository만 별도 테스트. JpaRepository 기본 메서드는 테스트하지 않는다.

---

## 7. 일관성 — 팀이 같은 형식으로 쓴다

- 포맷: Spotless + Google Java Format AOSP (4-space indent). 커밋 전 `./gradlew spotlessApply` 실행.
- 네이밍: 변수·메서드는 camelCase / 패키지는 소문자 / URL·파일명은 kebab-case / 상수는 UPPER_SNAKE_CASE.
- 어노테이션 순서: 길이가 짧은 순서대로 위에서 아래 (피라미드).
- 접근제어자 순서: `static → public → private`.
- Lombok은 `@Getter`, `@NoArgsConstructor`, `@RequiredArgsConstructor`, `@Builder`만 사용한다.
- `id`는 null 의미를 살리기 위해 `Long` 타입 사용 (`long` 금지).
- PR·리뷰는 한국어로 작성한다.
