package dto.auth;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        // token que la app recibe del SDK
        @NotBlank
        String idToken
) {
}
