package service.port;

import java.util.List;

// puerto de la respuesta empatica
//Implementacion previstas seria onpeai, gpt4, google gmeini
public interface LlmChatPort {
    // SystemPormpt seria el prmopt de sistema junto a las reglas de la app
    // conversationlog histpral reconete del  usuario con el assitente
    //usermessage es el ultimo mensaje del usuario transcirot y filtrado
    String generateEmpathicReply(String systemPrompt, List<ChatTurn> conversationLog, String userMessage);

    record ChatTurn(String role, String content) {
    }
}
