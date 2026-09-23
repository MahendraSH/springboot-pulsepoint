package basic.sprinng.pulsepoint.pulsepoint;

import java.util.Map;

@FunctionalInterface
public interface PulsePointRpcFunction {
    Object invoke(Map<String, Object> parameters) throws Exception;
}
