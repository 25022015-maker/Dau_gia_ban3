# Hệ Thống Đấu Giá Trực Tuyến

Ứng dụng đấu giá trực tuyến theo mô hình **Client–Server**. Server xử lý toàn bộ nghiệp vụ qua REST API và WebSocket STOMP; Client là giao diện JavaFX kết nối đến Server qua mạng LAN hoặc localhost.

Người dùng có thể đặt giá thủ công hoặc thiết lập đặt giá tự động. Giá cả và trạng thái phiên được cập nhật real-time tới tất cả client đang xem. Hệ thống tích hợp cơ chế chống snipe (tự động gia hạn phiên khi có bid vào phút cuối), quản lý tài khoản người dùng, và luồng duyệt phiên đấu giá qua Admin.

---

## Công Nghệ Sử Dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21 |
| Server Framework | Spring Boot 3.3 |
| Bảo mật | Spring Security + JWT (jjwt 0.12.5) |
| ORM / Database | Spring Data JPA / Hibernate + MySQL 8.0+ |
| Real-time | Spring WebSocket (STOMP) |
| Client UI | JavaFX 21 (FXML) |
| HTTP Client | Java HttpClient (built-in) |
| Serialization | Gson 2.10.1 |
| Build tool | Apache Maven 3.8+ |

---

## Yêu Cầu Cài Đặt

| Công cụ | Phiên bản tối thiểu |
|---|---|
| JDK | 21 |
| Maven | 3.8+ |
| MySQL | 8.0+ |

**Tạo database trước khi chạy:**

```sql
CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**Kiểm tra và chỉnh thông tin kết nối tại:**
`auction-springboot-server/springboot-server/src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/auction_db
spring.datasource.username=root
spring.datasource.password=123456
```

> Đổi `password` nếu MySQL của bạn dùng mật khẩu khác.

---

## Cấu Trúc Thư Mục

```
Dau_gia_ban3/
├── pom.xml                              # Parent POM (multi-module Maven)
├── README.md
│
├── auction-client/                      # Module Client — giao diện JavaFX
│   └── src/main/java/com/example/
│       ├── app/
│       │   ├── ClientApp.java           # JavaFX Application entry point
│       │   └── Launcher.java            # Main class cho fat JAR
│       ├── controller/
│       │   ├── LoginRegisterController  # Đăng nhập / đăng ký
│       │   ├── MainLayoutController     # Khung chính, điều hướng giữa màn hình
│       │   ├── DashboardController      # Trang chủ — danh sách phiên đấu giá
│       │   ├── ProductCardController    # Card hiển thị từng phiên
│       │   ├── AuctionRoomController    # Phòng đấu giá real-time
│       │   ├── CreateAuctionController  # Tạo phiên đấu giá mới
│       │   ├── AccountController        # Thông tin tài khoản, nạp tiền
│       │   ├── AdminAuctionsController  # Admin — quản lý phiên đấu giá
│       │   └── AdminUsersController     # Admin — quản lý người dùng
│       └── service/
│           ├── ApiClient.java           # Gửi HTTP request đến Server
│           ├── StompClient.java         # Kết nối WebSocket STOMP real-time
│           └── SessionManager.java      # Lưu token, userId, server time offset
│
└── auction-springboot-server/
    └── springboot-server/               # Module Server — Spring Boot
        └── src/main/java/com/auction/project/
            ├── controller/
            │   ├── AuthController       # POST /api/auth/login, /register
            │   ├── AuctionController    # CRUD phiên, đặt giá, auto-bid, lịch sử
            │   ├── AdminController      # Quản lý admin (users, auctions, stats)
            │   ├── UserController       # Profile, nạp tiền, thông báo
            │   └── TimeController       # GET /api/time — đồng bộ giờ server
            ├── service/
            │   ├── AuctionService       # Toàn bộ nghiệp vụ đấu giá
            │   ├── UserService          # Quản lý người dùng, ban/unban
            │   ├── AuthService          # Xác thực, tạo JWT
            │   ├── ItemFactory          # Factory Method tạo đúng loại Item
            │   └── AuctionScheduler     # Cron tự động mở/đóng phiên mỗi 10 giây
            ├── entity/
            │   ├── Auction, Item (+ ArtItem, VehicleItem, ElectronicsItem)
            │   ├── User, BidTransaction, AutoBid, Notification
            │   └── enums/  AuctionStatus, Role
            ├── repository/              # Spring Data JPA repositories
            ├── dto/                     # Request / Response DTO
            ├── security/                # JwtAuthFilter, UserDetailsServiceImpl
            ├── config/                  # SecurityConfig, WebSocketConfig
            └── exception/               # GlobalExceptionHandler, custom exceptions
