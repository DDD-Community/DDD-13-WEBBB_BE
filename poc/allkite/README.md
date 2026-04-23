# AI 응원 서비스 PoC (janghh)

취준생 심리 케어 AI 서비스 - Claude API 연동

## 실행 방법

### 1. 환경변수 설정
```bash
export CLAUDE_API_KEY=your-api-key-here
```

### 2. build.gradle 의존성 (메인 프로젝트에 추가 시)
```groovy
// Spring AI BOM
dependencyManagement {
    imports {
        mavenBom 'org.springframework.ai:spring-ai-bom:1.0.0'
    }
}

repositories {
    mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }
}

// 의존성
implementation 'org.springframework.ai:spring-ai-starter-model-anthropic'
```

### 3. application.yml 설정
```yaml
spring:
  ai:
    anthropic:
      api-key: ${CLAUDE_API_KEY}
      chat:
        options:
          model: claude-sonnet-4-5-20250929
```
