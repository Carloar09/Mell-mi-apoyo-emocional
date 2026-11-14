package elicuci.czelada.mell_apoyo_emocional.repository;

import elicuci.czelada.mell_apoyo_emocional.model.Message;
import elicuci.czelada.mell_apoyo_emocional.model.enums.SenderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    /** Mensajes del usuario (todas sus conversaciones) posteriores a una fecha, en orden cronológico. */
    List<Message> findByConversationUserIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID userId, Instant after);

    /** ¿El usuario ya habló alguna vez? Sirve para elegir el saludo. */
    boolean existsByConversationUserIdAndSender(UUID userId, SenderType sender);
}