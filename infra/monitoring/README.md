# WEBBB 모니터링 알림 (EC2 CPU/로그 → Discord)

EC2 **CPU 피크** 와 **앱 예외/키워드 로그 N회 반복** 을 감지해 **Discord 채널(이대리 봇)** 로 알림을 보냅니다.
codex + omc(architect/critic) 3중 리뷰를 반영했고, **AWS Free Tier** 범위로 설계했습니다.

> 설계 배경/근거: [docs/superpowers/specs/2026-06-05-ec2-cpu-log-discord-alerts-design.md](../../docs/superpowers/specs/2026-06-05-ec2-cpu-log-discord-alerts-design.md)

## 구성 파일
| 파일 | 용도 |
|---|---|
| `cloudwatch-discord-alerts.yaml` | CloudFormation 템플릿(SNS·Lambda·알람·DLQ·Heartbeat 전부) |
| `lambda/discord_notifier.py` | Discord 전송 Lambda 소스(템플릿 인라인본과 동일) |
| `amazon-cloudwatch-agent.json` | EC2 로그를 CloudWatch로 보낼 때 쓰는 Agent 설정 |

---

## 한눈에 보는 흐름
```
CPU 알람 ┐
        ├─ SNS(webbb-alerts) ─ Lambda ─ Discord(이대리 봇, 채널 1512246623642718308)
로그 알람 ┘                        └ 실패 시 DLQ
Heartbeat(1일1회) ─ Lambda ─ Discord "정상 동작 중"
Lambda/DLQ 장애 ─ SNS(webbb-ops) ─ 이메일   ← Discord와 분리된 경로
```

---

## STEP 0. 런타임 확인 (딱 1번, 인스턴스 내부에서)

**브라우저**에서 AWS 콘솔 → EC2 → 인스턴스 선택 → `연결` → **Session Manager** → 검은 터미널이 열리면 거기서:

```bash
hostname            # ip-... 로 나오면 EC2 안(정상). juneseok... 면 아직 내 맥임
sudo docker ps      # WEBBB 컨테이너가 보이면 => Docker 런타임
systemctl list-units --type=service --state=running | grep -i webbb   # 보이면 => systemd 런타임
TOKEN=$(curl -s -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds:60')
curl -s -H "X-aws-ec2-metadata-token:$TOKEN" http://169.254.169.254/latest/meta-data/instance-id; echo
curl -s -H "X-aws-ec2-metadata-token:$TOKEN" http://169.254.169.254/latest/meta-data/placement/region; echo
```

확인 결과로 **InstanceId / Region / 런타임(Docker·systemd·nohup)** 을 메모해 둡니다.

---

## STEP 1. 로그를 CloudWatch로 보내기 (런타임별, 택1)

> 목표: 앱 로그가 로그 그룹 `/webbb/app` 으로 들어가게 한다. (CPU 알람은 이 단계 없이도 동작)

### A) Docker 런타임 — Agent 불필요, `awslogs` 드라이버 사용 (가장 단순)
컨테이너 실행 시 로그 드라이버를 awslogs로:
```bash
docker run ... \
  --log-driver=awslogs \
  --log-opt awslogs-region=<REGION> \
  --log-opt awslogs-group=/webbb/app \
  --log-opt awslogs-create-group=false \
  --log-opt 'awslogs-multiline-pattern=^\d{4}-\d{2}-\d{2}'
```
docker-compose면:
```yaml
services:
  app:
    logging:
      driver: awslogs
      options:
        awslogs-region: "<REGION>"
        awslogs-group: "/webbb/app"
        awslogs-multiline-pattern: '^\d{4}-\d{2}-\d{2}'
```
→ **EC2 인스턴스 역할(instance profile)** 에 `logs:CreateLogStream`, `logs:PutLogEvents` 권한 필요.

### B) systemd 런타임 — stdout을 파일로 돌리고 CloudWatch Agent로 tail
1. 유닛 파일(`/etc/systemd/system/webbb.service` 등)에 한 줄 추가(앱 Java 코드 무수정):
   ```ini
   [Service]
   StandardOutput=append:/var/log/webbb/app.log
   StandardError=append:/var/log/webbb/app.log
   ```
   `sudo systemctl daemon-reload && sudo systemctl restart webbb`
2. CloudWatch Agent 설치 + 설정(아래 "Agent 설치" 참고).

