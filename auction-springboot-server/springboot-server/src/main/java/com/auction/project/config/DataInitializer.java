package com.auction.project.config;

import com.auction.project.entity.User;
import com.auction.project.entity.enums.Role;
import com.auction.project.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Tự động tạo tài khoản admin mặc định khi server khởi động lần đầu.
 * Nếu đã tồn tại username "admin" thì bỏ qua.
 *
 * Tài khoản mặc định:
 *   username : admin
 *   password : admin123
 *   role     : ADMIN
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository  userRepo;
    private final PasswordEncoder encoder;

    public DataInitializer(UserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder  = encoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepo.existsByUsername("admin")) {
            User admin = new User(
                    "admin",
                    encoder.encode("admin123"),
                    "admin@auction.local",
                    Role.ADMIN
            );
            admin.setBalance(0L);
            userRepo.save(admin);
            log.info("=== Tạo tài khoản admin mặc định: admin / admin123 ===");
        }
        if (!userRepo.existsByUsername("bidder1")) {
            User bidder = new User(
                    "bidder1",
                    encoder.encode("bidder123"),
                    "bidder1@auction.local",
                    Role.BIDDER
            );
            bidder.setBalance(1_000_000L);
            userRepo.save(bidder);
            log.info("=== Tạo tài khoản bidder mặc định: bidder1 / bidder123 ===");
        }
    }
}
