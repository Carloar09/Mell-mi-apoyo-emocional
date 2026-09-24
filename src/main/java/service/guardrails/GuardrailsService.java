package service.guardrails;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

//filtro de seguridad  Filtro de seguridad que se aplica al texto transcrito del usuario ANTES
//de enviarlo su responsabilidad es decidir si el mensaje debe:
// seguir el flujo normal
//  activar el protocolo de derivación a líneas de ayuda
//  Esto es un guardrail de PRIMERA LÍNEA basado en heurística de palabras clave.
//ej. un segundo llamado al LLM con un prompt de clasificación de riesgo,
//MELL nunca debe presentarse como una herramienta de diagnóstico tratamiento clínico: su rol aquí es contener y derivar, no intervenir.

@Service
public class GuardrailsService {

    // Lista de señales de alarma en español. Ampliar/afinar con apoyo
    // de un profesional de salud mental antes de producción real.
    private static final List<String> CRISIS_SIGNALS = List.of(
            "quiero morir",
            "no quiero vivir",
            "quitarme la vida",
            "suicid",
            "hacerme dano",
            "autolesion",
            "no puedo mas",
            "no vale la pena vivir",
            "desvivirme",
            "la vida no vale ni mrd"
    );
    public GuardrailsResult evaluate(String userText) {
        String normalized = normalize(userText);

        boolean crisisDetected = CRISIS_SIGNALS.stream().anyMatch(normalized::contains);

        if (crisisDetected) {
            return GuardrailsResult.crisis();
        }
        return GuardrailsResult.safe(userText);

    }
    private String normalize(String text) {
        String lower = text.toLowerCase(Locale.forLanguageTag("es"));
        String withoutAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents;
    }

    public record GuardrailsResult(boolean isCrisis, String sanitizedText) {
        static GuardrailsResult crisis() {
            return new GuardrailsResult(true, null);
        }

        static GuardrailsResult safe(String text) {
            return new GuardrailsResult(false, text);
        }
    }

}
