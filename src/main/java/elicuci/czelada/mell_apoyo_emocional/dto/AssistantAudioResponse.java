package elicuci.czelada.mell_apoyo_emocional.dto;

import java.util.UUID;

public record AssistantAudioResponse(
        UUID conversationId,
        UUID messageId,
        String replyText,
        String audioBase64,     // audio sintetizado codificado en base64, listo para reproducir en la app
        boolean crisisEscalated,
        String hotlineMessage   // solo se llena si crisisEscalated = true

) {
}
