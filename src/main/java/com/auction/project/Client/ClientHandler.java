package com.auction.project.Client;

import com.auction.project.Packets.Response;

import java.io.ObjectInputStream;
import java.util.function.Consumer;

public class ClientHandler extends Thread {

    private final ObjectInputStream in;

    // Callback để GUI (JavaFX) lắng nghe response từ server
    // Ví dụ dùng: setOnResponse(res -> Platform.runLater(() -> label.setText(res.getMessage())))
    private Consumer<Response> onResponse;

    public ClientHandler(ObjectInputStream in) {
        this.in = in;
        setDaemon(true); // tự tắt khi app đóng, không treo JVM
        setName("client-listener-thread");
    }

    // Đặt callback — gọi trước khi start()
    public void setOnResponse(Consumer<Response> onResponse) {
        this.onResponse = onResponse;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Object obj = in.readObject();

                if (obj instanceof Response response) {
                    // Nếu GUI đã đăng ký callback → gọi callback
                    if (onResponse != null) {
                        onResponse.accept(response);
                    } else {
                        // Fallback: in ra console nếu chưa có GUI
                        System.out.println("[Server] " + response);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Disconnected from server.");
        }
    }
}