package com.auction.project.Packets;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private String message;

    // Constructor mặc định
    public Response() {}

    // Constructor đầy đủ
    public Response(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() { return type; }
    public String getMessage() { return message; }

    public void setType(String type) { this.type = type; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() {
        return "Response{type='" + type + "', message='" + message + "'}";
    }
}
