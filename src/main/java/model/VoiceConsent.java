package model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

//Registro eitoc para la voz clonada, tieien que pasar por todo esto antes de crear un voiceprofile
@Entity
@Table(name = "voice_consents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceConsent {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven;

    @Column(name = "consent_audio_url")
    private String consentAudioUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
