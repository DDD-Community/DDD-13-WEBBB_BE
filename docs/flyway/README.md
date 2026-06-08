# Flyway 마이그레이션 가이드

이 프로젝트는 **Flyway**로 DB 스키마를 버전 관리합니다.
엔티티를 변경할 때 반드시 마이그레이션 파일을 함께 추가해야 합니다.

---

## 로컬 환경 최초 설정 (한 번만)

`application-local.yml`은 gitignore 대상이므로 직접 추가해야 합니다.
`application-local.yml.example`을 참고하거나, 기존 파일에 아래 항목을 추가하세요.

```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 1
```

> 이 설정 없이 앱을 실행하면 아래 오류가 발생합니다:
> `Found non-empty schema(s) but no schema history table`

설정 추가 후 `./gradlew bootRun`을 실행하면 Flyway가 자동으로 초기화됩니다.

---

## 엔티티를 변경할 때

### 규칙: 엔티티 변경 = 마이그레이션 파일 필수

`@Entity` 클래스의 스키마를 변경(컬럼 추가/수정/삭제, 테이블 추가 등)하면
반드시 마이그레이션 SQL 파일을 함께 PR에 포함해야 합니다.

### 파일 위치 및 네이밍

```
src/main/resources/db/migration/
├── V1__init_schema.sql          ← 초기 스키마 (수정 금지)
├── V2__add_xxx_to_yyy.sql       ← 컬럼 추가 예시
└── V3__create_zzz_table.sql     ← 테이블 추가 예시
```

**파일명 규칙:** `V{순번}__{설명}.sql`
- `V`는 대문자
- 순번은 이전 파일보다 1 높게
- `__`는 언더스코어 두 개
- 설명은 영어 snake_case

### 예시: 컬럼 추가

```sql
-- V2__add_profile_image_to_users.sql
ALTER TABLE users
    ADD COLUMN profile_image VARCHAR(500) AFTER nickname;
```

### 예시: 테이블 추가

```sql
-- V3__create_notification_table.sql
CREATE TABLE notification
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BIT(1)      NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
```

---

## 절대 하면 안 되는 것

### ❌ 기존 마이그레이션 파일 수정

한 번 적용된 파일(`V1__`, `V2__`, ...)은 **절대 수정하지 않습니다.**

Flyway는 각 파일의 체크섬을 저장해두고, 파일이 변경되면 아래 오류로 앱 구동을 거부합니다:

```
Checksum mismatch for migration version 1
```

변경이 필요하면 항상 **새 버전 파일**을 추가하세요.

---

## 배포 시 동작 방식

별도 작업 없이 **앱 배포만 하면 자동 적용**됩니다.

```
PR 머지 → Manual Deploy 실행
  → Docker 이미지 빌드 (SQL 파일 JAR에 내장)
  → EC2에서 docker compose up -d
  → Spring Boot 시작 시 Flyway 자동 실행
  → 미적용 마이그레이션 순서대로 적용
  → 앱 정상 기동
```

---

## 자주 묻는 질문

**Q. `ddl-auto: update`처럼 엔티티만 추가하면 안 되나요?**

안 됩니다. 전 환경이 `ddl-auto: validate`로 설정되어 있어 마이그레이션 파일 없이 엔티티를 변경하면 앱이 구동되지 않습니다.

**Q. 로컬에서 실수로 마이그레이션 파일 없이 엔티티를 변경했어요.**

로컬 DB에서 직접 `ALTER TABLE`로 컬럼을 추가한 뒤, 마이그레이션 파일을 작성하세요.
그러면 다음 실행 시 Flyway가 파일을 적용하려 하지만 컬럼이 이미 존재해 오류가 납니다.
이 경우 `flyway_schema_history` 테이블에서 해당 버전 레코드를 삭제하고 다시 시도하거나,
로컬 DB를 초기화(`DROP DATABASE → CREATE DATABASE`)하는 게 빠릅니다.

**Q. 여러 명이 동시에 마이그레이션 파일을 추가하면 버전이 충돌할 수 있지 않나요?**

PR 머지 순서대로 버전 번호를 매겨야 합니다. 머지 전 다른 PR과 버전이 겹치지 않는지 확인하세요.
겹치면 한쪽이 버전 번호를 올려서 해결합니다.
