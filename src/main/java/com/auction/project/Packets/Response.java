package com.auction.project.Packets;

import java.io.Serializable;

public class Response implements Serializable {
    private String type;
    private String message;

    public Response(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() { return type; }
    public String getMessage() { return message; }
}