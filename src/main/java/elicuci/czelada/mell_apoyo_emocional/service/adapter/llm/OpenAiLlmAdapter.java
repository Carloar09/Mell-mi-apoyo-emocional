
package elicuci.czelada.mell_apoyo_emocional.service.adapter.llm;

import elicuci.czelada.mell_apoyo_emocional.config.ExternalApiClientsConfig;
import elicuci.czelada.mell_apoyo_emocional.config.OpenAiProperties;
import elicuci.czelada.mell_apoyo_emocional.service.port.LlmChatPort;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiLlmAdapter implements LlmChatPort {

    private final WebClient openAiWebClient;
    private final OpenAiProperties properties;

    public OpenAiLlmAdapter(
            @Qualifier(ExternalApiClientsConfig.OPENAI_CLIENT)
            WebClient openAiWebClient,
            OpenAiProperties properties
    ) {
        this.openAiWebClient = openAiWebClient;
        this.properties = properties;
    }

    @Override
    public String generateEmpathicReply(
            String systemPrompt,
            List<LlmChatPort.ChatTurn> conversationLog,
            String userMessage
    ) {
        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of(
                "role", "system",
                "content", systemPrompt
        ));

        for (LlmChatPort.ChatTurn turn : conversationLog) {
            messages.add(Map.of(
                    "role", turn.role(),
                    "content", turn.content()
            ));
        }

        messages.add(Map.of(
                "role", "user",
                "content", userMessage
        ));

        Map<String, Object> requestBody = Map.of(
                "model", properties.chatModel(),
                "messages", messages,
                "temperature", 0.8,
                "max_completion_tokens", 160,   // tope de longitud: menos espera y menos monólogo
                "frequency_penalty", 0.3        // evita que repita siempre las mismas muletillas
        );

        try {
            ChatCompletionResponse response = openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .block();

            if (response == null
                    || response.choices() == null
                    || response.choices().isEmpty()
                    || response.choices().get(0).message() == null) {
                throw new IllegalStateException(
                        "OpenAI devolvió una respuesta sin opciones válidas."
                );
            }

            String content = response.choices().get(0).message().content();

            return content != null ? content : "";

        } catch (WebClientResponseException e) {
            System.err.println(
                    "Error HTTP de OpenAI: " + e.getStatusCode()
            );

            System.err.println(
                    "Detalle de OpenAI: " + e.getResponseBodyAsString()
            );

            throw e;
        }
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(ChatMessage message) {
    }

    private record ChatMessage(String role, String content) {
    }
}

