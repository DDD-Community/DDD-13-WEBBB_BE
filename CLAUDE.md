# DDD-13-WEBBB_BE

Java 21 + Spring Boot 3.4.5 백엔드. DDD + Layered Architecture.

## Stack
- Java 21 (toolchain), Spring Boot 3.4.5, Gradle
- Spring Data JPA + QueryDSL 5.1, MySQL, Redis
- Spring Security, Spring Validation
- springdoc-openapi 2.8.6 (Swagger UI)
- Lombok, Spotless + Google Java Format **AOSP (4-space indent)**

## 명령어
| 동작 | 명령 |
|---|---|
| 빌드 | `./gradlew build` |
| 테스트 | `./gradlew test` |
| 단일 테스트 | `./gradlew test --tests <FQN>` |
| 포맷 적용 | `./gradlew spotlessApply` |
| 포맷 검사 | `./gradlew spotlessCheck` |
| 실행 | `./gradlew bootRun` |

## 아키텍처 의존 방향
`interfaces → application → domain ← infrastructure`

- `domain`: 외부 SDK / HTTP 의존 금지. JPA 어노테이션(`@Entity`, `@Id` 등)은 허용.
- `application`(Service): 유스케이스 + `@Transactional` 경계.
- `interfaces`(Controller): HTTP ↔ DTO 변환만. Repository 직접 호출 금지.
- `infrastructure`: QueryDSL 구현체, 외부 API 클라이언트 등 기술 세부.

## 작업 시 우선 점검
- DTO는 `record`. 엔티티는 `@Getter + @NoArgsConstructor(access = PROTECTED) + 정적 팩토리`.
- 의존성 주입은 생성자(`@RequiredArgsConstructor`)만. `@Autowired` 필드 주입 금지.
- `@Setter`, `@Data` 사용 금지. 상태 변경은 의미 있는 도메인 메서드.
- Service 클래스 기본값 `@Transactional(readOnly = true)`, 변경 메서드에만 `@Transactional`.
- 엔티티를 Controller 응답으로 직접 반환 금지 → record 응답 DTO로 변환.
- 다른 도메인 Repository 직접 주입 금지 → 그 도메인의 Service를 주입.
- 커밋 전 `./gradlew spotlessCheck` 통과.
- PR/리뷰는 한국어.

## 상세 규칙 (반드시 규칙을 참고하여 개발한다.)
1. 코드를 작성할 때는 항상 **코딩 원칙 7가지: @docs/CODE_RULES.md**을 참고하여 작성한다.
2. PR을 올리기 전에는 **팀 컨벤션 (네이밍 / 메서드 순서 / 테스트): @.github/copilot-instructions.md**을 참고하여 평가한다.
3. 설계를 진행하기 전에는 **아키텍처 문서: @docs/architecture.md**를 항상 참조한다.
4. 새 AI 기능을 추가할 때는 **AI 기능 추가 가이드: @docs/ai-feature-guide.md**를 따른다. Claude/OpenAI 어댑터를 새로 만들지 않고 `AiGateway`를 주입해 사용한다.
5. 커밋 메시지는 `release-please` 호환 Conventional Commit 형식을 따른다.
   - 형식: `type: 제목` 또는 `type(scope): 제목`
   - 유형: `feat` / `fix` / `refactor` / `docs` / `test` / `chore`
   - 브레이킹 체인지: `feat!:` 또는 `feat(scope)!:`
   - 예시: `feat(ai): AI 공통 감정 분석 서비스 추가`, `fix: 존재하지 않는 환율 제거`
   - `[BE]` 같은 접두사 금지. `type:` 없이 시작 금지.
6. GitHub 이슈를 작성할 때는 **이슈 템플릿: @.github/ISSUE_TEMPLATE/**을 참고한다. 작업 유형에 맞는 템플릿(feat/fix/refactor/docs/build)을 선택해 항목을 채운다.
7. PR을 작성할 때는 **PR 템플릿: @.github/pull_request_template.md**의 항목(변경 내용 & 이유 / 테스트 방법 / 관련 이슈)을 빠짐없이 채운다.