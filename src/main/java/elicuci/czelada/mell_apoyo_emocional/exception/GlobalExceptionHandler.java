package elicuci.czelada.mell_apoyo_emocional.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Error no controlado procesando la solicitud", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("Error interno procesando la solicitud"));
    }

    private Map<String, Object> errorBody(String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "message", message
        );
    }
}
