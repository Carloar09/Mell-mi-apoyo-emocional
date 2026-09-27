package elicuci.czelada.mell_apoyo_emocional.service.adapter.tts;
import elicuci.czelada.mell_apoyo_emocional.config.ElevenLabsProperties;
import elicuci.czelada.mell_apoyo_emocional.config.ExternalApiClientsConfig;
import elicuci.czelada.mell_apoyo_emocional.service.port.TextToSpeechPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

// Implementacion del puerto TTs contra Elevenlabs
// endpoinst  POST /text-to-speech/{voice_id}
// POST /voices/add  (Instant Voice Cloning, muestra ~30s)
//  Para migrar a XTTS v2 (self-hosted): crear XttsTtsAdapter implementando
// el mismo puerto. El orquestador no cambia
@Component
public class ElevenLabsTtsAdapter implements TextToSpeechPort {
    private final WebClient elevenLabsWebClient;
    private final ElevenLabsProperties properties;

    public ElevenLabsTtsAdapter(@Qualifier(ExternalApiClientsConfig.ELEVENLABS_CLIENT) WebClient elevenLabsWebClient,
                                ElevenLabsProperties properties) {
        this.elevenLabsWebClient = elevenLabsWebClient;
        this.properties = properties;
    }
    @Override
    public byte[] synthesize(String text, String externalVoiceId) {
        String voiceId = (externalVoiceId != null && !externalVoiceId.isBlank())
                ? externalVoiceId
                : properties.defaultVoiceId();

        Map<String, Object> body = Map.of(
                "text", text,
                "model_id", "eleven_multilingual_v2"
        );

        return elevenLabsWebClient.post()
                .uri("/text-to-speech/{voiceId}", voiceId)
                .accept(MediaType.parseMediaType("audio/mpeg"))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }

    @Override
    public String cloneVoice(byte[] sampleAudioBytes, String mimeType, String profileName) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("name", profileName);
        builder.part("files", new ByteArrayResource(sampleAudioBytes) {
            @Override
            public String getFilename() {
                return "sample.wav";
            }
        });

        CloneVoiceResponse response = elevenLabsWebClient.post()
                .uri("/voices/add")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(CloneVoiceResponse.class)
                .block();

        return response != null ? response.voice_id() : null;
    }

    private record CloneVoiceResponse(String voice_id) {
    }


}
