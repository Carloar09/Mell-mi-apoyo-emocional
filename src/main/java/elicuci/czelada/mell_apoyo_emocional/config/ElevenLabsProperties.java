package elicuci.czelada.mell_apoyo_emocional.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mell.elevenlabs")
public record ElevenLabsProperties (
        String apiKey,
        String baseUrl,
        String defaultVoiceId
)
{
}
