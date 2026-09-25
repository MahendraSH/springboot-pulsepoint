package basic.sprinng.pulsepoint.pulsepoint.stream;

/**
 * Functional interface for RPC functions that produce an SSE stream.
 */
@FunctionalInterface
public interface PulsePointStream {
    /**
     * Executes streaming logic by emitting data chunks to the provided emitter.
     *
     * @param emitter the stream emitter
     * @throws Exception if any error occurs
     */
    void stream(PulsePointStreamEmitter emitter) throws Exception;
}
