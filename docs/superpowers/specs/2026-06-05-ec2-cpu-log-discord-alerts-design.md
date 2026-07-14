# EC2 CPU/로그 → Discord 알림 설계서

- 작성일: 2026-06-05
- 대상: WEBBB BE (Spring Boot 3.4.5 / Java 21), AWS 계정 `729859598191`, EC2 배포
- 검증: codex(비용/사실관계) + omc-architect(구조/운영) + omc-critic(적대적) 3중 리뷰 반영
- 제약: **AWS Free Tier 내** · IaC = CloudFormation · **앱 Java 코드 무수정**

## 1. 목표

1. **CPU 피크 알림**: EC2 CPU가 지속적으로 높으면 알림.
2. **로그 반복 알림**: 특정 예외/키워드가 일정 시간 내 N회 이상이면 알림.
3. 두 알림 모두 **Discord 채널 `1512246623642718308`** 에 **이대리 웹훅**으로 게시.

## 2. 아키텍처

```
[EC2 CPUUtilization(기본 5분)] ───────────────► [CW Alarm: CPU≥80%, 3주기 중 2]
                                                         │
[앱 STDOUT 로그] ─(런타임별 수집: §6)─► [CW Logs /webbb/app (Retention 14d)]
        + CloudWatch Agent multi_line_start_pattern 으로 스택트레이스 1이벤트 병합
                                                         │
                              [Metric Filter: 예외/키워드 → AppErrorCount (defaultValue 0)]
                                                         │
                              [CW Alarm: Sum≥N / 5분]
                                                         │
   CPU·로그 Alarm ──(ALARM/OK)──► [SNS: webbb-alerts] ──► [Lambda(py3.12) 변환기]
                                                              │ timeout 8s, 429 backoff
                                                              │ 웹훅URL=SSM SecureString
                                                              ▼
                                  POST {Discord 웹훅 URL}  (username=이대리)
                                                              │ 실패(async 2회 재시도 후)
                                                              ▼  [DLQ(SQS)]
   [Self-monitoring]
   · EventBridge(1일 1회) → Lambda → Discord "✅ 정상 동작 중" (데드맨 스위치)
   · Lambda Errors / DLQ depth Alarm ──► [SNS: webbb-ops] ──► 이메일 (Discord와 분리된 경로)
```

**핵심 사실**: SNS는 Discord로 직접 전송 불가(SNS는 email/SMS/HTTP(S)/SQS/Lambda만 지원, Discord 웹훅 JSON 포맷과 불일치 — SNS HTTP 구독은 자체 봉투 포맷+confirm 핸드셰이크) → **Lambda 변환기 필수**. (codex 확인)

## 3. 설계 결정 및 근거

| 결정 | 내용 | 근거 |
|---|---|---|
| Discord 전송 | **이대리 웹훅(Webhook)** | 봇이 없는 것으로 확인 → 웹훅 채택. 리뷰어 3인 권고와 일치, 레포 기존 PR 알림도 웹훅 사용. 토큰·서버멤버십·채널권한 불필요 |
| 웹훅 URL 보관 | SSM Parameter Store **SecureString** | Secrets Manager는 유료. SSM standard + aws/ssm 키는 Free Tier |
| CPU 알람 정책 | EvaluationPeriods 3 / Datapoints 2 | 순간 출렁임(flapping) 억제. 로그 알람과 정책 분리 (architect) |
| 로그 알람 정책 | Sum≥N / Period300 / Eval1 / Datapoints1 | "5분 내 N회" 의미와 일치 (codex 권장값) |
| 멀티라인 | Agent `multi_line_start_pattern` | Java 스택트레이스를 1이벤트로 → 예외 1건=1카운트 (B3) |
| self-monitoring | Heartbeat + Lambda/DLQ 알람(이메일 별도경로) | 알림 시스템 자체 장애 탐지 (B4). 같은 경로 재사용 시 무한루프 → 분리 |
| Lambda 코드 | Python 3.12 인라인(urllib, 무의존) | 패키징/S3 불필요. 4KB 이내 유지 |
| 로그 본문 | embed엔 메타데이터만(알람명/상태/사유/시각) | 민감정보 Discord 유출 방지 (critic S2) |

## 4. 컴포넌트

### 4.1 CPU 알람
- `AWS/EC2 CPUUtilization`, Dimension `InstanceId`, 기본(5분) 모니터링 (상세 1분은 유료 → 제외)
- 평균 ≥ 80%, Period 300, EvaluationPeriods 3, DatapointsToAlarm 2, TreatMissingData notBreaching
- AlarmActions + OKActions → `webbb-alerts`
- 한계: 5분 해상도라 순간(2~3분) 스파이크는 희석될 수 있음 — "피크"=지속 고부하 기준.

