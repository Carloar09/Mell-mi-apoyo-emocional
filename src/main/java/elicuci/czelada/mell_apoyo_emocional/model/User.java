package elicuci.czelada.mell_apoyo_emocional.model;

import jakarta.persistence.*;
import lombok.*;
import elicuci.czelada.mell_apoyo_emocional.model.enums.AuthProvider;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    // Nullable: un usuario que entra solo por Google no tiene password propio
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider;

    // "sub" que devuelve Google al verificar el token. Null si es LOCAL.
    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

}
