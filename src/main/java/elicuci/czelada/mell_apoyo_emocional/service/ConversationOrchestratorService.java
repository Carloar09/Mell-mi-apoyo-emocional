package elicuci.czelada.mell_apoyo_emocional.service;

import elicuci.czelada.mell_apoyo_emocional.dto.AssistantAudioResponse;
import elicuci.czelada.mell_apoyo_emocional.dto.IncomingAudioMessageRequest;
import elicuci.czelada.mell_apoyo_emocional.model.Conversation;
import elicuci.czelada.mell_apoyo_emocional.model.Message;
import elicuci.czelada.mell_apoyo_emocional.model.User;
import elicuci.czelada.mell_apoyo_emocional.model.VoiceProfile;
import elicuci.czelada.mell_apoyo_emocional.model.enums.SenderType;
import elicuci.czelada.mell_apoyo_emocional.repository.ConversationRepository;
import elicuci.czelada.mell_apoyo_emocional.repository.MessageRepository;
import elicuci.czelada.mell_apoyo_emocional.repository.UserRepository;
import elicuci.czelada.mell_apoyo_emocional.repository.VoiceProfileRepository;
import elicuci.czelada.mell_apoyo_emocional.service.guardrails.GuardrailsService;
import elicuci.czelada.mell_apoyo_emocional.service.port.AudioStoragePort;
import elicuci.czelada.mell_apoyo_emocional.service.port.LlmChatPort;
import elicuci.czelada.mell_apoyo_emocional.service.port.SpeechToTextPort;
import elicuci.czelada.mell_apoyo_emocional.service.port.TextToSpeechPort;
import elicuci.czelada.mell_apoyo_emocional.service.prompt.MellPrompts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(ConversationOrchestratorService.class);

    private static final int MAX_LIVE_CONTEXT = 40; // tope de seguridad si el resumen se atrasa

    private static final String FIRST_GREETING =
            "Hola, soy Mell, tu asistente de apoyo emocional. Cuéntame sobre ti.";

    private final SpeechToTextPort speechToTextPort;
    private final LlmChatPort llmChatPort;
    private final TextToSpeechPort textToSpeechPort;
    private final AudioStoragePort audioStoragePort;
    private final GuardrailsService guardrailsService;
    private final UserMemoryService userMemoryService;

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final VoiceProfileRepository voiceProfileRepository;
    private final MessageRepository messageRepository;

    /** El saludo de primera vez es siempre el mismo texto: se sintetiza una vez por voz. */
    private final Map<String, byte[]> greetingCache = new ConcurrentHashMap<>();

    @Value("${mell.guardrails.crisis-hotline-message}")
    private String crisisHotlineMessage;

    public ConversationOrchestratorService(SpeechToTextPort speechToTextPort,
                                           LlmChatPort llmChatPort,
                                           TextToSpeechPort textToSpeechPort,
                                           AudioStoragePort audioStoragePort,
                                           GuardrailsService guardrailsService,
                                           UserMemoryService userMemoryService,
                                           UserRepository userRepository,
                                           ConversationRepository conversationRepository,
                                           VoiceProfileRepository voiceProfileRepository,
                                           MessageRepository messageRepository) {
        this.speechToTextPort = speechToTextPort;
        this.llmChatPort = llmChatPort;
        this.textToSpeechPort = textToSpeechPort;
        this.audioStoragePort = audioStoragePort;
        this.guardrailsService = guardrailsService;
        this.userMemoryService = userMemoryService;
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.voiceProfileRepository = voiceProfileRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public AssistantAudioResponse handleIncomingAudio(IncomingAudioMessageRequest request, byte[] audioBytes) {
        long t0 = System.currentTimeMillis();

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Conversation conversation = resolveConversation(request, user);

        // La subida a S3 corre en paralelo con Whisper; si falla, la conversación sigue
        CompletableFuture<String> userAudioUpload = CompletableFuture
                .supplyAsync(() -> audioStoragePort.upload(audioBytes, request.mimeType(), "users/" + user.getId()))
                .exceptionally(e -> {
                    log.warn("No se pudo subir el audio del usuario a S3", e);
                    return null;
                });

        // 1) STT
        String userText = speechToTextPort.transcribe(audioBytes, request.mimeType());
        long t1 = System.currentTimeMillis();

        // Memoria: se lee ANTES de guardar el mensaje actual (el adaptador del LLM lo añade al final)
        UserMemoryService.MemorySnapshot memory = userMemoryService.snapshot(user.getId());
        List<LlmChatPort.ChatTurn> recent = loadRecent(user.getId(), memory.summarizedUntil());

        persistMessage(conversation, SenderType.USER, userText, userAudioUpload.join(), null);

        // 2) Guardrails
        GuardrailsService.GuardrailsResult guardrailsResult = guardrailsService.evaluate(userText);

        if (guardrailsResult.isCrisis()) {
            Message assistantMsg = persistMessage(conversation, SenderType.ASSISTANT,
                    crisisHotlineMessage, null, "CRISIS");

            return new AssistantAudioResponse(
                    conversation.getId(),
                    assistantMsg.getId(),
                    crisisHotlineMessage,
                    null,
                    true,
                    crisisHotlineMessage
            );
        }

        // 3) LLM con resumen de memoria + mensajes recientes
        String replyText = llmChatPort.generateEmpathicReply(
                buildSystemPrompt(user, memory.summary()), recent, guardrailsResult.sanitizedText());
        long t2 = System.currentTimeMillis();

        // 4) TTS (el audio de la respuesta ya no se sube a S3: solo agregaba espera)
        String externalVoiceId = resolveExternalVoiceId(conversation);
        byte[] replyAudioBytes = textToSpeechPort.synthesize(replyText, externalVoiceId);
        long t3 = System.currentTimeMillis();

        Message assistantMessage = persistMessage(conversation, SenderType.ASSISTANT, replyText, null, null);

        log.info("Latencia (ms) -> stt={} llm={} tts={} total={}", t1 - t0, t2 - t1, t3 - t2, t3 - t0);

        // Resumen de memoria en segundo plano (no retrasa la respuesta)
        userMemoryService.refreshIfNeeded(user.getId());

        return new AssistantAudioResponse(
                conversation.getId(),
                assistantMessage.getId(),
                replyText,
                Base64.getEncoder().encodeToString(replyAudioBytes),
                false,
                null
        );
    }

    /**
     * Saludo de voz al abrir una sesión. Crea la conversación y guarda el saludo como
     * mensaje del asistente, para que el LLM sepa qué se dijo antes de la primera respuesta.
     */
    @Transactional
    public AssistantAudioResponse greet(UUID userId, UUID voiceProfileId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        boolean returning = messageRepository.existsByConversationUserIdAndSender(userId, SenderType.USER);
        String text = returning ? returningGreeting(user) : FIRST_GREETING;

        VoiceProfile voiceProfile = voiceProfileId != null
                ? voiceProfileRepository.findById(voiceProfileId).orElse(null)
                : null;

        Conversation conversation = conversationRepository.save(
                Conversation.builder().user(user).voiceProfile(voiceProfile).build());

        String externalVoiceId = voiceProfile != null ? voiceProfile.getExternalVoiceId() : null;

        byte[] audio;
        if (returning) {
            audio = textToSpeechPort.synthesize(text, externalVoiceId); // lleva el nombre: no se cachea
        } else {
            String cacheKey = externalVoiceId == null ? "default" : externalVoiceId;
            audio = greetingCache.computeIfAbsent(cacheKey, k -> textToSpeechPort.synthesize(text, externalVoiceId));
        }

        Message assistantMessage = persistMessage(conversation, SenderType.ASSISTANT, text, null, null);

        return new AssistantAudioResponse(
                conversation.getId(),
                assistantMessage.getId(),
                text,
                Base64.getEncoder().encodeToString(audio),
                false,
                null
        );
    }

    private String returningGreeting(User user) {
        String name = (user.getFirstName() == null || user.getFirstName().isBlank())
                ? "" : ", " + user.getFirstName();
        return "Hola de nuevo" + name + ". Qué bueno escucharte. ¿Cómo has estado?";
    }

    private String buildSystemPrompt(User user, String summary) {
        StringBuilder sb = new StringBuilder(MellPrompts.SYSTEM);
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            sb.append("\n\n[PERSONA] Se llama ").append(user.getFirstName()).append(".");
        }
        if (summary != null && !summary.isBlank()) {
            sb.append("\n\n[LO QUE SABES DE ESTA PERSONA]\n").append(summary);
        }
        return sb.toString();
    }

    /** Mensajes del usuario (todas sus conversaciones) que aún no están en el resumen. */
    private List<LlmChatPort.ChatTurn> loadRecent(UUID userId, Instant after) {
        List<Message> pending = messageRepository
                .findByConversationUserIdAndCreatedAtAfterOrderByCreatedAtAsc(userId, after);

        int from = Math.max(0, pending.size() - MAX_LIVE_CONTEXT);
        List<LlmChatPort.ChatTurn> turns = new ArrayList<>();
        for (Message m : pending.subList(from, pending.size())) {
            if (m.getContentText() == null || m.getContentText().isBlank()) continue;
            turns.add(new LlmChatPort.ChatTurn(
                    m.getSender() == SenderType.USER ? "user" : "assistant",
                    m.getContentText()));
        }
        return turns;
    }

    private Conversation resolveConversation(IncomingAudioMessageRequest request, User user) {
        if (request.conversationId() != null) {
            return conversationRepository.findById(request.conversationId())
                    .orElseThrow(() -> new IllegalArgumentException("Conversación no encontrada"));
        }

        VoiceProfile voiceProfile = null;
        if (request.voiceProfileId() != null) {
            voiceProfile = voiceProfileRepository.findById(request.voiceProfileId()).orElse(null);
        }

        return conversationRepository.save(
                Conversation.builder().user(user).voiceProfile(voiceProfile).build());
    }

    private String resolveExternalVoiceId(Conversation conversation) {
        VoiceProfile profile = conversation.getVoiceProfile();
        return profile != null ? profile.getExternalVoiceId() : null; // null => voz default
    }

    private Message persistMessage(Conversation conversation, SenderType sender, String text,
                                   String audioUrl, String detectedSentiment) {
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .contentText(text)
                .audioUrl(audioUrl)
                .detectedSentiment(detectedSentiment)
                .build();
        return messageRepository.save(message);
    }
}