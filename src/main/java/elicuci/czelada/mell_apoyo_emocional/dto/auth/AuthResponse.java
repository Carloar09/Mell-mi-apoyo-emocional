package elicuci.czelada.mell_apoyo_emocional.dto.auth;

import java.util.UUID;

public record AuthResponse(

        String token,
        UUID userId,
        String email,
        String firstName
) {
}
