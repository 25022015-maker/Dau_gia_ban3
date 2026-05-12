package com.auction.project.Server;

import com.auction.project.Observer.Observer;
import com.auction.project.Packets.Response;
import com.auction.project.Packets.ResponseType;

/**
 * Adapter Pattern — cầu nối giữa Observer interface của nhóm và Socket.
 *
 * Vấn đề:
 *   - Nhóm định nghĩa Observer.update(int auctionId, double price, String bidder)
 *   - Khi Auction.notifyObservers() gọi, cần gửi data qua Socket về client
 *   - ClientHandler giữ Socket, nhưng Observer không biết gì về Socket
 *
 * Giải pháp:
 *   ClientHandlerObserver implement Observer của nhóm,
 *   bên trong giữ reference tới ClientHandler để gửi socket.
 *
 * Luồng:
 *   Auction.placeBid()
 *     → notifyObservers()
 *         → ClientHandlerObserver.update(auctionId, price, bidder)  ← đây
 *             → handler.sendResponse(BID_UPDATE JSON)
 *                 → Socket → Client nhận realtime
 */
public class ClientHandlerObserver implements Observer {

    private final ClientHandler handler;

    public ClientHandlerObserver(ClientHandler handler) {
        this.handler = handler;
    }

    /**
     * Được Auction.notifyObservers() gọi mỗi khi có bid mới hợp lệ.
     * Signature khớp chính xác với Observer interface của nhóm.
     *
     * @param auctionId ID phiên vừa có bid mới (int — khớp với Entity.id)
     * @param newPrice  giá mới cao nhất
     * @param bidder    username người vừa thắng
     */
    @Override
    public void update(int auctionId, double newPrice, String bidder) {
        // Đóng gói thành BidUpdateData để Gson serialize thành JSON
        BidUpdateData data = new BidUpdateData(auctionId, newPrice, bidder);

        Response response = new Response(
                ResponseType.BID_UPDATE,
                String.format("💰 %s vừa đặt %.0f VNĐ", bidder, newPrice),
                data
        );

        // Gửi về client — sendResponse() đã synchronized
        handler.sendResponse(response);
    }

    /** @return ClientHandler tương ứng — dùng để unregister khi client disconnect */
    public ClientHandler getHandler() {
        return handler;
    }

    // ── Payload gửi kèm BID_UPDATE ────────────────────────────────────────────

    /**
     * Data object Gson serialize thành JSON, gửi về client.
     * Client parse để cập nhật giá trên UI.
     */
    public static class BidUpdateData {
        private final int auctionId;
        private final double currentPrice;
        private final String leadingBidder;

        public BidUpdateData(int auctionId, double currentPrice, String leadingBidder) {
            this.auctionId = auctionId;
            this.currentPrice = currentPrice;
            this.leadingBidder = leadingBidder;
        }

        public int getAuctionId()        { return auctionId; }
        public double getCurrentPrice()  { return currentPrice; }
        public String getLeadingBidder() { return leadingBidder; }
    }
}