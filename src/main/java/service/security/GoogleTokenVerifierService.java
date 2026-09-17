package service.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import config.GoogleAuthProperties;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleTokenVerifierService {
    // veerifica si  un ID Token de Google sea auténtico (firmado por Google,
    // que no sea  expirado,

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierService(GoogleAuthProperties properties) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(properties.clientId()))
                .build();
    }

    public GooglePayload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new IllegalArgumentException("Token de Google inválido o expirado");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            return new GooglePayload(
                    payload.getSubject(),
                    payload.getEmail(),
                    (String) payload.get("given_name")
            );
        } catch (GeneralSecurityException | java.io.IOException e) {
            throw new IllegalArgumentException("No se pudo verificar el token de Google", e);
        }
    }

    public record GooglePayload(String sub, String email, String firstName) {
    }
}
