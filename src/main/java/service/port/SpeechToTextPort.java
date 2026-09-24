package service.port;

//Puerto de transcripción de voz a texto.
//Implementaciones actuales previstas: OpenAI Whisper, Deepgram.
// asi el dominio no conoce cual de lso dos esta atras
public interface SpeechToTextPort {

    String transcribe(byte[] audioBytes, String mimeType);
    // audioBytes aduioo crudo capturado por la app
    // mineType tiempo del audio reicibido
}
