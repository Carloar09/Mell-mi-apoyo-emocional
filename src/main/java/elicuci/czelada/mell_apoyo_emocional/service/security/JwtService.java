package elicuci.czelada.mell_apoyo_emocional.service.security;

import elicuci.czelada.mell_apoyo_emocional.config.SecurityProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import io.jsonwebtoken.Claims;

@Service
public class JwtService {

    //  Va a generar y validar los JWT propios de MELL estos son los tokens que la app
    // que va  a usar para llamar al resto de la API
    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(SecurityProperties properties) {
        if (properties.jwtSecret() == null || properties.jwtSecret().length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET debe estar configurado y tener al menos 32 caracteres");
        }
        this.signingKey = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = properties.jwtExpirationMinutes();
    }

    public String generateToken(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    public UUID extractUserId(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
