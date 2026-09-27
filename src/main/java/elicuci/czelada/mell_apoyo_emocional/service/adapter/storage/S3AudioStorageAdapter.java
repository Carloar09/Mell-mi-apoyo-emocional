package elicuci.czelada.mell_apoyo_emocional.service.adapter.storage;

import elicuci.czelada.mell_apoyo_emocional.config.AwsS3Properties;
import elicuci.czelada.mell_apoyo_emocional.service.port.AudioStoragePort;
import elicuci.czelada.mell_apoyo_emocional.service.port.LlmChatPort;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

@Component
public class S3AudioStorageAdapter implements AudioStoragePort {

    private final S3Client s3Client;
    private final AwsS3Properties properties;

    public S3AudioStorageAdapter(S3Client s3Client, AwsS3Properties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }
    @Override
    public String upload(byte[] audioBytes, String mimeType, String keyPrefix) {
        String key = keyPrefix + "/" + UUID.randomUUID() + ".audio";

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.s3Bucket())
                .key(key)
                .contentType(mimeType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(audioBytes));

        return "s3://" + properties.s3Bucket() + "/" + key;
    }

    @Override
    public byte[] download(String storagePathOrUrl) {
        String key = storagePathOrUrl.replaceFirst("^s3://" + properties.s3Bucket() + "/", "");

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.s3Bucket())
                .key(key)
                .build();

        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
            return response.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo descargar el audio desde S3: " + key, e);
        }
    }

}
