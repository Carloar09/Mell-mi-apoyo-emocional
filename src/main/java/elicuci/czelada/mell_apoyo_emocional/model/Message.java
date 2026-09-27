package elicuci.czelada.mell_apoyo_emocional.model;

import jakarta.persistence.*;
import lombok.*;
import elicuci.czelada.mell_apoyo_emocional.model.enums.SenderType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderType sender;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "detected_sentiment")
    private String detectedSentiment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
