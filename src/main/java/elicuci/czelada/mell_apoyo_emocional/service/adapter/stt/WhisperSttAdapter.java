package elicuci.czelada.mell_apoyo_emocional.service.adapter.stt;

import elicuci.czelada.mell_apoyo_emocional.config.ExternalApiClientsConfig;
import elicuci.czelada.mell_apoyo_emocional.config.OpenAiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import elicuci.czelada.mell_apoyo_emocional.service.port.SpeechToTextPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// puerto stt contra opeai, vendira para el endpoint :post / audio y transcipcion
//esto s ehace por que el equipo decide migrar  a deepgram solo se crea un aadaptador
//implementando el mismo puerto
@Component
@ConditionalOnProperty(name = "mell.stt.provider", havingValue = "whisper", matchIfMissing = true)
public class WhisperSttAdapter implements SpeechToTextPort {
    private final WebClient openAiWebClient;
    private final OpenAiProperties properties;
    //CONSTRUCUTO USA una inyeeccion de dependencia para proporcionar
    // que el webcliente este configurado para OPEai y las configuraciones de la OPENAI
    public WhisperSttAdapter(@Qualifier(ExternalApiClientsConfig.OPENAI_CLIENT) WebClient openAiWebClient,
                             OpenAiProperties properties) {
        this.openAiWebClient = openAiWebClient;
        this.properties = properties;
    }
    // este mettodo es para que convierta un archivo de audio en texto
    @Override
    public String transcribe(byte[] audioBytes, String mimeType) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(audioBytes) {
                    @Override
                    public String getFilename() {
                        return "audio." + extensionFor(mimeType);
                    }
                })
                .header("Content-Disposition", "form-data; name=file; filename=audio." + extensionFor(mimeType));
        builder.part("model", properties.whisperModel());

        try {
            WhisperResponse response = openAiWebClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(org.springframework.web.reactive.function.BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(WhisperResponse.class)
                    .block();

            return response != null ? response.text() : "";
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            throw new RuntimeException(
                    "Error llamando a Whisper (status " + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        }
    }
    private String extensionFor(String mimeType) {
        if (mimeType == null) return "wav";
        if (mimeType.contains("mp4") || mimeType.contains("m4a") || mimeType.contains("x-m4a")) return "m4a";
        if (mimeType.contains("aac")) return "aac";
        if (mimeType.contains("webm")) return "webm";
        if (mimeType.contains("mpeg") || mimeType.contains("mp3")) return "mp3";
        if (mimeType.contains("wav")) return "wav";
        if (mimeType.contains("ogg")) return "ogg";
        return "wav";
    }

    private record WhisperResponse(String text) {
    }

}
