package elicuci.czelada.mell_apoyo_emocional.service;

import elicuci.czelada.mell_apoyo_emocional.model.Message;
import elicuci.czelada.mell_apoyo_emocional.model.UserMemory;
import elicuci.czelada.mell_apoyo_emocional.model.enums.SenderType;
import elicuci.czelada.mell_apoyo_emocional.repository.MessageRepository;
import elicuci.czelada.mell_apoyo_emocional.repository.UserMemoryRepository;
import elicuci.czelada.mell_apoyo_emocional.service.port.LlmChatPort;
import elicuci.czelada.mell_apoyo_emocional.service.prompt.MellPrompts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memoria a largo plazo: un resumen por usuario + los mensajes aún no resumidos.
 * Cuando los mensajes sin resumir pasan de LIVE_MAX, los más viejos se absorben
 * al resumen (en segundo plano) y solo quedan los últimos KEEP.
 */
@Service
public class UserMemoryService {

    private static final Logger log = LoggerFactory.getLogger(UserMemoryService.class);

    static final int LIVE_MAX = 30;   // a partir de aquí se resume
    static final int KEEP = 14;       // mensajes recientes que quedan sin resumir
    static final int MAX_BATCH = 60;  // máximo de mensajes por resumen

    public record MemorySnapshot(String summary, Instant summarizedUntil) {
    }

    private final UserMemoryRepository memoryRepository;
    private final MessageRepository messageRepository;
    private final LlmChatPort llmChatPort;
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    public UserMemoryService(UserMemoryRepository memoryRepository,
                             MessageRepository messageRepository,
                             LlmChatPort llmChatPort) {
        this.memoryRepository = memoryRepository;
        this.messageRepository = messageRepository;
        this.llmChatPort = llmChatPort;
    }

    @Transactional(readOnly = true)
    public MemorySnapshot snapshot(UUID userId) {
        return memoryRepository.findById(userId)
                .map(m -> new MemorySnapshot(
                        m.getSummary(),
                        m.getSummarizedUntil() != null ? m.getSummarizedUntil() : Instant.EPOCH))
                .orElse(new MemorySnapshot("", Instant.EPOCH));
    }

    @Async
    @Transactional
    public void refreshIfNeeded(UUID userId) {
        if (!inFlight.add(userId)) return; // ya hay un resumen en curso para este usuario
        try {
            UserMemory memory = memoryRepository.findById(userId)
                    .orElseGet(() -> UserMemory.builder().userId(userId).summary("").build());

            Instant after = memory.getSummarizedUntil() != null ? memory.getSummarizedUntil() : Instant.EPOCH;
            List<Message> pending = messageRepository
                    .findByConversationUserIdAndCreatedAtAfterOrderByCreatedAtAsc(userId, after);

            if (pending.size() <= LIVE_MAX) return;

            int absorb = Math.min(pending.size() - KEEP, MAX_BATCH);
            List<Message> batch = pending.subList(0, absorb);

            StringBuilder transcript = new StringBuilder();
            for (Message m : batch) {
                if (m.getContentText() == null || m.getContentText().isBlank()) continue;
                transcript.append(m.getSender() == SenderType.USER ? "Persona: " : "MELL: ")
                        .append(m.getContentText()).append("\n");
            }

            String userPrompt = "RESUMEN ACTUAL:\n"
                    + (memory.getSummary().isBlank() ? "(todavía no hay nada)" : memory.getSummary())
                    + "\n\nCONVERSACIÓN NUEVA:\n" + transcript
                    + "\nDevuelve el resumen actualizado.";

            String updated = llmChatPort.generateEmpathicReply(MellPrompts.MEMORY_SUMMARY, List.of(), userPrompt);
            if (updated == null || updated.isBlank()) return;

            memory.setSummary(updated.trim());
            memory.setSummarizedUntil(batch.get(batch.size() - 1).getCreatedAt());
            memoryRepository.save(memory);
        } catch (Exception e) {
            log.warn("No se pudo actualizar la memoria del usuario {}", userId, e);
        } finally {
            inFlight.remove(userId);
        }
    }
}