```

---

## Vị Trí Các File JAR

Sau khi build bằng `mvn package -DskipTests`, các file JAR được tạo tại:

| Module | Đường dẫn |
|---|---|
| **Server** | `auction-springboot-server/springboot-server/target/auction-server-spring-1.0-SNAPSHOT.jar` |
| **Client** | `auction-client/target/auction-client-1.0-SNAPSHOT.jar` |

Cả hai đều là **fat JAR** (đã đóng gói đầy đủ dependencies), chạy trực tiếp bằng `java -jar` mà không cần cài thêm thư viện.

---

## Hướng Dẫn Chạy

> **Quan trọng: phải khởi động Server trước, sau đó mới chạy Client.**

### Bước 1 — Build toàn bộ project

Mở terminal tại thư mục gốc `Dau_gia_ban3/`:

```bash
mvn package -DskipTests
```

Lệnh này build cả Server lẫn Client trong một lần.

---

### Bước 2 — Khởi động Server

```bash
java -jar auction-springboot-server/springboot-server/target/auction-server-spring-1.0-SNAPSHOT.jar
```

Chờ đến khi console hiển thị dòng:
```
Started AuctionServerApplication in X.XXX seconds
```

Server đang chạy tại cổng **8080**. Lần đầu khởi động, hệ thống tự tạo tài khoản admin mặc định:

```
Username: admin  |  Password: admin123
```

---

### Bước 3 — Lấy IP máy Server (chỉ cần nếu Client chạy trên máy khác)

```bash
# Windows
ipconfig
```

Tìm dòng **IPv4 Address**, ví dụ: `192.168.1.50`

---

### Bước 4 — Chạy Client

Mở **terminal mới** (không tắt server):

```bash
# Client và Server cùng một máy
java -jar auction-client/target/auction-client-1.0-SNAPSHOT.jar

