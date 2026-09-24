package service.port;
// puerto de la sintesis de voz ElevenLabs API, XTTS v2 (self-hosted)
public interface TextToSpeechPort {

    //text texto a sintetizar
    //externarvoiceid id de la voz en el provedor
    byte[] synthesize(String text, String externalVoiceId);

    // clona una voz a partir de una mieustra de 30 segundos y bajo consenitmiento
    //verificado devulve la voice_id
    String cloneVoice(byte[] sampleAudioBytes, String mimeType, String profileName);
}
