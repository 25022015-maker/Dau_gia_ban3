# 🏆 Hệ Thống Đấu Giá Trực Tuyến

> **Môn:** Lập trình nâng cao — Bài tập lớn  
> **Công nghệ:** Java 25 · JavaFX 21 · Socket TCP · Gson · Maven

---

## 📁 Cấu Trúc Dự Án

```
Dau_gia_ban3/
├── src/main/java/com/auction/project/
│   ├── Client/
│   │   ├── ClientApp.java          # Entry point Client
│   │   ├── ClientHandler.java      # Xử lý response từ Server, cập nhật UI
│   │   ├── NetworkClient.java      # Quản lý kết nối, gửi request
│   │   └── SocketClient.java       # Low-level TCP socket wrapper
│   ├── Controllers/
│   │   └── ServerController.java   # MVC Controller phía Server
│   ├── DAO/
│   │   └── AuctionDAO.java         # Tầng truy cập dữ liệu (chỉ Server dùng)
│   ├── Models/
│   │   ├── Auction.java            # Entity phiên đấu giá + state machine
│   │   └── AuctionManager.java     # Singleton + Observer + ReentrantLock
│   ├── Packets/
│   │   ├── BidRequest.java         # Packet đặt giá Client → Server
│   │   ├── LoginRequest.java       # Packet đăng nhập Client → Server
│   │   ├── Response.java           # Packet phản hồi Server → Client
│   │   └── ResponseType.java       # Enum các loại response
│   └── Server/
│       ├── ClientHandler.java      # Xử lý mỗi client trong 1 thread riêng
│       ├── DataStorage.java        # Lưu trữ dữ liệu xuống file
│       ├── ServerApp.java          # Entry point Server
│       └── SocketServer.java       # Lắng nghe kết nối TCP
├── server-data/                    # Dữ liệu được tạo tự động khi Server chạy
│   └── backups/
├── pom.xml
└── README.md
```

---

## ⚙️ Yêu Cầu Môi Trường

| Công cụ | Phiên bản |
|---------|-----------|
| JDK | 25 (LTS) |
| Maven | 3.9+ |
| IntelliJ IDEA | 2024+ (khuyến nghị) |

---

## 🚀 Hướng Dẫn Chạy

> **Quan trọng:** Phải chạy **Server trước**, Client sau.  
> Server và Client có thể chạy trên cùng máy hoặc khác máy trong cùng mạng LAN.

---

### Bước 1 — Build toàn bộ dự án

Mở terminal tại thư mục gốc `Dau_gia_ban3/`:

```bash
mvn clean compile
```

Kiểm tra output — không được có lỗi `BUILD FAILURE`.

---

### Bước 2 — Chạy Server

#### Cách A: Qua IntelliJ IDEA
1. Mở file `src/main/java/com/auction/project/Server/ServerApp.java`
2. Click chuột phải → **Run 'ServerApp.main()'**

#### Cách B: Qua Maven
```bash
mvn exec:java -Dexec.mainClass="com.auction.project.Server.ServerApp"
```

#### Cách C: Qua JAR
```bash
mvn package -DskipTests
java -cp target/Dau_gia_ban3-1.0-SNAPSHOT.jar com.auction.project.Server.ServerApp
```

**✅ Server khởi động thành công khi thấy log:**
```
═══════════════════════════════════════════
  Server đấu giá đang chạy tại cổng: 9090
  Chờ kết nối từ client...
═══════════════════════════════════════════
ServerController: Đã nạp 3 phiên đấu giá.
```

> **Mặc định:** Server lắng nghe tại `localhost:9090`.  
> Để đổi port, sửa hằng số `SERVER_PORT` trong `ServerApp.java`.

---

### Bước 3 — Chạy Client

> Mở **terminal / run config mới** — KHÔNG dừng Server.  
> Có thể chạy nhiều Client đồng thời để test realtime.

#### Cách A: Qua IntelliJ IDEA
1. Mở file `src/main/java/com/auction/project/Client/ClientApp.java`
2. Click chuột phải → **Run 'ClientApp.main()'**
3. Để chạy Client thứ 2: **Run** → **Edit Configurations** → nhân bản config → chạy thêm

#### Cách B: Qua Maven (terminal riêng)
```bash
# Terminal 2 — Client 1
mvn exec:java -Dexec.mainClass="com.auction.project.Client.ClientApp"

# Terminal 3 — Client 2 (mở terminal mới, chạy song song)
mvn exec:java -Dexec.mainClass="com.auction.project.Client.ClientApp"
```

