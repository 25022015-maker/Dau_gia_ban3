package com.auction.project.Server;

import com.auction.project.Packets.BidRequest;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quản lý lưu trữ dữ liệu bằng Java Serialization.
 *
 * Tính năng nâng cấp so với phiên bản cũ:
 *  - Atomic write: ghi ra .tmp rồi rename → không mất dữ liệu khi crash
 *  - Tự động tạo backup có timestamp trước mỗi lần ghi
 *  - Auto-save: background thread tự lưu mỗi N giây
 *  - Thread-safe: dùng ReentrantLock cho thao tác ghi file
 */
public class DataStorage {

    private static final Logger LOG = Logger.getLogger(DataStorage.class.getName());
    private static final DateTimeFormatter BACKUP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ── Đường dẫn file ────────────────────────────────────────────────────────
    private static final Path DATA_DIR   = Paths.get("server-data");
    private static final Path BACKUP_DIR = DATA_DIR.resolve("backups");
    private static final Path USER_FILE  = DATA_DIR.resolve("users.dat");
    private static final Path BID_FILE   = DATA_DIR.resolve("bids.dat");

    // ── Lock để tránh 2 thread ghi file cùng lúc ─────────────────────────────
    private static final java.util.concurrent.locks.ReentrantLock LOCK =
            new java.util.concurrent.locks.ReentrantLock();

    // ── Auto-save ─────────────────────────────────────────────────────────────
    private static ScheduledExecutorService autoSaveScheduler;

    // ─────────────────────────────────────────────────────────────────────────
    //  USERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lưu danh sách users xuống file (atomic write + backup).
     *
     * @param users map username → passwordHash
     */
    public static void saveUsers(java.util.concurrent.ConcurrentHashMap<String, String> users) {
        LOCK.lock();
        try {
            ensureDirs();
            backupIfExists(USER_FILE);
            atomicWrite(USER_FILE, users);
            LOG.info("Đã lưu " + users.size() + " users → " + USER_FILE);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Lỗi khi lưu users: " + e.getMessage(), e);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Tải danh sách users từ file. Trả về map rỗng nếu chưa có file.
     */
    @SuppressWarnings("unchecked")
    public static java.util.concurrent.ConcurrentHashMap<String, String> loadUsers() {
        if (!Files.exists(USER_FILE)) {
            LOG.info("Chưa có file users, khởi tạo mới.");
            return new java.util.concurrent.ConcurrentHashMap<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(USER_FILE)))) {
            java.util.concurrent.ConcurrentHashMap<String, String> users =
                    (java.util.concurrent.ConcurrentHashMap<String, String>) ois.readObject();
            LOG.info("Đã tải " + users.size() + " users từ " + USER_FILE);
            return users;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Lỗi khi đọc users: " + e.getMessage(), e);
            return new java.util.concurrent.ConcurrentHashMap<>();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BIDS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lưu danh sách bids xuống file (atomic write + backup).
     *
     * @param bids danh sách BidRequest cần lưu
     */
    public static void saveBids(List<BidRequest> bids) {
        LOCK.lock();
        try {
            ensureDirs();
            backupIfExists(BID_FILE);
            atomicWrite(BID_FILE, (Serializable) new ArrayList<>(bids));
            LOG.info("Đã lưu " + bids.size() + " bids → " + BID_FILE);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Lỗi khi lưu bids: " + e.getMessage(), e);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Tải danh sách bids từ file. Trả về list rỗng nếu chưa có file.
     */
    @SuppressWarnings("unchecked")
    public static List<BidRequest> loadBids() {
        if (!Files.exists(BID_FILE)) {
            LOG.info("Chưa có file bids, khởi tạo mới.");
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(BID_FILE)))) {
            List<BidRequest> bids = (List<BidRequest>) ois.readObject();
            LOG.info("Đã tải " + bids.size() + " bids từ " + BID_FILE);
            return bids;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Lỗi khi đọc bids: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AUTO-SAVE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bật auto-save: cứ mỗi {@code intervalSeconds} giây, tự động lưu dữ liệu.
     *
     * Gọi trong ServerApp khi server khởi động:
     * <pre>
     *   DataStorage.startAutoSave(server.getUsers(), server.getBids(), 60);
     * </pre>
     *
     * @param users           tham chiếu đến map users đang dùng trên server
     * @param bids            tham chiếu đến list bids đang dùng trên server
     * @param intervalSeconds chu kỳ lưu (giây)
     */
    public static void startAutoSave(
            java.util.concurrent.ConcurrentHashMap<String, String> users,
            List<BidRequest> bids,
            long intervalSeconds) {

        if (autoSaveScheduler != null && !autoSaveScheduler.isShutdown()) {
            LOG.warning("AutoSave đã chạy rồi, bỏ qua.");
            return;
        }

        autoSaveScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auto-save-thread");
            t.setDaemon(true);  // không chặn JVM tắt
            return t;
        });

        autoSaveScheduler.scheduleAtFixedRate(() -> {
            LOG.info("[Auto-save] Đang lưu dữ liệu...");
            saveUsers(users);
            saveBids(bids);
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);

        LOG.info("Auto-save đã bật – chu kỳ " + intervalSeconds + " giây.");
    }

    /**
     * Tắt auto-save và thực hiện lưu lần cuối trước khi tắt server.
     *
     * Gọi trong shutdown hook của ServerApp:
     * <pre>
     *   Runtime.getRuntime().addShutdownHook(new Thread(() ->
     *       DataStorage.stopAutoSave(users, bids)));
     * </pre>
     */
    public static void stopAutoSave(
            java.util.concurrent.ConcurrentHashMap<String, String> users,
            List<BidRequest> bids) {

        if (autoSaveScheduler != null) {
            autoSaveScheduler.shutdown();
        }
        // Lưu lần cuối khi tắt server
        LOG.info("[Auto-save] Lưu lần cuối trước khi tắt...");
        saveUsers(users);
        saveBids(bids);
        LOG.info("[Auto-save] Đã dừng.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Tạo thư mục data/ và backups/ nếu chưa tồn tại. */
    private static void ensureDirs() throws IOException {
        Files.createDirectories(DATA_DIR);
        Files.createDirectories(BACKUP_DIR);
    }

    /**
     * Nếu file đã tồn tại, copy vào backups/ với tên có timestamp.
     * Ví dụ: users_20250505_143022.bak
     */
    private static void backupIfExists(Path target) throws IOException {
        if (!Files.exists(target)) return;
        String ts       = LocalDateTime.now().format(BACKUP_FMT);
        String baseName = target.getFileName().toString().replace(".dat", "");
        Path   backup   = BACKUP_DIR.resolve(baseName + "_" + ts + ".bak");
        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
        LOG.info("Backup → " + backup);
    }

    /**
     * Ghi object ra file một cách atomic:
     * 1. Ghi vào file tạm (.tmp)
     * 2. Rename sang tên thật → nếu crash ở bước 1, file cũ vẫn còn nguyên
     */
    private static void atomicWrite(Path target, Serializable data) throws IOException {
        Path tmp = target.getParent().resolve(target.getFileName() + ".tmp");
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
