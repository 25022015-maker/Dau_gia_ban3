# Hệ Thống Đấu Giá Trực Tuyến

> **Môn:** Lập trình nâng cao — Bài tập lớn
> **Công nghệ:** Java 21 · JavaFX 21 · Spring Boot 3 · MySQL · WebSocket (STOMP) · Maven

---

## Cấu Trúc Dự Án

```
Dau_gia_ban3/
├── auction-client/                  # Giao diện JavaFX
│   └── src/main/java/com/example/
│       ├── app/ClientApp.java       # Entry point client
│       ├── controller/              # Các controller JavaFX
│       └── service/                 # ApiClient, SessionManager, StompClient
├── auction-common/                  # Các class dùng chung
├── auction-springboot-server/       # Backend Spring Boot
│   └── src/main/java/com/auction/project/
│       ├── controller/              # REST API endpoints
│       ├── service/                 # Business logic
│       ├── entity/                  # JPA entities
│       ├── repository/              # Spring Data JPA
│       ├── security/                # JWT + Spring Security
│       └── config/                  # WebSocket, CORS, DataInitializer
└── pom.xml
```

---

## Yêu Cầu Môi Trường

| Công cụ | Phiên bản |
|---------|-----------|
| JDK | 21 |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| IntelliJ IDEA | 2024+ (khuyến nghị) |

---

## Cấu Hình Database

Tạo database MySQL trước khi khởi động server:

```sql
CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Cấu hình kết nối trong `auction-springboot-server/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/auction_db
spring.datasource.username=root
spring.datasource.password=123456
```

---

## Hướng Dẫn Chạy

> **Quan trọng:** Phải khởi động **Server trước**, Client sau.

### Bước 1 — Khởi động Server (Spring Boot)

Mở terminal, `cd` vào thư mục server rồi chạy:

```bash
cd D:\Dau_gia_ban3\auction-springboot-server\springboot-server
mvn spring-boot:run
```

Server chạy tại `http://localhost:8080`.

Khi khởi động lần đầu, hệ thống tự tạo tài khoản admin mặc định:
```
=== Tạo tài khoản admin mặc định: admin / admin123 ===
```

### Bước 2 — Chạy Client (JavaFX)

Mở **terminal mới** (không tắt server), `cd` vào thư mục gốc rồi chạy:

```bash
cd D:\Dau_gia_ban3
mvn javafx:run -pl auction-client
```

Để chạy nhiều client đồng thời (test real-time), mở thêm terminal và chạy lại lệnh trên.

---

## Tài Khoản Test

| Username | Email | Password | Vai trò | Số dư |
|----------|-------|----------|---------|-------|
| `admin` | `admin@auction.local` | `admin123` | Admin | 0 |
| `bidder1` | `bidder1@auction.local` | `bidder123` | Bidder | 1,000,000 |
| `seller` | `seller123@gmail.com` | `123456` | Seller | — |
| `seller1` | `seller1234@gmail.com` | `123456` | Seller | — |

> Tài khoản `admin` và `bidder1` được tạo tự động khi server khởi động lần đầu.
> Tài khoản seller đăng ký thông qua giao diện ứng dụng với vai trò **Người bán**.

---

## Tính Năng

### Người dùng (Bidder / Seller)
- Đăng ký / đăng nhập với JWT
- Xem danh sách phiên đấu giá theo trạng thái (Đang diễn ra / Sắp diễn ra / Đã kết thúc)
- Tìm kiếm phiên đấu giá theo tên sản phẩm
- Xem chi tiết phiên và lịch sử đặt giá
- Đặt giá thủ công
- **Tự động đặt giá**: thiết lập giá tối đa và bước nhảy — hệ thống tự đặt giá mỗi khi có người khác vượt qua
- Nạp tiền vào tài khoản
- Seller tạo phiên đấu giá mới và hủy phiên của mình

### Admin
- Xem thống kê tổng quan (tổng phiên, tổng người dùng, tổng lượt đặt giá)
- Quản lý người dùng (khóa / mở khóa tài khoản)
- Hủy bất kỳ phiên đấu giá nào

### Thời gian thực
- Giá hiện tại, người dẫn đầu, số lượt đặt giá cập nhật tức thì qua WebSocket (STOMP)
- Không cần tải lại trang

### Chống sniping (Anti-sniping)
- Nếu có người đặt giá trong N giây cuối, thời gian kết thúc tự động được gia hạn

---

## Kiến Trúc

```
JavaFX Client                     Spring Boot Server
─────────────                     ─────────────────
ApiClient (HTTP)   ─── REST ───►  AuthController
                                  AuctionController
                                  AdminController
StompClient (WS)   ◄─ STOMP ───  AuctionService
                                   └─ SimpMessagingTemplate
                                       └─► /topic/auction/{id}
```

### Luồng đặt giá
1. Client gửi `POST /api/auctions/{id}/bid` với JWT
2. Server kiểm tra: phiên đang chạy, giá hợp lệ, số dư đủ
3. Server cập nhật giá trong DB (pessimistic lock để tránh race condition)
4. Server broadcast `BidUpdateMessage` tới `/topic/auction/{id}`
5. Tất cả client đang xem phiên nhận cập nhật tức thì
6. Server kiểm tra auto-bid chain: nếu có người đăng ký auto-bid với giá cao hơn, tự động đặt giá tiếp

---

## Troubleshooting

**Lỗi kết nối database**
> Kiểm tra MySQL đang chạy và thông tin trong `application.properties` đúng.

**`Port 8080 already in use`**
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Client không hiển thị cập nhật real-time**
> Kiểm tra server đang chạy và WebSocket kết nối thành công (xem console log của client).

**Không đăng nhập được bằng tài khoản admin**
> Khởi động lại server — tài khoản admin chỉ được tạo khi server khởi động và chưa tồn tại trong DB.
