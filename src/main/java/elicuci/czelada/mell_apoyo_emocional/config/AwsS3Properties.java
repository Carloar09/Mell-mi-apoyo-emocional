package elicuci.czelada.mell_apoyo_emocional.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mell.aws")
public record AwsS3Properties(
        String region,
        String s3Bucket,
        String accessKey,
        String secretKey

) {
}
