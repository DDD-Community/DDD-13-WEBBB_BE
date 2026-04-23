package com.dnd.webbb.ai.application;

import com.dnd.webbb.ai.application.dto.AnalysisResponse;
import com.dnd.webbb.ai.application.dto.EncouragementResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiEncouragementService {

    private static final Logger log = LoggerFactory.getLogger(AiEncouragementService.class);

    private static final List<String> CRISIS_KEYWORDS =
            List.of("죽고 싶", "자해", "자살", "극단적 선택", "삶을 끝내");

    private static final String CRISIS_MESSAGE =
            "당신의 이야기를 들었습니다. 지금 많이 힘드시죠. "
                    + "전문 상담사와 이야기를 나눠보시는 건 어떨까요?\n\n"
                    + "• 자살예방상담전화: 1393 (24시간)\n"
                    + "• 정신건강위기상담전화: 109 (24시간)";

    private static final String SYSTEM_PROMPT =
            """
            너는 취준생 고민 상담 전문가야. 아래 규칙을 반드시 지켜.

            ## 역할
            - 사용자의 고민글을 읽고 감정을 분석한 뒤, 공감과 응원이 담긴 응답을 생성해.

            ## 가드레일
            - 공감을 최우선으로 해. 상대의 감정을 먼저 인정해줘.
            - 다른 사람과 비교하지 마.
            - "넌 ~해야 해", "~하면 안 돼" 같은 단정적 표현을 쓰지 마.
            - 훈계하거나 가르치려 들지 마.
            - 구체적이고 실천 가능한 작은 행동 하나를 제안해.

            ## 출력 형식
            반드시 아래 JSON 구조로만 응답해:
            - emotionTags: 감정 태그 리스트 (예: 불안, 번아웃, 자존감저하, 외로움, 막막함 등)
            - summary: 고민을 한 문장으로 요약
            - encouragementMessage: 공감과 응원이 담긴 메시지
            - smallActionTip: 오늘 당장 할 수 있는 작은 행동 1개
            """;

    private final ChatClient chatClient;

    public AiEncouragementService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public AnalysisResponse analyze(String text, String tone) {
        if (containsCrisisSignal(text)) {
            return new AnalysisResponse(true, List.of(), "", CRISIS_MESSAGE, "");
        }

        try {
            String userPrompt =
                    String.format(
                            """
                    말투: %s
                    고민글: %s
                    """,
                            tone, text);

            EncouragementResult result =
                    chatClient
                            .prompt()
                            .system(SYSTEM_PROMPT)
                            .user(userPrompt)
                            .call()
                            .entity(EncouragementResult.class);

            return new AnalysisResponse(
                    false,
                    result.emotionTags(),
                    result.summary(),
                    result.encouragementMessage(),
                    result.smallActionTip());
        } catch (Exception e) {
            log.error("AI 호출 실패", e);
            return new AnalysisResponse(
                    false, List.of(), "", "지금은 응답을 생성하기 어렵습니다. 잠시 후 다시 시도해주세요.", "");
        }
    }

    private boolean containsCrisisSignal(String text) {
        return CRISIS_KEYWORDS.stream().anyMatch(text::contains);
    }
}
