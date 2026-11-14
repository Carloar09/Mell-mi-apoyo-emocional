package elicuci.czelada.mell_apoyo_emocional.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_memories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMemory {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(name = "summarized_until")
    private Instant summarizedUntil;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}