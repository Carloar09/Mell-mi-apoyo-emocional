package elicuci.czelada.mell_apoyo_emocional.service.adapter.llm;

import elicuci.czelada.mell_apoyo_emocional.config.ExternalApiClientsConfig;
import elicuci.czelada.mell_apoyo_emocional.config.OpenAiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import elicuci.czelada.mell_apoyo_emocional.service.port.LlmChatPort;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// implementacion del puerto de chat empático contra openai
// sirve  para mirghar a geminir:: crear geminillmadamter implementando llmchatport
// y se cambia el bena  inyectado endpoint: POST /chat/completions).
@Component
public class OpenAiLlmAdapter implements LlmChatPort {

    private final WebClient openAiWebClient;
    private final OpenAiProperties properties;

    public OpenAiLlmAdapter(@Qualifier(ExternalApiClientsConfig.OPENAI_CLIENT) WebClient openAiWebClient,
                            OpenAiProperties properties) {
        this.openAiWebClient = openAiWebClient;
        this.properties = properties;
    }
    @Override
    public String generateEmpathicReply(String systemPrompt, List<LlmChatPort.ChatTurn> conversationLog, String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (LlmChatPort.ChatTurn turn : conversationLog) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> requestBody = Map.of(
                "elicuci/czelada/mell_apoyo_emocional/model", properties.chatModel(),
                "messages", messages,
                "temperature", 0.7
        );

        ChatCompletionResponse response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return "";
        }
        return response.choices().get(0).message().content();
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(ChatMessage message) {
    }

    private record ChatMessage(String role, String content) {
    }
}
