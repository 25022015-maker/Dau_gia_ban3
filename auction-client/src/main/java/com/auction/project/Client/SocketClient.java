package com.auction.project.Client;

import com.auction.project.Packets.GsonFactory;
import com.auction.project.Packets.Response;
import com.auction.project.Packets.ResponseType;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class SocketClient {

    private static final Logger logger = Logger.getLogger(SocketClient.class.getName());
    // FIX: dùng GsonFactory thay vì new Gson() để hỗ trợ LocalDateTime
    private static final Gson gson = GsonFactory.create();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    private Thread listenerThread;
    private Consumer<Response> onResponse;

    public SocketClient() {}

    public SocketClient(String host, int port) throws IOException {
        connect(host, port);
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        connected = true;
        logger.info("SocketClient: Đã kết nối tới " + host + ":" + port);
        startListenerThread();
    }

    public void disconnect() {
        connected = false;
        try {
            if (listenerThread != null && listenerThread.isAlive()) listenerThread.interrupt();
            if (socket != null && !socket.isClosed()) socket.close();
            logger.info("SocketClient: Đã ngắt kết nối.");
        } catch (IOException e) {
            logger.warning("SocketClient: Lỗi khi ngắt kết nối — " + e.getMessage());
        }
    }

    private void startListenerThread() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while (connected && (line = in.readLine()) != null) {
                    final String rawJson = line;
                    try {
                        Response response = gson.fromJson(rawJson, Response.class);
                        if (onResponse != null) onResponse.accept(response);
                    } catch (Exception parseEx) {
                        logger.warning("SocketClient: Không parse được response — " + parseEx.getMessage());
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    logger.warning("SocketClient: Mất kết nối server — " + e.getMessage());
                    if (onResponse != null) {
                        onResponse.accept(new Response(ResponseType.ERROR, "Mất kết nối tới server."));
                    }
                }
            }
        }, "SocketListenerThread");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public synchronized void send(Object req) {
        if (!connected || socket == null || socket.isClosed()) {
            System.err.println("Không còn kết nối server.");
            return;
        }
        try {
            String json = gson.toJson(req);
            out.println(json);
        } catch (Exception e) {
            logger.warning("Lỗi gửi dữ liệu: " + e.getMessage());
            connected = false;
        }
    }

    public synchronized void sendRaw(String jsonString) {
        if (!connected || socket == null || socket.isClosed()) {
            System.err.println("Không còn kết nối server.");
            return;
        }
        out.println(jsonString);
    }

    public void setOnResponse(Consumer<Response> onResponse) {
        this.onResponse = onResponse;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}