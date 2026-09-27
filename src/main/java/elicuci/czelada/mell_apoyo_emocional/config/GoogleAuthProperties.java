package elicuci.czelada.mell_apoyo_emocional.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mell.google")
public record GoogleAuthProperties(
        String clientId
) {
}
