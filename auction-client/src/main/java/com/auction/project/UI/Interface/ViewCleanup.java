package com.auction.project.UI.Interface;

public interface ViewCleanup {
    /**
     * Hàm này sẽ được gọi tự động trước khi màn hình bị đóng/chuyển đi
     * để gỡ bỏ các Listener và dừng các Timer đang chạy ngầm.
     */
    void cleanup();
}