### C) nohup 런타임 — nohup.out 을 Agent로 tail
- `amazon-cloudwatch-agent.json` 의 `file_path` 를 실제 `nohup.out` 경로로 바꾸고 Agent 설치.

### CloudWatch Agent 설치 (B/C 공통)
```bash
sudo yum install -y amazon-cloudwatch-agent        # Amazon Linux (Ubuntu는 .deb)
sudo mkdir -p /var/log/webbb
# 이 레포의 amazon-cloudwatch-agent.json 을 /opt/aws/amazon-cloudwatch-agent/etc/ 에 복사 후:
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config -m ec2 -s \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
```
→ EC2 인스턴스 역할에 `CloudWatchAgentServerPolicy` 부착 필요.

> 멀티라인(`^\d{4}-\d{2}-\d{2}`)은 Java 스택트레이스를 1개 이벤트로 묶어 "예외 1건=1카운트"를 보장합니다. **실제 로그 첫 줄 형식이 다르면 이 정규식을 맞춰주세요.**

---

## STEP 2. Discord 봇 토큰을 SSM에 저장 (1번)
토큰 값은 템플릿에 넣지 않습니다. 콘솔: **Systems Manager → 파라미터 스토어 → 파라미터 생성**
- 이름: `/webbb/monitoring/discord-bot-token`
- 유형: **SecureString** (KMS 키: `alias/aws/ssm` 기본)
- 값: 이대리 봇 토큰

> 이대리 봇이 대상 서버에 있고, 채널 `1512246623642718308` 에 **메시지 보내기** 권한이 있어야 합니다.

---

## STEP 3. CloudFormation 스택 배포

### 방법 ① 콘솔 (CLI 막혀 있을 때 권장)
1. 콘솔 → **CloudFormation → 스택 생성 → 새 리소스 사용**
2. "템플릿 파일 업로드" → `cloudwatch-discord-alerts.yaml` 선택 → 다음
3. 파라미터 입력:
   - `InstanceId`: STEP 0에서 확인한 값
   - `OpsEmail`: 파이프라인 장애 통보 받을 이메일
   - 나머지(채널ID·토큰파라미터명·임계치)는 기본값 확인/조정
4. 다음 → **"IAM 리소스를 생성할 수 있음" 체크** → 스택 생성
5. 상태가 `CREATE_COMPLETE` 면 완료

### 방법 ② CLI (aws login 권한 풀리면)
```bash
aws cloudformation deploy \
  --stack-name webbb-monitoring \
  --template-file infra/monitoring/cloudwatch-discord-alerts.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --region <REGION> \
  --parameter-overrides InstanceId=<i-xxxx> OpsEmail=<you@example.com>
```

---

## STEP 4. 이메일 구독 확인
배포 후 `OpsEmail` 주소로 **SNS 구독 확인 메일**이 옵니다 → **Confirm subscription** 클릭. (안 하면 ops 알림 안 옴)

---

## STEP 5. 동작 검증 (e2e)
1. **로그 알람**: EC2에서 매칭 패턴을 임계치+1회 로그파일에 주입
   ```bash
   for i in $(seq 1 11); do echo "$(date '+%Y-%m-%d %H:%M:%S') ERROR test alarm $i" | sudo tee -a /var/log/webbb/app.log >/dev/null; done
   ```
   → 5분 내 Discord에 🔴 알림 → 멈추면 🟢 OK 알림 확인
2. **CPU 알람**: `sudo yum install -y stress && stress --cpu 2 --timeout 600` → CPU 알람 발화 확인
3. **Heartbeat**: 콘솔 Lambda → `webbb-discord-notifier` → 테스트 이벤트 `{}` 실행 → Discord "✅ 정상 동작 중"
4. **실패 경로**: SSM 토큰을 잠깐 틀린 값으로 → 알람 1건 유발 → DLQ 적재 + ops 이메일 도착 확인 → 토큰 복구

---