### 4.2 로그 수집 (§6 런타임별)
- LogGroup `/webbb/app`, **RetentionInDays 14** (5GB/월 비용 방어, 이 스택이 소유)
- Agent 사용 시 `multi_line_start_pattern = "^\d{4}-\d{2}-\d{2}"`

### 4.3 Metric Filter + 로그 알람
- 패턴(기본): `?ERROR ?Exception` (파라미터로 특정 예외 클래스명 지정 가능). **실측 로그로 콘솔 Test Pattern 검증 필수**
- 메트릭 `WEBBB/App AppErrorCount`, value 1, **defaultValue 0**
- 알람: Sum ≥ N(기본 10) / 300 / Eval1 / Datapoints1, TreatMissingData notBreaching
- 한계: N=에러 "로그 라인 수"≈사건 수. 운영 초기 실측 보정.

### 4.4 Lambda 변환기 (Python 3.12)
- 트리거 SNS(`webbb-alerts`), timeout 8s, urllib만
- SNS→CloudWatch Alarm JSON 파싱 → embed(알람명/상태전이/사유/리전/시각) → 웹훅URL(SSM, 캐시) → Discord 웹훅 POST(username=이대리)
- 429 시 `Retry-After` 1회 backoff, 실패 시 raise → SNS 재시도 → DLQ
- 웹훅 URL **로깅 금지** (URL 자체가 시크릿)
- Heartbeat 모드: SNS Records 없는 이벤트(EventBridge)면 "정상 동작 중" 메시지

### 4.5 Self-monitoring
- Heartbeat: EventBridge `rate(1 day)` → Lambda → Discord 핑
- Lambda Errors > 0 Alarm → `webbb-ops`(이메일)
- DLQ(SQS) + depth>0 Alarm → `webbb-ops`

### 4.6 IaC
- 단일 CloudFormation 템플릿 `infra/monitoring/cloudwatch-discord-alerts.yaml`
- 웹훅 URL 값은 템플릿에 미포함 (SSM에 사전 등록)

## 5. 보안 하드닝 (웹훅 URL)
- 웹훅 URL: SSM SecureString `/webbb/monitoring/discord-webhook-url`
- Lambda IAM 최소권한: `ssm:GetParameter`는 **해당 파라미터 ARN으로만**, `kms:Decrypt`는 `kms:ViaService=ssm.<region>.amazonaws.com` 조건으로만, Logs 기본 + DLQ `sqs:SendMessage`만
- 웹훅은 채널이 URL에 종속 → 서버 멤버십/채널 권한 설정 불필요 (봇 대비 폭발 반경 작음)
- 어떤 로그/embed에도 웹훅 URL·민감정보 미포함

## 6. Step 0 — 배포 런타임 분기 (구현 1단계, 1줄 확인)
EC2 안에서 `hostname` 확인 후 한 번만 판별. 결과별 로그 수집:

| 런타임 | 로그 수집 | 앱 영향 | Agent |
|---|---|---|---|
| Docker | `awslogs` 로그 드라이버 (컨테이너 STDOUT 직결) | 없음 | 불필요 (권장) |
| systemd + journald | Agent journald 수집 | 없음(진짜 무수정) | 필요 |
| systemd(파일) / nohup | `logging.file.name` 1줄 or `nohup.out` + Agent file tail | yml 1줄(코드 아님) | 필요 |

## 7. Free Tier 비용 (codex 정정 반영)
- 알람 ~4개(CPU/로그/LambdaErrors/DLQ) < 10 alarm metrics 무료
- 커스텀 메트릭 1개(AppErrorCount) < 10개 무료
- Lambda/SNS/SQS/EventBridge/SSM(standard)/KMS(aws/ssm, 20k req/월): 사용량 극소 → 실질 0 수렴 ("완전 무료" 단정은 회피)
- **유일한 비용 리스크**: CloudWatch Logs 5GB/월 초과 → Retention 14d + ERROR 위주 수집으로 방어

## 8. 미해결 / 배포 시 확정
1. 배포 런타임/로그 경로 (Step 0)
2. 리전 / InstanceId (Step 0 메타데이터)
3. 예외/키워드 실제 패턴 + 멀티라인 검증
4. CPU/로그 임계치 최종값 (운영 튜닝)
5. 채널 `1512246623642718308`에 웹훅 생성 + URL 확보
6. self-monitoring 이메일 주소

## 9. e2e 검증 절차 (배포 후 필수)
- 합성 에러: 매칭 패턴 N+1회 로그 주입 → 5분 내 Discord 도달 → OK 복귀 확인
- 합성 CPU: 부하 발생 → CPU 알람 발화
- Heartbeat: 스케줄 수동 트리거 → Discord 핑
- 실패경로: 웹훅 URL 일시 무효화 → DLQ 적재 + webbb-ops 이메일 도달
