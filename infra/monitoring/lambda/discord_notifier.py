"""
SNS(CloudWatch Alarm) → Discord 변환기 Lambda.

- 트리거: SNS 토픽 webbb-alerts (CPU/로그 알람)
- 또는: EventBridge 스케줄(SNS Records 없음) → Heartbeat(데드맨 스위치) 메시지
- 의존성: 없음 (urllib + boto3는 Lambda 기본 제공)

환경변수:
  CHANNEL_ID    : Discord 채널 ID (예: 1512246623642718308)
  TOKEN_PARAM   : 봇 토큰이 저장된 SSM 파라미터 이름 (SecureString)

주의: 봇 토큰/Authorization 헤더는 절대 로깅하지 않는다.

이 파일은 CloudFormation 템플릿(cloudwatch-discord-alerts.yaml)의
인라인 Lambda 코드와 동일하게 유지한다(source of truth).
"""

import json
import os
import time
import urllib.error
import urllib.request

import boto3

_ssm = boto3.client("ssm")
_token_cache = {}


def _get_token():
    if "v" not in _token_cache:
        name = os.environ["TOKEN_PARAM"]
        resp = _ssm.get_parameter(Name=name, WithDecryption=True)
        _token_cache["v"] = resp["Parameter"]["Value"]
    return _token_cache["v"]


def _post(channel_id, payload):
    url = "https://discord.com/api/v10/channels/%s/messages" % channel_id
    body = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=body,
        method="POST",
        headers={
            "Authorization": "Bot " + _get_token(),
            "Content-Type": "application/json",
            "User-Agent": "webbb-monitor/1.0",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=8) as r:
            return r.status
    except urllib.error.HTTPError as e:
        # 429 Too Many Requests → Retry-After 만큼 1회 대기 후 재시도
        if e.code == 429:
            retry_after = float(e.headers.get("Retry-After", "1") or "1")
            time.sleep(min(retry_after, 5))
            with urllib.request.urlopen(req, timeout=8) as r:
                return r.status
        raise  # 그 외 실패는 raise → SNS 재시도 → DLQ


def _embed(alarm):
    state = alarm.get("NewStateValue", "")
    color = 15158332 if state == "ALARM" else (5763719 if state == "OK" else 15844367)
    name = alarm.get("AlarmName", "(unknown)")
    reason = (alarm.get("NewStateReason") or "")[:1500]
    region = alarm.get("Region") or os.environ.get("AWS_REGION", "")
    when = alarm.get("StateChangeTime", "")
    title = ("🔴 " if state == "ALARM" else "🟢 " if state == "OK" else "🟡 ") + name
    return {
        "embeds": [
            {
                "title": title[:250],
                "description": reason,
                "color": color,
                "fields": [
                    {"name": "상태", "value": state or "-", "inline": True},
                    {"name": "리전", "value": region or "-", "inline": True},
                    {"name": "시각", "value": when or "-", "inline": False},
                ],
            }
        ]
    }


def handler(event, context):
    channel_id = os.environ["CHANNEL_ID"]

    # Heartbeat: SNS Records가 없으면 EventBridge 스케줄 호출로 간주
    if not event.get("Records"):
        _post(channel_id, {"content": "✅ WEBBB 모니터링 정상 동작 중 (heartbeat)"})
        return {"ok": True, "mode": "heartbeat"}

    for rec in event["Records"]:
        message = rec.get("Sns", {}).get("Message", "")
        try:
            alarm = json.loads(message)
        except (ValueError, TypeError):
            _post(channel_id, {"content": "⚠️ " + str(message)[:1800]})
            continue
        _post(channel_id, _embed(alarm))

    return {"ok": True, "mode": "alarm", "count": len(event["Records"])}
