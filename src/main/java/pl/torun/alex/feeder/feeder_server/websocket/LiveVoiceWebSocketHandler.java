package pl.torun.alex.feeder.feeder_server.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import pl.torun.alex.feeder.feeder_server.config.SpeakerProperties;
import pl.torun.alex.feeder.feeder_server.service.AudioService;
import pl.torun.alex.feeder.feeder_server.service.JwtService;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveVoiceWebSocketHandler extends BinaryWebSocketHandler {

    private final SpeakerProperties speakerProperties;
    private final AudioService audioService;
    private final JwtService jwtService;

    private static final String SOCKET_ATTR = "speaker_socket";
    private static final String STREAM_ATTR = "speaker_output_stream";

    // Track active sessions to prevent concurrent talkers from colliding
    private final Map<String, Socket> activeConnections = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        if (!speakerProperties.isEnabled()) {
            log.warn("Speaker disabled, closing live voice session {}", session.getId());
            session.close(new CloseStatus(4003, "Speaker disabled"));
            return;
        }

        // Validate JWT token from query string (?token=...)
        String token = extractToken(session.getUri());
        if (token == null || !jwtService.validateToken(token)) {
            log.warn("Unauthorized live voice connection attempt: session {}", session.getId());
            session.close(new CloseStatus(4001, "Unauthorized"));
            return;
        }

        String username = jwtService.extractUsername(token);
        log.info("Live voice session established for user '{}', session: {}", username, session.getId());

        // Connect to Keenetic speaker socket
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(speakerProperties.getHost(), speakerProperties.getPort()),
                    speakerProperties.getTimeoutMs());
            socket.setTcpNoDelay(true); // Lowest latency
            OutputStream os = socket.getOutputStream();

            session.getAttributes().put(SOCKET_ATTR, socket);
            session.getAttributes().put(STREAM_ATTR, os);
            activeConnections.put(session.getId(), socket);

            // Send wake-up chime to un-mute speaker
            if (speakerProperties.isChimeEnabled()) {
                byte[] chime = audioService.getWakeUpChime();
                if (chime != null && chime.length > 0) {
                    os.write(chime);
                    os.flush();
                    log.info("Sent wake-up chime for live session {}", session.getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to connect live session to speaker {}:{}: {}",
                    speakerProperties.getHost(), speakerProperties.getPort(), e.getMessage());
            session.close(new CloseStatus(4500, "Could not connect to speaker"));
        }
    }

    @Override
    protected void handleBinaryMessage(@NonNull WebSocketSession session, @NonNull BinaryMessage message) {
        OutputStream os = (OutputStream) session.getAttributes().get(STREAM_ATTR);
        Socket socket = (Socket) session.getAttributes().get(SOCKET_ATTR);

        if (os == null || socket == null || socket.isClosed()) {
            return;
        }

        ByteBuffer payload = message.getPayload();
        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);

        try {
            synchronized (socket) {
                os.write(bytes);
                os.flush();
            }
        } catch (Exception e) {
            log.error("Error streaming live audio chunk to speaker: {}", e.getMessage());
            cleanupSession(session);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        log.info("Live voice session closed: {}, status: {}", session.getId(), status);
        cleanupSession(session);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        log.warn("Transport error in live voice session {}: {}", session.getId(), exception.getMessage());
        cleanupSession(session);
    }

    private void cleanupSession(WebSocketSession session) {
        Socket socket = activeConnections.remove(session.getId());
        if (socket != null && !socket.isClosed()) {
            try {
                OutputStream os = socket.getOutputStream();
                if (os != null) {
                    os.flush();
                }
                socket.close();
                log.info("Closed speaker socket for session {}", session.getId());
            } catch (Exception e) {
                log.debug("Error closing speaker socket: {}", e.getMessage());
            }
        }
    }

    private String extractToken(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String param : uri.getQuery().split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && "token".equalsIgnoreCase(pair[0])) {
                return pair[1];
            }
        }
        return null;
    }
}
