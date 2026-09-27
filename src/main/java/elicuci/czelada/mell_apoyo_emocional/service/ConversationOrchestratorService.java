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
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;


//Punto único de orquestación del flujo conversacional de MELL:
//audio usuario -> [STT] -> texto -> [Guardrails] -> (LLM | derivación crisis)
//        -> texto respuesta -> [TTS con voice_profile] -> audio respuesta
// Este servicio SOLO conoce los puertos (interfaces), nunca las clases
// concretas de OpenAI/ElevenLabs/S3. Eso permite:
// cambiar de proveedor sin tocar esta clase,
//  testear el flujo completo con mocks de los 4 puertos.



@Service
public class ConversationOrchestratorService {
    private static final String SYSTEM_PROMPT = """
            Eres MELL, un acompañante emocional. Tu rol es escuchar activamente,
            validar emociones y sostener una conversación cálida y empática.
            No eres un profesional de salud mental, no diagnosticas ni das
            tratamiento clínico. Mantén respuestas breves, cercanas y en español.
            """;

    private final SpeechToTextPort speechToTextPort;
    private final LlmChatPort llmChatPort;
    private final TextToSpeechPort textToSpeechPort;
    private final AudioStoragePort audioStoragePort;
    private final GuardrailsService guardrailsService;

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final VoiceProfileRepository voiceProfileRepository;
    private final MessageRepository messageRepository;

    @Value("${mell.guardrails.crisis-hotline-message}")
    private String crisisHotlineMessage;

    public ConversationOrchestratorService(SpeechToTextPort speechToTextPort,
                                           LlmChatPort llmChatPort,
                                           TextToSpeechPort textToSpeechPort,
                                           AudioStoragePort audioStoragePort,
                                           GuardrailsService guardrailsService,
                                           UserRepository userRepository,
                                           ConversationRepository conversationRepository,
                                           VoiceProfileRepository voiceProfileRepository,
                                           MessageRepository messageRepository) {
        this.speechToTextPort = speechToTextPort;
        this.llmChatPort = llmChatPort;
        this.textToSpeechPort = textToSpeechPort;
        this.audioStoragePort = audioStoragePort;
        this.guardrailsService = guardrailsService;
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.voiceProfileRepository = voiceProfileRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public AssistantAudioResponse handleIncomingAudio(IncomingAudioMessageRequest request, byte[] audioBytes) {

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Conversation conversation = resolveConversation(request, user);

        // 1) STT: audio del usuario -> texto
        String userText = speechToTextPort.transcribe(audioBytes, request.mimeType());
        String userAudioUrl = audioStoragePort.upload(audioBytes, request.mimeType(), "users/" + user.getId());

        persistMessage(conversation, SenderType.USER, userText, userAudioUrl, null);

        // 2) Guardrails: evalúa señales de crisis ANTES de llegar al LLM
        GuardrailsService.GuardrailsResult guardrailsResult = guardrailsService.evaluate(userText);

        if (guardrailsResult.isCrisis()) {
            Message assistantMsg = persistMessage(conversation, SenderType.ASSISTANT,
                    crisisHotlineMessage, null, "CRISIS");

            return new AssistantAudioResponse(
                    conversation.getId(),
                    assistantMsg.getId(),
                    crisisHotlineMessage,
                    null,          // no se sintetiza audio: la UI debe mostrar la derivación de forma clara y textual
                    true,
                    crisisHotlineMessage
            );
        }

        // 3) LLM: genera respuesta empática con contexto de la conversación
        List<LlmChatPort.ChatTurn> history = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .map(m -> new LlmChatPort.ChatTurn(
                        m.getSender() == SenderType.USER ? "user" : "assistant",
                        m.getContentText()))
                .collect(Collectors.toList());

        String replyText = llmChatPort.generateEmpathicReply(SYSTEM_PROMPT, history, guardrailsResult.sanitizedText());

        // 4) TTS: sintetiza la respuesta con el voice_profile asociado (o default)
        String externalVoiceId = resolveExternalVoiceId(conversation);
        byte[] replyAudioBytes = textToSpeechPort.synthesize(replyText, externalVoiceId);
        String replyAudioUrl = audioStoragePort.upload(replyAudioBytes, "audio/mpeg", "assistant/" + user.getId());

        Message assistantMessage = persistMessage(conversation, SenderType.ASSISTANT, replyText, replyAudioUrl, null);

        return new AssistantAudioResponse(
                conversation.getId(),
                assistantMessage.getId(),
                replyText,
                Base64.getEncoder().encodeToString(replyAudioBytes),
                false,
                null
        );
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

        Conversation conversation = Conversation.builder()
                .user(user)
                .voiceProfile(voiceProfile)
                .build();

        return conversationRepository.save(conversation);
    }

    private String resolveExternalVoiceId(Conversation conversation) {
        VoiceProfile profile = conversation.getVoiceProfile();
        return profile != null ? profile.getExternalVoiceId() : null; // null => el adaptador usa la voz default
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
