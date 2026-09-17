package config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

// aqui se conectan las APIS externas cada bean es un webcliente
//pre-configurado con base-url + headers de autenticación, listo para ser
//inyectado en el adaptador correspondiente (STT, LLM, TTS).
//Ningún otro componente del dominio debería construir un WebClient a mano:
//todos deben pedir estos beans por @Qualifier.
@Configuration
public class ExternalApiClientsConfig {
    public static final String OPENAI_CLIENT = "openAiWebClient";
    public static final String ELEVENLABS_CLIENT = "elevenLabsWebClient";

    @Bean(OPENAI_CLIENT)
    public WebClient openAiWebClient(OpenAiProperties props) {
        return WebClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
                .build();
    }

    @Bean(ELEVENLABS_CLIENT)
    public WebClient elevenLabsWebClient(ElevenLabsProperties props) {
        return WebClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("xi-api-key", props.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
