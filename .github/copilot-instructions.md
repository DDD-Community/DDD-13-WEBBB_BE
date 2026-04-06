# Copilot All-in-One Instructions

작성자: @현호

이 파일 하나로 GitHub Copilot이 다음을 우선 지키게 한다.
- CODE_RULES 준수 여부 점검
- 도메인 분리 상태 점검
- DDD + Layered Architecture 준수 여부 점검
- 한국어 PR 메시지 작성
- 한국어 리뷰 코멘트 작성

## 기본 전제
- 이 저장소는 Spring Boot 3 + Java 21 기반 백엔드다.
- 최우선 기준은 가독성, 책임 분리, 일관성이다.
- 기존 네이밍, 포맷, 폴더 구조 컨벤션을 우선 준수한다.
- 답변, 리뷰, PR 메시지는 기본적으로 한국어로 작성한다.

## 아키텍처 핵심 규칙
- DDD + Layered Architecture를 지킨다.
- 의존 방향은 항상 `interfaces → application → domain ← infrastructure` 이다.
- Domain Layer는 순수 Java 코드로 유지하며, 외부 SDK, HTTP/DB 구현 세부사항에 의존하지 않는다. 단, JPA 어노테이션(@Entity, @Id 등)은 허용한다.
- Application Layer는 유스케이스 조합과 트랜잭션 경계(@Transactional)를 담당한다.
- Infrastructure Layer는 QueryDSL 복잡 쿼리, 외부 API 클라이언트, 메시징 등 구현 세부사항만 담당한다.
- Interface Layer는 Controller, Request/Response DTO에 집중한다.
- 외부 API(Google OAuth 등)도 하나의 독립 도메인처럼 분리한다.

## 계층별 금지/권장 규칙
- Controller에는 비즈니스 로직을 넣지 않는다. Controller는 Service(application)만 호출한다.
- Controller가 Repository를 직접 호출하지 않는다.
- Controller는 ResponseEntity를 사용하여 상태코드를 메서드 안에서 명시한다.
- Repository 인터페이스는 domain 패키지에 선언하고, 복잡한 쿼리 구현체만 infrastructure에 둔다.
- Repository는 영속성 책임만 가지며 도메인 정책 판단을 하지 않는다.
- 다른 도메인의 데이터가 필요하면 해당 도메인의 Service를 주입받는다. 다른 도메인의 Repository를 직접 주입하지 않는다.
- 순환 의존이 생기면 공통 로직을 별도 도메인으로 분리하거나 조회 책임을 한쪽으로 통일한다.
- 엔티티를 API 응답으로 직접 반환하지 않는다. 반드시 DTO로 변환한다.

## 네이밍 규칙

### 기본 스타일
- 변수/메서드는 camelCase (userEmail)
- 패키지명은 단어가 달라지더라도 무조건 소문자 (frontend, useremail)
- URL, 파일명은 kebab-case (/user-email-page)
- ENUM, 상수는 대문자 + 언더스코어 (NORMAL_STATUS)
- 함수명은 소문자로 시작하고 동사로 네이밍 (getUserId(), isNormal())
- 클래스/인터페이스/Enum은 명사 + UpperCamelCase (UserEmail)
- 객체 이름을 함수 이름에 중복해서 넣지 않는다 (line.getLength() O / line.getLineLength() X)
- 컬렉션은 복수형을 사용하고 컬렉션명을 넣지 않는다 (List ids)
- 의도가 드러난다면 되도록 짧은 이름을 선택한다 (getUser() O / retreiveUser() X)
- 함수는 하나의 역할을 수행한다. 부수효과가 있다면 메서드명으로 나타낸다 (moveAndKill())

### 메서드 네이밍 가이드

| 구분 | 의미 | 상황 | 예시 |
|---|---|---|---|
| get | 존재한다고 확신하는 데이터 | 필드 접근, 존재 보장 참조 | getUserId(), getName() |
| find | 존재할 수도 없을 수도 있는 탐색 | Repository, 조건 검색 | findUserById(), findOrdersByDate() |
| read | 외부 리소스/IO 읽기 | 파일, HTTP, DB 레코드 | readFile(), readFromDB() |

### Controller / Service 메서드 네이밍

| 요청 | Controller | Service |
|---|---|---|
| 목록 조회 | readXXX | getXXXs, findXXX |
| 단건 상세 조회 | readXXX | getXXX, findXXX |
| 등록 | createXXX | addXXX |
| 수정 | updateXXX | modifyXXX |
| 삭제 | deleteXXX | removeXXX |

### 정적 팩토리 메서드명
- from: 파라미터 1개
- of: 파라미터 2개 이상

## 메서드 작성 순서
- RCUD 순서 (read → create → update → delete)
- public 메서드 아래에 관련 private 메서드를 바로 위치
- private 메서드가 여러 곳에서 사용되면 가장 마지막 public 뒤에 정리

## 접근제어자 순서
- static → public → private 순으로 작성

## 어노테이션
- 길이가 짧은 순서대로 위에서부터 배치 (피라미드)

