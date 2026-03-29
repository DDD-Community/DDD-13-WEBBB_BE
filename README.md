# WEBBB BE

## 로컬 개발 환경 설정

### 사전 요구사항

- Java 21+
- Docker & Docker Compose

### 1. 인프라 실행

```bash
docker-compose up -d
```

| 서비스 | 포트 | 계정 |
|--------|------|------|
| MySQL  | 3306 | webbb / webbb |
| Redis  | 6379 | - |

DB명: `webbb`

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

또는 IntelliJ에서 `WebbbApplication.java` 실행

### 3. 빌드

```bash
./gradlew build
```

### 인프라 종료

```bash
docker-compose down
```

데이터까지 초기화하려면:

```bash
docker-compose down -v
```