**✅ Client kết nối thành công khi thấy log:**
```
SocketClient: Đã kết nối tới localhost:9090
Listener thread bắt đầu — chờ dữ liệu từ server...
```

---

### Bước 4 — Tài Khoản Test Sẵn Có

| Username | Password | Vai trò |
|----------|----------|---------|
| `alice` | `alice123` | Bidder |
| `bob` | `bob123` | Bidder |
| `seller1` | `seller123` | Seller |
| `admin` | `admin123` | Admin |

---

### Bước 5 — Dừng Hệ Thống

```bash
# Dừng Client trước: Ctrl + C trong terminal Client
# Dừng Server sau:   Ctrl + C trong terminal Server
```

Server sẽ tự động chạy **final save** (lưu dữ liệu lần cuối) trước khi tắt.

---

## 🧪 Test Cases — Tính Năng Real-time Update (Observer Pattern)

> Mục tiêu: Xác nhận khi **Client A** đặt giá, **Client B** (đang xem cùng phiên) nhận được thông báo **ngay lập tức** mà không cần refresh.

---

### TC-01 — Broadcast Cơ Bản

**Mô tả:** Một client đặt giá, tất cả client còn lại nhận được BID_UPDATE.

**Chuẩn bị:**
- Khởi động Server
- Mở **2 terminal**, chạy **Client A** và **Client B**
- Cả hai đăng nhập và subscribe phiên `A001`

**Các bước:**

| Bước | Hành động | Client A | Client B |
|------|-----------|----------|----------|
| 1 | Client A và B cùng subscribe phiên `A001` | Nhận trạng thái hiện tại | Nhận trạng thái hiện tại |
| 2 | Client A đặt giá `6,000,000` | Nhận `BID_SUCCESS` | Nhận `BID_UPDATE` ngay lập tức |
| 3 | Client B đặt giá `7,000,000` | Nhận `BID_UPDATE` ngay lập tức | Nhận `BID_SUCCESS` |

**Kết quả mong đợi:**
```
[Client B] 🔔 BID_UPDATE: 💰 alice vừa đặt 6,000,000 VNĐ cho 'Laptop Dell XPS 15'
[Client B] 🔔 BID_UPDATE: 💰 bob vừa đặt 7,000,000 VNĐ cho 'Laptop Dell XPS 15'
```

**✅ Pass:** Client B nhận thông báo trong vòng < 1 giây sau khi Client A đặt giá.  
**❌ Fail:** Client B không nhận được, hoặc phải chờ > 5 giây.

---

### TC-02 — Client Không Subscribe Không Nhận Broadcast

**Mô tả:** Client chưa subscribe phiên không nhận BID_UPDATE của phiên đó.

**Chuẩn bị:**
- Client A: subscribe phiên `A001`
- Client B: **KHÔNG** subscribe phiên nào (chỉ đăng nhập)

**Các bước:**
1. Client A đặt giá `6,500,000` vào phiên `A001`

**Kết quả mong đợi:**
```
[Client A] BID_SUCCESS — Đặt giá thành công!
[Client B] (im lặng — không nhận được gì)
```

**✅ Pass:** Client B không nhận BID_UPDATE.  
**❌ Fail:** Client B nhận BID_UPDATE dù chưa subscribe.

---

### TC-03 — Concurrent Bidding (Đặt Giá Đồng Thời)

**Mô tả:** 2 client đặt giá **cùng lúc** — chỉ một người thắng, không có lost update.

**Chuẩn bị:**
- Giá hiện tại phiên `A001`: `5,000,000`
- Client A và B cùng subscribe phiên `A001`

**Các bước:**
1. Trong vòng < 1 giây: Client A gửi bid `6,000,000` VÀ Client B gửi bid `6,000,000`

**Kết quả mong đợi — một trong hai kịch bản:**

*Kịch bản 1 — Client A vào trước:*
```
[Client A] BID_SUCCESS  — giá 6,000,000 được chấp nhận
[Client B] BID_FAILURE  — "Giá đặt (6,000,000) phải cao hơn giá hiện tại (6,000,000)"
[Cả hai]   BID_UPDATE   — leader: alice, currentPrice: 6,000,000
```

*Kịch bản 2 — Client B vào trước:*
```
[Client B] BID_SUCCESS  — giá 6,000,000 được chấp nhận
[Client A] BID_FAILURE  — "Giá đặt (6,000,000) phải cao hơn giá hiện tại (6,000,000)"
[Cả hai]   BID_UPDATE   — leader: bob, currentPrice: 6,000,000
```

