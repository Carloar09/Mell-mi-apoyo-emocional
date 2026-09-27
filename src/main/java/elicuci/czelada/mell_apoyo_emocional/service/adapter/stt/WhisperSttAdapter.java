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

// puerto stt contra opeai, vendira para el endpoint :post / audio y transcipcion
//esto s ehace por que el equipo decide migrar  a deepgram solo se crea un aadaptador
//implementando el mismo puerto
@Component
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
        builder.part("elicuci/czelada/mell_apoyo_emocional/model", properties.whisperModel());

        WhisperResponse response = openAiWebClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(org.springframework.web.reactive.function.BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(WhisperResponse.class)
                .block();

        return response != null ? response.text() : "";
    }

    private String extensionFor(String mimeType) {
        if (mimeType == null) return "wav";
        if (mimeType.contains("mp4") || mimeType.contains("m4a")) return "m4a";
        if (mimeType.contains("webm")) return "webm";
        if (mimeType.contains("mpeg")) return "mp3";
        return "wav";
    }

    private record WhisperResponse(String text) {
    }

}
