package com.auction.project.Server;

import com.auction.project.Entities.Auction;
import com.auction.project.Manager.AuctionManager;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng lưu trữ dữ liệu xuống file cho Server.
 *
 * <p><b>Thay đổi so với phiên bản cũ:</b>
 * <ul>
 *   <li>Xóa dependency vào {@code ClientHandler.getCurrentBid()} — không còn tồn tại</li>
 *   <li>Thay {@code BidRequest} bằng {@code Auction} làm đơn vị lưu trữ chính</li>
 *   <li>Lấy dữ liệu từ {@code AuctionManager.getInstance()} thay vì từ ClientHandler</li>
 *   <li>Giữ nguyên cơ chế atomic write + backup + auto-save</li>
 * </ul>
 *
 * <p><b>Nguyên tắc:</b> CHỈ Server mới được dùng class này.
 */
public class DataStorage {

    private static final Logger LOG = Logger.getLogger(DataStorage.class.getName());
    private static final DateTimeFormatter BACKUP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ── FILE PATHS ────────────────────────────────────────────────────────────
    private static final Path DATA_DIR   = Paths.get("server-data");
    private static final Path BACKUP_DIR = DATA_DIR.resolve("backups");

    private static final Path USER_FILE    = DATA_DIR.resolve("users.dat");
    private static final Path AUCTION_FILE = DATA_DIR.resolve("auctions.dat");

    // ── LOCK ──────────────────────────────────────────────────────────────────
    private static final ReentrantLock LOCK = new ReentrantLock();

    // ── AUTO SAVE ─────────────────────────────────────────────────────────────
    private static ScheduledExecutorService autoSaveScheduler;

    // =========================================================
    // USERS
    // =========================================================

    /**
     * Lưu map users xuống file (dùng Java serialization + atomic write).
     *
     * @param users map username → password cần lưu
     */
    public static void saveUsers(ConcurrentHashMap<String, String> users) {
        LOCK.lock();
        try {
            ensureDirs();
            backupIfExists(USER_FILE);
            atomicWrite(USER_FILE, users);
            LOG.info("Saved users: " + users.size());
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Save users error", e);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Load map users từ file. Trả về map rỗng nếu file chưa tồn tại.
     *
     * @return ConcurrentHashMap username → password
     */
    @SuppressWarnings("unchecked")
    public static ConcurrentHashMap<String, String> loadUsers() {
        if (!Files.exists(USER_FILE)) {
            return new ConcurrentHashMap<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(USER_FILE)))) {
            return (ConcurrentHashMap<String, String>) ois.readObject();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Load users error", e);
            return new ConcurrentHashMap<>();
        }
    }

    // =========================================================
    // AUCTIONS
    // =========================================================

    /**
     * Lưu danh sách phiên đấu giá xuống file.
     *
     * <p>Lấy trực tiếp từ {@code AuctionManager} — không cần truyền tham số.
     * Gọi phương thức này để persist trạng thái hiện tại của toàn bộ phiên.
     */
    public static void saveAuctions() {
        LOCK.lock();
        try {
            ensureDirs();
            backupIfExists(AUCTION_FILE);

            // Lấy dữ liệu từ AuctionManager (Singleton) — nguồn sự thật duy nhất
            List<Auction> auctions = new ArrayList<>(
                    AuctionManager.getInstance().getAllAuctions());

            atomicWrite(AUCTION_FILE, (Serializable) auctions);
            LOG.info("Saved auctions: " + auctions.size());
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Save auctions error", e);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Lưu danh sách phiên đấu giá được truyền vào trực tiếp.
     * Dùng khi cần lưu một snapshot cụ thể (không lấy từ AuctionManager).
     *
     * @param auctions danh sách phiên cần lưu
     */
    public static void saveAuctions(List<Auction> auctions) {
        LOCK.lock();
        try {
            ensureDirs();
            backupIfExists(AUCTION_FILE);
            atomicWrite(AUCTION_FILE, (Serializable) new ArrayList<>(auctions));
            LOG.info("Saved auctions: " + auctions.size());
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Save auctions error", e);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Load danh sách phiên đấu giá từ file.
     * Trả về list rỗng nếu file chưa tồn tại.
     *
     * @return List&lt;Auction&gt; đã lưu trước đó
     */
    @SuppressWarnings("unchecked")
    public static List<Auction> loadAuctions() {
        if (!Files.exists(AUCTION_FILE)) {
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(AUCTION_FILE)))) {
            return (List<Auction>) ois.readObject();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Load auctions error", e);
            return new ArrayList<>();
        }
    }

    // =========================================================
    // AUTO SAVE
    // =========================================================

    /**
     * Bắt đầu tự động lưu định kỳ.
     *
     * <p>Lấy dữ liệu từ {@code AuctionManager} tại thời điểm save —
     * không cần truyền tham số snapshot cố định.
     *
     * @param users           map users cần lưu định kỳ
     * @param intervalSeconds chu kỳ lưu (giây)
     */
    public static void startAutoSave(
            ConcurrentHashMap<String, String> users,
            long intervalSeconds) {

        if (autoSaveScheduler != null && !autoSaveScheduler.isShutdown()) {
            return; // Đã chạy rồi, không khởi động lại
        }

        autoSaveScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auto-save");
            t.setDaemon(true);
            return t;
        });

        autoSaveScheduler.scheduleAtFixedRate(() -> {
            LOG.info("[AutoSave] Đang lưu...");
            saveUsers(users);
            saveAuctions(); // Lấy trực tiếp từ AuctionManager
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);

        LOG.info("[AutoSave] Đã bắt đầu, chu kỳ: " + intervalSeconds + "s");
    }

    /**
     * Dừng auto-save và thực hiện lưu lần cuối trước khi shutdown.
     *
     * @param users map users cần lưu lần cuối
     */
    public static void stopAutoSave(ConcurrentHashMap<String, String> users) {
        if (autoSaveScheduler != null) {
            autoSaveScheduler.shutdown();
        }

        LOG.info("[AutoSave] Final save trước khi shutdown...");
        saveUsers(users);
        saveAuctions(); // Lưu trạng thái cuối cùng của tất cả phiên
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /** Tạo thư mục data và backup nếu chưa tồn tại */
    private static void ensureDirs() throws IOException {
        Files.createDirectories(DATA_DIR);
        Files.createDirectories(BACKUP_DIR);
    }

    /**
     * Tạo bản backup có timestamp trước khi ghi đè file.
     * Ví dụ: {@code auctions_20250916_153045.bak}
     */
    private static void backupIfExists(Path target) throws IOException {
        if (!Files.exists(target)) return;

        String ts   = LocalDateTime.now().format(BACKUP_FMT);
        String name = target.getFileName().toString().replace(".dat", "");
        Path backup = BACKUP_DIR.resolve(name + "_" + ts + ".bak");

        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Ghi file theo kiểu atomic: ghi vào file .tmp trước, sau đó rename.
     * Đảm bảo file gốc không bị corrupt nếu JVM crash giữa chừng.
     */
    private static void atomicWrite(Path target, Serializable data) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tmp)))) {
            oos.writeObject(data);
            oos.flush();
        }

        Files.move(tmp, target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }
}