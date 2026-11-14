package elicuci.czelada.mell_apoyo_emocional.service.adapter.stt;

import elicuci.czelada.mell_apoyo_emocional.service.port.SpeechToTextPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * Implementación del puerto STT contra Deepgram (POST /v1/listen con el audio como cuerpo binario).
 * Se activa con mell.stt.provider=deepgram.
 */
@Component
@ConditionalOnProperty(name = "mell.stt.provider", havingValue = "deepgram")
public class DeepgramSttAdapter implements SpeechToTextPort {

    private final WebClient webClient;
    private final String model;
    private final String language;

    public DeepgramSttAdapter(@Value("${mell.deepgram.api-key}") String apiKey,
                              @Value("${mell.deepgram.model:nova-3}") String model,
                              @Value("${mell.deepgram.language:es}") String language) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.deepgram.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Token " + apiKey)
                .build();
        this.model = model;
        this.language = language;
    }

    @Override
    public String transcribe(byte[] audioBytes, String mimeType) {
        // "audio/webm;codecs=opus" -> "audio/webm"
        String baseType = (mimeType == null || mimeType.isBlank())
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : mimeType.split(";")[0].trim();

        try {
            DeepgramResponse response = webClient.post()
                    .uri(uri -> uri.path("/listen")
                            .queryParam("model", model)
                            .queryParam("language", language)
                            .queryParam("smart_format", "true")
                            .build())
                    .contentType(MediaType.parseMediaType(baseType))
                    .bodyValue(audioBytes)
                    .retrieve()
                    .bodyToMono(DeepgramResponse.class)
                    .block();

            if (response == null || response.results() == null
                    || response.results().channels() == null
                    || response.results().channels().isEmpty()
                    || response.results().channels().get(0).alternatives() == null
                    || response.results().channels().get(0).alternatives().isEmpty()) {
                return "";
            }
            String transcript = response.results().channels().get(0).alternatives().get(0).transcript();
            return transcript != null ? transcript : "";
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Deepgram " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        }
    }

    private record DeepgramResponse(Results results) {
    }

    private record Results(List<Channel> channels) {
    }

    private record Channel(List<Alternative> alternatives) {
    }

    private record Alternative(String transcript) {
    }
}