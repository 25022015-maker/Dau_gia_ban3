package com.example.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class StompClient implements WebSocket.Listener {

    private static final Logger LOG = Logger.getLogger(StompClient.class.getName());
    private static final StompClient INSTANCE = new StompClient();

    private WebSocket ws;
    private final StringBuilder buf = new StringBuilder();
    private final AtomicInteger subCounter = new AtomicInteger(0);
    private volatile boolean connected  = false;
    private volatile boolean connecting = false; // guard chống race condition

    /** topic → list of handlers */
    private final Map<String, List<Consumer<String>>> handlers = new ConcurrentHashMap<>();
    /** topic → sub-id (để gửi UNSUBSCRIBE đúng) */
    private final Map<String, String> subIds = new ConcurrentHashMap<>();

    private StompClient() {}

    public static StompClient getInstance() { return INSTANCE; }

    // ── Connect / Disconnect ──────────────────────────────────────────────────

    public void connect() {
        if (connected || connecting) return;   // ngăn double-connect
        connecting = true;
        String token = SessionManager.getToken();
        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(ApiClient.BASE_URL.replace("http", "ws") + "/ws"), this)
                .thenAccept(sock -> {
                    ws = sock;
                    String frame = "CONNECT\naccept-version:1.2\nhost:localhost\n" +
                            (token != null ? "Authorization:Bearer " + token + "\n" : "") +
                            "\n\0";
                    sendFrame(frame);
                    LOG.info("[STOMP] WebSocket connected, CONNECT sent");
                })
                .exceptionally(ex -> {
                    connecting = false;  // cho phép retry sau
                    LOG.warning("[STOMP] Không kết nối được WebSocket: " + ex.getMessage());
                    return null;
                });
    }

    public void disconnect() {
        connected  = false;
        connecting = false;
        handlers.clear();
        subIds.clear();
        subCounter.set(0);
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
            ws = null;
        }
    }

    // ── Subscribe / Unsubscribe ───────────────────────────────────────────────

    public void subscribe(String topic, Consumer<String> handler) {
        handlers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(handler);
        if (connected && ws != null && !subIds.containsKey(topic)) {
            sendSubscribe(topic);
        }
    }

    public void unsubscribe(String topic) {
        handlers.remove(topic);
        String id = subIds.remove(topic);
        if (id != null && ws != null) {
            sendFrame("UNSUBSCRIBE\nid:" + id + "\n\n\0");
        }
    }

    private void sendSubscribe(String topic) {
        String id = "sub-" + subCounter.getAndIncrement();
        subIds.put(topic, id);
        sendFrame("SUBSCRIBE\nid:" + id + "\ndestination:" + topic + "\n\n\0");
    }

    // ── WebSocket.Listener ────────────────────────────────────────────────────

    @Override
    public CompletionStage<?> onText(WebSocket sock, CharSequence data, boolean last) {
        buf.append(data);
        if (last) { parseFrame(buf.toString()); buf.setLength(0); }
        sock.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket sock, ByteBuffer data, boolean last) {
        buf.append(StandardCharsets.UTF_8.decode(data));
        if (last) { parseFrame(buf.toString()); buf.setLength(0); }
        sock.request(1);
        return null;
    }

    @Override
    public void onOpen(WebSocket sock) { sock.request(1); }

    @Override
    public CompletionStage<?> onClose(WebSocket sock, int statusCode, String reason) {
        connected  = false;
        connecting = false;
        LOG.info("[STOMP] WebSocket closed: " + reason);
        return null;
    }

    @Override
    public void onError(WebSocket sock, Throwable error) {
        connecting = false;
        LOG.warning("[STOMP] Lỗi WebSocket: " + error.getMessage());
    }

    // ── Frame parser ──────────────────────────────────────────────────────────

    private void parseFrame(String raw) {
        String[] frames = raw.split("\0");
        for (String frame : frames) {
            frame = frame.trim();
            if (frame.isEmpty()) continue;

            String[] parts = frame.split("\n\n", 2);
            String[] lines = parts[0].split("\n");
            if (lines.length == 0) continue;
            String command = lines[0].trim();
            String body    = parts.length > 1 ? parts[1].replace("\0", "") : "";

            if ("CONNECTED".equals(command)) {
                connected  = true;
                connecting = false;
                LOG.info("[STOMP] CONNECTED — subscribing " + handlers.size() + " topics");
                // Gửi SUBSCRIBE cho tất cả topic đã đăng ký trước khi kết nối xong
                for (String topic : handlers.keySet()) {
                    if (!subIds.containsKey(topic)) sendSubscribe(topic);
                }

            } else if ("MESSAGE".equals(command)) {
                String destination = null;
                for (int i = 1; i < lines.length; i++) {
                    if (lines[i].startsWith("destination:")) {
                        destination = lines[i].substring("destination:".length()).trim();
                        break;
                    }
                }
                if (destination != null) {
                    List<Consumer<String>> list = handlers.get(destination);
                    if (list != null) {
                        String finalBody = body;
                        list.forEach(h -> {
                            try { h.accept(finalBody); }
                            catch (Exception e) { LOG.warning("[STOMP] handler error: " + e.getMessage()); }
                        });
                    }
                }

            } else if ("ERROR".equals(command)) {
                LOG.warning("[STOMP] ERROR frame: " + body);
            }
        }
    }

    private void sendFrame(String frame) {
        if (ws == null) return;
        ws.sendText(frame, true);
    }

    public boolean isConnected() { return connected; }
}