**✅ Pass:** Đúng một người nhận SUCCESS, một người nhận FAILURE. Chỉ có một BID_UPDATE duy nhất được broadcast.  
**❌ Fail:** Cả hai nhận SUCCESS (lost update), hoặc giá bị rollback về 5,000,000.

---

### TC-04 — Client Ngắt Kết Nối Không Ảnh Hưởng Broadcast

**Mô tả:** Khi một client disconnect, server xóa khỏi observer list — các client còn lại vẫn nhận broadcast bình thường.

**Chuẩn bị:**
- 3 client (A, B, C) cùng subscribe phiên `A001`

**Các bước:**
1. Client C đóng ứng dụng (Ctrl+C)
2. Kiểm tra Server log xác nhận C đã bị xóa khỏi observer
3. Client A đặt giá `8,000,000`

**Kết quả mong đợi:**
```
[Server]   DISCONNECT | Client: xxxxxxxx | Đã xóa khỏi tất cả observer.
[Client A] BID_SUCCESS
[Client B] 🔔 BID_UPDATE — nhận bình thường
[Client C] (đã offline — không nhận gì)
```

**✅ Pass:** Server không crash, Client B vẫn nhận được BID_UPDATE.  
**❌ Fail:** Server ném `NullPointerException` hoặc `SocketException` khi cố broadcast tới C.

---

### TC-05 — Đặt Giá Thấp Hơn Giá Hiện Tại

**Mô tả:** Server từ chối bid không hợp lệ, không broadcast BID_UPDATE.

**Chuẩn bị:**
- Giá hiện tại phiên `A001`: `6,000,000`
- Client A và B đều subscribe phiên `A001`

**Các bước:**
1. Client A đặt giá `4,000,000` (thấp hơn giá hiện tại)

**Kết quả mong đợi:**
```
[Client A] BID_FAILURE — "Giá đặt (4,000,000) phải cao hơn giá hiện tại (6,000,000)."
[Client B] (im lặng — không có BID_UPDATE)
```

**✅ Pass:** Chỉ Client A nhận FAILURE. Client B không nhận thông báo nào.  
**❌ Fail:** BID_UPDATE được broadcast dù bid không hợp lệ.

---

### TC-06 — Subscribe Nhận Trạng Thái Hiện Tại Ngay Lập Tức

**Mô tả:** Client subscribe muộn (khi phiên đang chạy) phải nhận được trạng thái hiện tại ngay.

**Chuẩn bị:**
- Phiên `A001` đang chạy, giá hiện tại đã lên `9,000,000`
- Client C vừa mở ứng dụng, chưa subscribe

**Các bước:**
1. Client C gửi lệnh subscribe phiên `A001`

**Kết quả mong đợi:**
```
[Client C] BID_UPDATE — nhận ngay trạng thái: currentPrice=9,000,000, leader=alice
```

**✅ Pass:** Client C nhận được giá hiện tại ngay khi subscribe, không cần chờ bid tiếp theo.  
**❌ Fail:** Client C phải chờ đến khi có bid mới mới biết giá hiện tại.

---

## 🔧 Troubleshooting

**Lỗi: `Connection refused: localhost:9090`**
> Server chưa chạy. Kiểm tra lại Bước 2.

**Lỗi: `Address already in use`**
> Port 9090 đang bị chiếm. Chạy lệnh sau để kill process:
> ```bash
> # Windows
> netstat -ano | findstr :9090
> taskkill /PID <PID> /F
>
> # macOS / Linux
> lsof -ti:9090 | xargs kill -9
> ```

**Client đặt giá nhưng không thấy BID_UPDATE ở client khác**
> Kiểm tra xem client kia đã gọi `subscribeToAuction(auctionId)` chưa — phải subscribe thì mới vào observer list.

**`NotSerializableException` khi Server lưu dữ liệu**
> Đảm bảo `Auction.java` có `implements Serializable` và `serialVersionUID`.

---

## 📐 Kiến Trúc Tóm Tắt

```
CLIENT                          SERVER
──────                          ──────
NetworkClient                   SocketServer
    │  JSON over TCP                 │
    │ ──────────────────────────►    │  accept()
    │                           ClientHandler (Thread per client)
    │                                │
    │                           ServerController (MVC)
    │                                │
    │                           AuctionManager (Singleton)
    │                            ├── ReentrantLock (per auction)
    │                            └── Observer List (per auction)
    │                                │
    │  BID_UPDATE broadcast      notifyObservers()
    │ ◄──────────────────────────    │ → sendBidUpdate() mỗi ClientHandler
```

---

*Được tạo cho môn Lập trình nâng cao — Đại học Công nghệ*