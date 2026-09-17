package config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mell.openai")
public record OpenAiProperties(
        String apiKey,
        String baseUrl,
        String chatModel,
        String whisperModel
){
}
