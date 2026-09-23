package basic.sprinng.pulsepoint.pulsepoint;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PulsePointRpcRegistry {

    private final Map<String, PulsePointRpcFunction> functions = new ConcurrentHashMap<>();

    public void register(String name, PulsePointRpcFunction function) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("RPC function name cannot be empty");
        }
        functions.put(name, function);
    }

    public PulsePointRpcFunction lookup(String name) {
        if (name == null) {
            return null;
        }
        return functions.get(name);
    }

    public boolean hasFunction(String name) {
        return name != null && functions.containsKey(name);
    }
}
