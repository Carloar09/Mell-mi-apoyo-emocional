package model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name="voice_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceProfile {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Nullable: un perfil de voz "default" del sistema no requiere consentimiento
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consent_id")
    private VoiceConsent consent;

    @Column(name = "profile_name", nullable = false)
    private String profileName;

    private String gender;

    @Column(name = "sample_audio_url")
    private String sampleAudioUrl;

    // ID devuelto por ElevenLabs (o el motor TTS que corresponda) tras el cloning
    @Column(name = "external_voice_id")
    private String externalVoiceId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
