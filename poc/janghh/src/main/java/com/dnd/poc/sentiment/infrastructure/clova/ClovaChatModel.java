package com.dnd.poc.sentiment.infrastructure.clova;

import com.dnd.poc.global.common.exception.AppException;
import com.dnd.poc.global.common.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ClovaChatModel implements ChatModel {

    private final ClovaProperties properties;
    private final RestClient restClient;

    public ClovaChatModel(ClovaProperties properties) {
        this.properties = properties;
        this.restClient =
                RestClient.builder()
                        .baseUrl(properties.endpoint())
                        .defaultHeader("X-NCP-CLOVASTUDIO-API-KEY", properties.apiKey())
                        .defaultHeader("X-NCP-APIGW-API-KEY", properties.apigwKey())
                        .defaultHeader("Content-Type", "application/json")
                        .build();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        List<Map<String, String>> messages =
                prompt.getInstructions().stream()
                        .map(
                                msg ->
                                        Map.of(
                                                "role", toRole(msg.getMessageType()),
                                                "content", msg.getText()))
                        .toList();

        Map<String, Object> requestBody =
                Map.of(
                        "messages", messages,
                        "temperature", properties.temperature(),
                        "topP", properties.topP(),
                        "maxTokens", properties.maxTokens(),
                        "repeatPenalty", properties.repeatPenalty(),
                        "includeAiFilters", false);

        try {
            ClovaResponse response =
                    restClient.post().body(requestBody).retrieve().body(ClovaResponse.class);

            if (response == null
                    || response.result() == null
                    || response.result().message() == null) {
                throw new AppException(ErrorCode.CLOVA_API_ERROR);
            }

            String content = response.result().message().content();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));

        } catch (RestClientException e) {
            throw new AppException(ErrorCode.CLOVA_API_ERROR);
        }
    }

    private String toRole(MessageType type) {
        return switch (type) {
            case SYSTEM -> "system";
            case ASSISTANT -> "assistant";
            default -> "user";
        };
    }

    record ClovaResponse(Status status, Result result) {
        record Status(String code, String message) {}

        record Result(Message message, String stopReason) {}

        record Message(String role, String content) {}
    }
}