# Client chạy trên máy khác trong mạng LAN (thay IP đúng của máy server)
java "-Dserver.url=http://192.168.1.50:8080" -jar auction-client/target/auction-client-1.0-SNAPSHOT.jar
```

Để test nhiều người dùng đồng thời, mở thêm terminal và chạy lại lệnh trên.

---

## Tài Khoản Test Mặc Định

| Username | Email | Password | Vai trò | Số dư |
|---|---|---|---|---|
| `admin` | `admin@auction.local` | `admin123` | Admin | 0 |
| `bidder1` | `bidder1@auction.local` | `bidder123` | Người mua | 1.000.000 VND |
| `seller` | `seller123@gmail.com` | `123456` | Người bán | — |
| `seller1` | `seller1234@gmail.com` | `123456` | Người bán | — |

> Tài khoản `admin` và `bidder1` được tạo tự động khi server khởi động lần đầu. Tài khoản seller đăng ký thông qua giao diện với vai trò **Người bán**.

---

## Danh Sách Chức Năng Đã Hoàn Thành

### Xác thực & Tài khoản
- [x] Đăng ký tài khoản với vai trò Người mua hoặc Người bán
- [x] Đăng nhập bằng JWT, tự động lưu và đính kèm token cho mọi request
- [x] Xem thông tin tài khoản (username, email, số dư, vai trò)
- [x] Nạp tiền vào số dư tài khoản
- [x] Phân quyền 3 vai trò: Người mua (BUYER) / Người bán (SELLER) / Quản trị (ADMIN)

### Luồng Phiên Đấu Giá
- [x] Người bán tạo phiên đấu giá mới với 3 loại sản phẩm: Nghệ thuật, Phương tiện, Điện tử
- [x] Phiên mới tạo ở trạng thái **Chờ duyệt** — chưa hiển thị cho người dùng thường
- [x] Admin duyệt phiên → chuyển sang **Sắp diễn ra** hoặc **Đang diễn ra** tùy thời điểm
- [x] Scheduler tự động mở phiên đúng giờ (PENDING → RUNNING) và đóng phiên hết hạn (RUNNING → FINISHED/CANCELED)
- [x] Người bán hoặc Admin có thể hủy phiên (CANCELED)

### Đặt Giá
- [x] Đặt giá thủ công trong phòng đấu giá; kiểm tra bước giá tối thiểu và số dư
- [x] Đặt giá tự động (AutoBid): thiết lập giá tối đa và bước nhảy (bước nhảy ≥ bước giá tối thiểu của phiên)
- [x] AutoBid kích hoạt ngay khi đăng ký nếu người dẫn đầu hiện tại không phải bản thân
- [x] Chuỗi AutoBid đệ quy: khi A bid tự động thì B, C… cũng phản ứng liên tiếp
- [x] AutoBid tự vô hiệu hóa khi đạt giới hạn maxBid hoặc số dư không đủ
- [x] Người bán không thể tự đặt giá sản phẩm của mình
- [x] Người đang dẫn đầu không thể bid lại

### Thời Gian Thực (WebSocket STOMP)
- [x] Giá hiện tại, người dẫn đầu, số lượt bid cập nhật tức thì đến tất cả client trong phòng
- [x] Đồng hồ đếm ngược đồng bộ với giờ máy Server, không phụ thuộc giờ máy Client
- [x] Nhận thông báo trong ứng dụng khi bị người khác vượt giá
- [x] Nhận thông báo khi thắng phiên đấu giá kèm số tiền và số dư còn lại

### Chống Snipe (Anti-Snipe)
- [x] Nếu có bid trong 60 giây cuối, tự động gia hạn thêm 60 giây
- [x] Thông báo hiển thị ngay trong phòng kèm tên người đã kích hoạt anti-snipe

### Admin — Quản lý Người dùng
- [x] Xem danh sách toàn bộ người dùng
- [x] Khóa tài khoản (BANNED): người dùng bị đẩy ra khỏi ứng dụng ngay lập tức qua WebSocket; không thể đăng nhập lại khi còn bị khóa
- [x] Mở khóa tài khoản (ACTIVE): người dùng có thể đăng nhập lại bình thường

### Admin — Quản lý Phiên Đấu Giá
- [x] Xem toàn bộ phiên kể cả Chờ duyệt, có nhãn trạng thái màu sắc rõ ràng
- [x] Duyệt phiên Chờ duyệt để hiển thị cho người dùng
- [x] Vào phòng đấu giá bất kỳ để quan sát; giao diện Admin thay thế khung đặt giá bằng danh sách người tham gia
- [x] Ban người dùng trực tiếp từ trong phòng đấu giá
- [x] Hủy phiên đấu giá đang hoạt động
- [x] Xóa vĩnh viễn phiên đấu giá khỏi hệ thống (xóa toàn bộ lịch sử đặt giá, AutoBid và sản phẩm liên quan)
- [x] Xem thống kê tổng quan: tổng người dùng, tổng phiên, đang diễn ra, sắp diễn ra, đã kết thúc

---

## Troubleshooting

**Lỗi kết nối database**
> Kiểm tra MySQL đang chạy và thông tin trong `application.properties` khớp (host, port, username, password, tên database).

**`Port 8080 already in use`**
```bash
# Windows — tìm và tắt tiến trình đang chiếm cổng 8080
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Client hiện "Lỗi kết nối"**
> Kiểm tra Server đã khởi động xong chưa và tham số `-Dserver.url` trỏ đúng IP máy Server.

**Không có cập nhật real-time**
> Kiểm tra kết nối WebSocket STOMP thành công trong log Server (`STOMP CONNECTED`).

**Xóa phiên báo lỗi**
> Đảm bảo đã build lại Server (`mvn package -DskipTests`) và **restart** Server sau khi build.

**Báo cáo dự án**
>https://drive.google.com/file/d/1x0wnndyIWgaTtp1K0N4PnV7rK5emOEmr/view?usp=sharing