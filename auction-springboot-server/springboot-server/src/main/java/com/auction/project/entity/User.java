package com.auction.project.entity;

import com.auction.project.entity.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity người dùng - ánh xạ bảng users trong DB.
 * Tương thích với schema.sql cũ của nhóm (có thêm cột balance, status).
 *
 * Phân quyền: BIDDER | SELLER | ADMIN
 * Mật khẩu lưu dạng BCrypt hash (không lưu plain text).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String password;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** Số dư tài khoản (VND) */
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long balance = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.BIDDER;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** ACTIVE | BANNED */
    @Column(length = 20)
    private String status = "ACTIVE";

    // ── Constructors ──────────────────────────────────────────────────────────

    public User() {}

    public User(String username, String password, String email, Role role) {
        this.username = username;
        this.password = password;
        this.email    = email;
        this.role     = role;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                       { return id; }
    public void setId(Long id)                { this.id = id; }

    public String getUsername()               { return username; }
    public void setUsername(String username)  { this.username = username; }

    public String getPassword()               { return password; }
    public void setPassword(String password)  { this.password = password; }

    public String getEmail()                  { return email; }
    public void setEmail(String email)        { this.email = email; }

    public Long getBalance()                  { return balance; }
    public void setBalance(Long balance)      { this.balance = balance; }

    public Role getRole()                     { return role; }
    public void setRole(Role role)            { this.role = role; }

    public LocalDateTime getCreatedAt()                     { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)       { this.createdAt = createdAt; }

    public String getStatus()                 { return status; }
    public void setStatus(String status)      { this.status = status; }
}
