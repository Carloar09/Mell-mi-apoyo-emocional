package elicuci.czelada.mell_apoyo_emocional.service.port;
// puerto e almacnamiento de archivos de audio
// amazon s3  o strorage compatovle
public interface AudioStoragePort {
    // sube un audiop y devulve la url donde quedo almacenado
    String upload(byte[] audioBytes, String mimeType, String keyPrefix);

    byte[] download(String storagePathOrUrl);
}
