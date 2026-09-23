package basic.sprinng.pulsepoint.pulsepoint.exception;

import java.util.List;
import java.util.Map;

/**
 * Exception carrying field-level validation errors matching the PulsePoint wire contract:
 * { "error": "Validation failed", "errors": { "fieldName": ["message"] } }
 */
public class PulsePointValidationException extends RuntimeException {

    private final Map<String, List<String>> errors;

    public PulsePointValidationException(String message, Map<String, List<String>> errors) {
        super(message);
        this.errors = errors;
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }
}
