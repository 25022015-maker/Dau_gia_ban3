package com.auction.project.Packets;

/**
 * Packet phản hồi từ Server gửi về Client.
 *
 * <p>Cấu trúc thống nhất cho mọi loại phản hồi — Client chỉ cần kiểm tra
 * {@code type} rồi đọc {@code data} tương ứng. Field {@code data} là Object
 * để Gson có thể serialize/deserialize linh hoạt (String, List, Map, ...).
 *
 * <p>Ví dụ JSON khi broadcast bid mới:
 * <pre>{@code
 * {
 *   "type": "BID_UPDATE",
 *   "message": "Người dùng alice vừa đặt 500,000 VNĐ",
 *   "data": {
 *     "auctionId": "A001",
 *     "currentPrice": 500000,
 *     "leadingBidder": "alice"
 *   }
 * }
 * }</pre>
 */
public class Response {

    /** Loại phản hồi — Client dùng field này để rẽ nhánh xử lý */
    private ResponseType type;

    /** Thông điệp ngắn gọn để hiển thị cho người dùng */
    private String message;

    /**
     * Payload kèm theo — có thể là bất kỳ object nào.
     * Gson sẽ serialize thành JSON object lồng nhau.
     */
    private Object data;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Response() {}

    public Response(ResponseType type, String message) {
        this.type = type;
        this.message = message;
    }

    public Response(ResponseType type, String message, Object data) {
        this.type = type;
        this.message = message;
        this.data = data;
    }

    // ── Static factory methods (tiện dùng trong ServerController) ─────────────

    /** Tạo response lỗi nhanh */
    public static Response error(String message) {
        return new Response(ResponseType.ERROR, message);
    }

    /** Tạo response thành công đơn giản */
    public static Response success(ResponseType type, String message) {
        return new Response(type, message);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public ResponseType getType() {
        return type;
    }

    public void setType(ResponseType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}