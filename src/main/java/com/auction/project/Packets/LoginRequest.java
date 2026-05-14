package com.auction.project.Packets;

/**
 * Packet gửi từ Client lên Server khi người dùng thực hiện đăng nhập.
 *
 * <p>Server nhận packet này, kiểm tra thông tin qua {@code AuctionDAO},
 * rồi trả về {@code ResponseType.LOGIN_SUCCESS} hoặc {@code LOGIN_FAILURE}.
 */
public class LoginRequest {

  /** Loại action — luôn là "LOGIN" */
  private String action;

  /** Tên đăng nhập */
  private String username;

  /** Mật khẩu (nên hash phía client trước khi gửi trong thực tế) */
  private String password;

  // ── Constructor ───────────────────────────────────────────────────────────

  public LoginRequest() {
    this.action = "LOGIN";
  }

  public LoginRequest(String username, String password) {
    this.action = "LOGIN";
    this.username = username;
    this.password = password;
  }

  // ── Getters & Setters ─────────────────────────────────────────────────────

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}