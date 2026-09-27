package elicuci.czelada.mell_apoyo_emocional.repository;

import elicuci.czelada.mell_apoyo_emocional.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findByUserIdOrderByStartTimeDesc(UUID userId);
}
