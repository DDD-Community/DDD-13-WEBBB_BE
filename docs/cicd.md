# CI/CD 파이프라인 가이드

## 전체 흐름

```
feat/xxx 브랜치 작업
    │
    ▼
PR to main ──→ CI (spotless + test + docker build check)
    │                ❌ 실패 → 머지 차단
    ▼                ✅ 통과
main에 머지 ──→ CD (build → GHCR push → EC2 배포 → health check)
    │                ❌ health check 실패 → 워크플로우 실패 처리
    ├──→ Release Please → Release PR 자동 생성
    │
    ▼
Release PR 머지 ──→ GitHub Release + 태그 + CHANGELOG 자동 업데이트
```

---

## 워크플로우 상세

### 1. CI — `.github/workflows/ci.yml`

| 항목 | 값 |
|---|---|
| 트리거 | `main` 브랜치로 PR이 열릴 때 |
| 목적 | 코드 품질 검증 (머지 전 게이트) |

**실행 단계:**

1. **Spotless check** — Google Java Format 규칙에 맞는지 검사. 포맷이 안 맞으면 실패
2. **Gradle test** — 테스트 실행 (현재는 부트 기동 테스트 수준이므로, 도메인 로직이 추가되면 테스트도 함께 보강 필요)
3. **Docker build check** — Dockerfile이 정상 빌드되는지 확인 (이미지 push는 안 함)

> GitHub 레포 Settings > Branches > Branch protection rules에서
> `main` 브랜치에 "Require status checks to pass before merging"을 설정하면
> CI가 통과해야만 머지할 수 있습니다.

---

### 2. CD — `.github/workflows/cd.yml`

| 항목 | 값 |
|---|---|
| 트리거 | `main` 브랜치에 push (= PR 머지) |
| 목적 | 서버 자동 배포 |

**실행 단계:**

1. **이미지 이름 소문자 변환** — GHCR은 대문자를 허용하지 않으므로 `github.repository`를 소문자로 변환
2. **테스트 재실행** — 머지 후 최종 검증
3. **Docker 이미지 빌드 & GHCR push** — 두 개 태그로 push
   - `latest` — 항상 최신 빌드를 가리킴
   - `<commit-sha>` — 특정 커밋으로 롤백할 때 사용
4. **SCP로 compose 파일 전송** — EC2의 `~/app`에 최신 `docker-compose.prod.yml` 배치
5. **SSH 접속 후 배포** — `docker compose pull` → `up -d`로 컨테이너 교체
6. **Health check** — 최대 150초(5초 × 30회) 대기하며 `/actuator/health` 확인
   - 성공 시 `exit 0` → 워크플로우 성공
   - 실패 시 앱 로그 출력 후 `exit 1` → **워크플로우 실패 처리**

---

### 3. Release Please — `.github/workflows/release-please.yml`

| 항목 | 값 |
|---|---|
| 트리거 | `main` 브랜치에 push (= PR 머지) |
| 목적 | 버전 관리 및 릴리즈 자동화 |

**동작 방식:**

1. main에 push가 오면 커밋 메시지를 분석해서 **Release PR을 자동 생성/갱신**
2. Release PR에는 `CHANGELOG.md` 생성/업데이트와 `version.txt` 버전 범프가 포함될 수 있음
   - 현재 `release-type: simple` 설정이며, 별도 config 파일(`release-please-config.json` 등)은 없음
   - 첫 릴리즈 시 Release Please가 이 파일들을 자동 생성함
3. Release PR을 머지하면 **GitHub Release + Git 태그**가 자동 생성

---

## Conventional Commits 규칙

Release Please가 버전을 자동으로 올리려면 커밋 메시지가 아래 형식을 따라야 합니다.

| 접두사 | 의미 | 버전 변경 | 예시 |
|---|---|---|---|
| `feat:` | 새 기능 | Minor (0.**1**.0) | `feat: 소셜 로그인 추가` |
| `fix:` | 버그 수정 | Patch (0.0.**1**) | `fix: 토큰 만료 시 NPE 수정` |
| `feat!:` | 호환성 깨는 변경 | Major (**1**.0.0) | `feat!: API 응답 구조 변경` |
| `docs:` | 문서 | 변경 없음 | `docs: README 업데이트` |
| `chore:` | 빌드/설정 | 변경 없음 | `chore: CI 워크플로우 추가` |
| `ci:` | CI 변경 | 변경 없음 | `ci: Spotless 체크 추가` |
| `refactor:` | 리팩토링 | 변경 없음 | `refactor: 서비스 레이어 분리` |
| `test:` | 테스트 | 변경 없음 | `test: 유저 서비스 테스트 추가` |

