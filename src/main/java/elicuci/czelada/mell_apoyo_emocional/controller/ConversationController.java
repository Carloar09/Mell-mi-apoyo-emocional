package elicuci.czelada.mell_apoyo_emocional.controller;

import elicuci.czelada.mell_apoyo_emocional.dto.AssistantAudioResponse;
import elicuci.czelada.mell_apoyo_emocional.dto.IncomingAudioMessageRequest;
import elicuci.czelada.mell_apoyo_emocional.service.ConversationOrchestratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationOrchestratorService orchestratorService;

    public ConversationController(ConversationOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostMapping(path = "/messages", consumes = "multipart/form-data")
    public ResponseEntity<AssistantAudioResponse> sendAudioMessage(
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "conversationId", required = false) UUID conversationId,
            @RequestParam(value = "voiceProfileId", required = false) UUID voiceProfileId,
            @RequestParam("audio") MultipartFile audioFile
    ) {
        IncomingAudioMessageRequest request = new IncomingAudioMessageRequest(
                userId, conversationId, voiceProfileId, audioFile.getContentType()
        );

        byte[] audioBytes;
        try {
            audioBytes = audioFile.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo de audio recibido", e);
        }

        return ResponseEntity.ok(orchestratorService.handleIncomingAudio(request, audioBytes));
    }

    /** Saludo de voz al abrir la sesión: crea la conversación y devuelve el audio. */
    @PostMapping("/greeting")
    public ResponseEntity<AssistantAudioResponse> greeting(
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "voiceProfileId", required = false) UUID voiceProfileId
    ) {
        return ResponseEntity.ok(orchestratorService.greet(userId, voiceProfileId));
    }
}