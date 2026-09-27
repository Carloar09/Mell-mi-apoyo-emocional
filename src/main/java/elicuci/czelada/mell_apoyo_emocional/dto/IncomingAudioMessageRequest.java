package elicuci.czelada.mell_apoyo_emocional.dto;

import java.util.UUID;

//Metadatos que acompñan al archivo de audio (multipart) enviado por la app.
//El binario de audio viaja como MultipartFile aparte en el controller.
public record IncomingAudioMessageRequest(
        UUID userId,
        UUID conversationId,   // null => se crea una conversación nueva
        UUID voiceProfileId,   // null => se usa la voz default del sistema
        String mimeType

) {
}