## 비용 (Free Tier) — codex 독립 검증 완료, 정상 운영 시 $0
- **보유만으로 과금되는 리소스 없음.** 기대는 한도는 대부분 **영구 무료(always-free)** → 가입 1년 뒤에도 $0
- 알람 5개(<10) · 커스텀 메트릭 1개(<10) · Lambda/SNS/SQS/EventBridge/SSM(standard)/KMS(aws/ssm) 사용량 극소
- **유일한 변수 = CloudWatch Logs 수집 5GB/월** (앱 로그 + Lambda 자체 실행 로그 합산)
  - 비용 핵심은 **수집(ingestion, ~$0.76/GB)**. ⚠️ **보관기간(retention)은 "저장"만 줄일 뿐 수집량과 무관** — 7일로 줄여도 수집 5GB 한도 방어엔 효과 없음
  - 방어책(이미 반영): `LogIngestionBudgetAlarm`(일 ~120MB 초과 시 ops 이메일 조기경보) + Lambda 로그그룹 보관 14일 + **ERROR 위주 수집**
- 안전 조건(템플릿이 이미 충족): SSM=Standard, KMS=aws/ssm 관리형키, Lambda=VPC 밖, 알람≤10, 메트릭 dimension 추가 금지
- EC2 **상세 모니터링(1분)은 켜지 말 것**(유료). 기본 5분 사용.

## 로그 수집량(ingestion) 줄이기 — 비용의 유일한 변수
> 무료 5GB/월 ≈ **166MB/일**. 초기 앱이면 INFO를 다 보내도 그 밑일 가능성이 큼.
> 원칙: **먼저 배포 → `LogIngestionBudgetAlarm` 으로 관찰 → 울리면 줄이기.** 미리 과최적화 금지.

### 1순위: ERROR(또는 WARN+ERROR)만 CloudWatch로 (전체 로그는 인스턴스 디스크에 유지)
- 예외/키워드 알림이 목적이면 **ERROR-only가 최적** (자바 예외=ERROR로 찍힘). WARN 신호도 알림받고 싶을 때만 WARN 포함.
- 적용: `logback-spring.xml.example` → `src/main/resources/logback-spring.xml` 로 복사 (Java 코드 아님, 로깅 설정)
  - ERROR만: `ThresholdFilter` `<level>ERROR</level>` (예시 기본값)
  - WARN 포함: `<level>WARN</level>` 로 한 줄 변경
- `amazon-cloudwatch-agent.json` 의 `file_path` 를 `/var/log/webbb/error.log` 로 변경
- → CloudWatch엔 에러만 수집(급감). 로그 알람(`?ERROR ?Exception`)은 그대로 동작.
- ⚠️ **OutOfMemoryError** 는 JVM이 죽으며 로그를 못 남길 수 있음 → 메모리 메트릭/heartbeat로 별도 감지.

### 런타임별 주의
- **Agent(file tail · systemd/nohup)**: 위 방식 그대로 — 에러 파일만 tail. 가장 효과적.
- **Docker(awslogs)**: awslogs는 stdout 전체를 전송(레벨 필터 불가) → 둘 중 하나:
  - (a) prod 로그 레벨을 WARN/ERROR로 올려 stdout 자체를 줄이기 (application-prod.yml 또는 위 logback)
  - (b) awslogs 대신 "에러 파일 + Agent file tail" 로 전환

### 2순위: 노이즈 로거 낮추기 (INFO 유지하면서도 효과)
`application-prod.yml`(또는 위 logback)에서 최대 노이즈원부터:
```
logging.level.org.hibernate.SQL: WARN        # SQL 로깅 off (보통 가장 큰 노이즈)
logging.level.org.springframework.web: WARN
```
+ ALB 헬스체크 / actuator `/health` 핑 로그 제외.

### 측정 (배포 후)
CloudWatch → Metrics → `AWS/Logs > IncomingBytes`(LogGroupName=`/webbb/app`)로 일별 수집량 확인.
`LogIngestionBudgetAlarm` 이 일 ~120MB 초과 시 자동으로 ops 이메일 경고.

## 제거
콘솔 CloudFormation에서 `webbb-monitoring` 스택 삭제. 단, **SSM 봇 토큰 파라미터**와 (awslogs로 만든) 로그 그룹은 별도 정리.

## 조정 포인트
- 예외/키워드 패턴: 템플릿 `ErrorFilterPattern` (실측 로그로 콘솔 "패턴 테스트" 후 확정)
- 임계치: `CpuThreshold`, `ErrorThreshold`
- Lambda 코드 수정 시: `lambda/discord_notifier.py` 와 템플릿 인라인본을 **함께** 갱신
