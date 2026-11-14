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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log =
            LoggerFactory.getLogger(ElevenLabsTtsAdapter.class);

    public ElevenLabsTtsAdapter(@Qualifier(ExternalApiClientsConfig.ELEVENLABS_CLIENT) WebClient elevenLabsWebClient,
                                ElevenLabsProperties properties) {
        this.elevenLabsWebClient = elevenLabsWebClient;
        this.properties = properties;
    }
    @Override
    public byte[] synthesize(String text, String externalVoiceId) {

        String voiceId = properties.defaultVoiceId();

        log.info("Probando voz predeterminada: {}", voiceId);

        Map<String, Object> body = Map.of(
                "text", text,
                "model_id", "eleven_turbo_v2_5",
                "language_code", "es",
                "voice_settings", Map.of(
                        "stability", 0.40,          // más bajo = más expresiva, más emoción
                        "similarity_boost", 0.80,
                        "style", 0.35,
                        "use_speaker_boost", true
                )
        );

        try {
            return elevenLabsWebClient.post()
                    .uri("/text-to-speech/{voiceId}?output_format=mp3_44100_64", voiceId)
                    .accept(MediaType.parseMediaType("audio/mpeg"))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

        } catch (WebClientResponseException e) {
            log.error(
                    "Error de ElevenLabs. HTTP: {}, respuesta: {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
            throw e;
        }
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
