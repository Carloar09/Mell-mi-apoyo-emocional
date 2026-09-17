package model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voice_profile_id")
    private VoiceProfile voiceProfile;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @PrePersist
    void onCreate() {
        if (startTime == null) {
            startTime = Instant.now();
        }
    }
}
