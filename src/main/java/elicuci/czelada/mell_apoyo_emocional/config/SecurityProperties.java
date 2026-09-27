package elicuci.czelada.mell_apoyo_emocional.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "mell.security")
public record SecurityProperties(
        String jwtSecret,
        long jwtExpirationMinutes
) {
}
