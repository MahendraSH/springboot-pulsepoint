package basic.sprinng.pulsepoint.pulsepoint.stream;

import java.io.IOException;

/**
 * Emitter interface for sending Server-Sent Events (SSE) chunks to a PulsePoint client.
 */
public interface PulsePointStreamEmitter {
    /**
     * Sends an object serialized as JSON in `data: <json>\n\n` format.
     */
    void send(Object data) throws IOException;

    /**
     * Sends a raw string in `data: <text>\n\n` format.
     */
    void sendRaw(String text) throws IOException;

    /**
     * Completes the stream normally.
     */
    void complete();

    /**
     * Signals an error during streaming.
     */
    void error(Throwable t);
}