> `docs:`, `chore:`, `ci:` 등은 릴리즈에 포함되지 않습니다.
> `feat:` 또는 `fix:`가 하나 이상 있어야 Release PR이 생성됩니다.

---

## 인프라 구성

### Docker 이미지 (Dockerfile)

Multi-stage 빌드로 최종 이미지 크기를 최소화합니다.

```
Stage 1 (빌드)    eclipse-temurin:21-jdk  →  Gradle 빌드, JAR 생성
Stage 2 (실행)    eclipse-temurin:21-jre  →  JAR만 복사해서 실행
```

의존성 레이어를 분리하여 소스 코드만 변경되었을 때 Gradle 의존성 다운로드를 캐시합니다.

### 운영 Compose (docker-compose.prod.yml)

| 서비스 | 이미지 | 역할 | 외부 포트 |
|---|---|---|---|
| app | GHCR에서 pull | Spring Boot 애플리케이션 | 8080 |
| mysql | mysql:8.0 | 데이터베이스 | 없음 (내부 전용) |
| redis | redis:7.0 | 캐시/세션 저장소 | 없음 (내부 전용) |

- app은 mysql, redis가 healthy 상태일 때만 시작 (`depends_on` + `healthcheck`)
- mysql, redis는 호스트 포트를 노출하지 않음 (Docker 내부 네트워크로만 통신)
- 모든 민감 정보는 환경변수로 주입 (하드코딩 금지)

### 운영 프로필 (application-prod.yml)

| 설정 | 값 | 이유 |
|---|---|---|
| `ddl-auto` | `validate` | 운영에서 스키마 자동 변경 방지 |
| `show-sql` | `false` | 운영 로그 노이즈 제거 |
| `actuator` | `health`만 노출 | CD health check용, 상세 정보 숨김 |
| DB/Redis 접속 | `${ENV_VAR}` | 환경변수로 주입 |

---

## GitHub Secrets 설정

레포 Settings > Secrets and variables > Actions에서 아래 값을 등록해야 합니다.

| Secret | 용도 | 예시 값 |
|---|---|---|
| `EC2_HOST` | EC2 퍼블릭 IP | `3.35.xxx.xxx` |
| `EC2_USER` | SSH 유저 | `ec2-user` |
| `EC2_SSH_KEY` | SSH 프라이빗 키 (PEM 전체 내용) | `-----BEGIN RSA...` |
| `DB_PASSWORD` | MySQL 운영 비밀번호 | - |
| `GHCR_TOKEN` | GHCR 접근용 PAT (`read:packages` 권한) | `ghp_xxxx` |

### GHCR_TOKEN 생성 방법

1. GitHub Settings > Developer settings > Personal access tokens > Tokens (classic)
2. Generate new token
3. 권한: `read:packages` 체크
4. 생성된 토큰을 레포 Secrets에 `GHCR_TOKEN`으로 등록

> `GITHUB_TOKEN`은 워크플로우 러너 안에서만 유효합니다.
> EC2에서 GHCR 이미지를 pull하려면 별도 PAT가 필요합니다.

---

## EC2 사전 준비

배포 대상 EC2에 아래가 설치되어 있어야 합니다.

```bash
# Docker 설치
sudo yum install -y docker
sudo systemctl enable docker && sudo systemctl start docker
sudo usermod -aG docker ec2-user

# Docker Compose 플러그인 설치
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 작업 디렉토리 생성
mkdir -p ~/app
```

---

## 관련 파일 목록

| 파일 | 역할 |
|---|---|
| `Dockerfile` | 멀티스테이지 Docker 빌드 |
| `docker-compose.prod.yml` | 운영 배포용 Compose |
| `src/main/resources/application-prod.yml` | 운영 Spring 프로필 |
| `.github/workflows/ci.yml` | PR 시 CI 워크플로우 |
| `.github/workflows/cd.yml` | main push 시 CD 워크플로우 |
| `.github/workflows/release-please.yml` | main push 시 릴리즈 자동화 |