## Java / Spring Boot 규칙
- Java 21 기능을 적극 활용한다 (record, sealed class, pattern matching 등).
- DTO는 record를 사용한다.
- 엔티티는 @Getter + @NoArgsConstructor(access = PROTECTED) + 정적 팩토리 메서드 패턴을 사용한다.
- Setter는 사용하지 않는다. 상태 변경은 의미 있는 도메인 메서드로 한다.
- Lombok은 @Getter, @NoArgsConstructor, @RequiredArgsConstructor, @Builder 정도만 사용한다. @Data, @Setter는 금지한다.
- 의존성 주입은 생성자 주입(@RequiredArgsConstructor)을 사용한다. @Autowired 필드 주입은 금지한다.
- Service 클래스에는 기본으로 @Transactional(readOnly = true)를 달고, 변경 메서드에만 @Transactional을 건다.
- 환경변수는 application.yml + @Value 또는 @ConfigurationProperties를 통해 접근하고, 하드코딩하지 않는다.
- 로그에 개인정보, 토큰, 비밀값을 남기지 않는다.
- id는 null 의미를 추가하기 위해 Long 타입을 사용한다.

## Domain 규칙
- equals & hashCode를 재정의한다. equals는 id만 비교하며 instanceof로 구현한다.
- 생성자에서 null 검사와 규칙 검사를 실시한다.

## DTO 규칙
- DTO는 Controller → Service까지 전달되며, 변환은 Service에서 처리한다.
- DTO 관련 정적 팩토리 메서드를 DTO 내부에서 활용한다 (from, of).
- Request DTO 유효성 검사는 null 검사만 한다. 도메인 규칙은 도메인 안에서 검사한다.
- DTO는 record 문법을 사용한다.

## 예외 처리
- Custom 예외를 상태 코드와 title을 지정하여 사용한다.
- 응답 형식: `{ "type": "/요청/uri", "title": "에러 제목", "status": 400, "detail": "에러 상세 메시지" }`

## 테스트 규칙

### Controller
- RestAssured 인수 테스트만 작성한다.
- 해피케이스 테스트 모두 작성 (API 당 테스트).
- 각 상태코드마다 대표 케이스 1개 이상 테스트 (401, 403, 404 등).

### Service
- H2 DB를 사용한 통합 테스트. Mocking 대신 fake DB를 사용하여 리팩터링 내성을 확보한다.

### Repository
- @Query를 직접 작성한 경우에만 테스트한다.
- JpaRepository 기본 제공 메서드나 쿼리 메서드는 테스트하지 않는다.

### 테스트 네이밍
- @DisplayName으로 한국어 테스트명을 작성한다.
- 메서드명은 영어로 간단하게: 테스트 메서드명 + 상황 간단 설명.
- BDD 스타일 (Given / When / Then).

## API 문서화
- Swagger를 사용한다.

## Copilot이 코드 제안/리뷰 시 반드시 점검할 것
1. 도메인 경계가 명확한가 (다른 도메인 Repository 직접 주입 금지)
2. DDD Layered Architecture가 지켜졌는가 (interfaces → application → domain ← infrastructure)
3. 의존 방향이 올바른가 (domain이 다른 레이어를 import하지 않는가)
4. Controller에 비즈니스 로직이 없는가
5. Repository가 영속성 책임만 가지는가
6. 엔티티를 API 응답으로 직접 반환하지 않는가
7. DTO 검증, 예외 처리, 네이밍 컨벤션이 누락되지 않았는가
8. Setter 대신 도메인 메서드를 사용하는가
9. 순환 의존이 발생하지 않는가
10. 메서드 작성 순서(RCUD), 접근제어자 순서, 어노테이션 순서가 맞는가

## PR 리뷰 작성 규칙
- 반드시 한국어로 작성한다.
- 먼저 머지 전 필수 수정 사항을 찾고, 그 다음 권장 개선 사항을 제안한다.
- DDD 계층 위반, 도메인 누수, 트랜잭션 경계 누락, 검증 누락, 보안 이슈를 최우선으로 본다.
- 각 지적에는 가능하면 파일 또는 위치, 위반 규칙, 문제 이유, 수정 방향을 함께 적는다.
- 문제가 없다면 억지로 지적하지 말고 왜 괜찮은지 설명한다.

리뷰 출력 형식:
### 1. 머지 전 필수 수정
### 2. 권장 개선
### 3. 잘한 점
### 4. 최종 판단
- DDD/LAYER 준수: 좋음 | 보통 | 미흡
- 도메인 분리: 좋음 | 보통 | 미흡
- 즉시 수정 필요 여부: 예 | 아니오

## PR 메시지 작성 규칙
- 반드시 한국어로 작성한다.
- 과장 없이 실제 변경 사항만 작성한다.
- 도메인 분리, 레이어 변경, 의존 방향 변화, 트랜잭션 경계 변화는 명시적으로 드러낸다.
- 테스트 여부와 확인 방법을 적는다.
- 리뷰어가 어디를 집중해서 봐야 하는지 분명히 적는다.
