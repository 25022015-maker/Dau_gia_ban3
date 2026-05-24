package com.auction.project.service;

import com.auction.project.dto.*;
import com.auction.project.entity.enums.Role;
import com.auction.project.repository.AuctionRepository;
import com.auction.project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test") // Đọc cấu hình từ file application-test.properties
class AuctionSystemIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    private String sellerToken;
    private String bidderToken;
    private Long bidderId;

    @BeforeEach
    void setUp() {
        // Dọn sạch database thử nghiệm trước mỗi lượt test
        auctionRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Đăng ký tài khoản Seller
        RegisterRequest sellerReg = new RegisterRequest("seller01", "password123", "seller@test.com", "SELLER");
        ResponseEntity<AuthResponse> sellerRes = restTemplate.postForEntity("/api/auth/register", sellerReg, AuthResponse.class);
        sellerToken = sellerRes.getBody().token();

        // 2. Đăng ký tài khoản Bidder
        RegisterRequest bidderReg = new RegisterRequest("bidder01", "password123", "bidder@test.com", "BIDDER");
        ResponseEntity<AuthResponse> bidderRes = restTemplate.postForEntity("/api/auth/register", bidderReg, AuthResponse.class);
        bidderToken = bidderRes.getBody().token();
        bidderId = bidderRes.getBody().userId();
    }

    @Test
    @DisplayName("Hệ thống đấu giá trực tuyến E2E: Tạo phiên -> Subscribe WebSocket -> Nạp tiền -> Đặt giá -> Xác thực cập nhật thời gian thực")
    void testFullAuctionFlowAndWebSocketNotification() throws Exception {

        // BƯỚC 1: Seller tạo một phiên đấu giá mới qua API (Truyền đủ 16 tham số của record)
        CreateAuctionRequest createRequest = new CreateAuctionRequest(
                "Macbook Pro M3",                              // 1. itemName
                "Laptop mới 100%",                             // 2. description
                null,                                          // 3. imageBase64
                "ELECTRONICS",                                 // 4. itemType
                10_000_000L,                                   // 5. startPrice
                1_000_000L,                                    // 6. minBid
                LocalDateTime.now().minusMinutes(1).toString(),// 7. startTime
                LocalDateTime.now().plusMinutes(5).toString(), // 8. endTime
                60,                                            // 9. antiSnipeExtSeconds

                // Thuộc tính của Electronics
                "Apple",                                       // 10. brand
                12,                                            // 11. warrantyMonths

                // Các thuộc tính của Art và Vehicle không dùng đến (truyền null)
                null,                                          // 12. artist
                null,                                          // 13. yearCreated
                null,                                          // 14. vehicleBrand
                null,                                          // 15. manufactureYear
                null                                           // 16. mileageKm
        );


        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(sellerToken);
        HttpEntity<CreateAuctionRequest> entity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<AuctionResponse> createResponse = restTemplate.postForEntity(
                "/api/auctions", entity, AuctionResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long auctionId = createResponse.getBody().id();
        assertThat(auctionId).isNotNull();

        // BƯỚC 2: Thiết lập kết nối WebSocket (STOMP Client) giả lập Bidder lắng nghe thay đổi
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // Sử dụng BlockingQueue để hứng dữ liệu bất đồng bộ trả về từ WebSocket
        BlockingQueue<BidUpdateMessage> webSocketQueue = new LinkedBlockingDeque<>();

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                // Subscribe vào topic của phiên đấu giá vừa tạo
                session.subscribe("/topic/auction/" + auctionId, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return BidUpdateMessage.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        webSocketQueue.add((BidUpdateMessage) payload);
                    }
                });
            }
        };

        // Tiến hành kết nối WebSocket đến server
        String wsUrl = "ws://localhost:" + port + "/ws";
        StompSession stompSession = stompClient.connectAsync(wsUrl, sessionHandler).get(5, TimeUnit.SECONDS);
        assertThat(stompSession.isConnected()).isTrue();

        // BƯỚC 3: Người mua nạp 20 triệu vào tài khoản để đủ điều kiện đấu giá
        HttpHeaders bidderHeaders = new HttpHeaders();
        bidderHeaders.setBearerAuth(bidderToken);
        HttpEntity<Map<String, Long>> topUpEntity = new HttpEntity<>(Map.of("amount", 20_000_000L), bidderHeaders);

        ResponseEntity<UserProfileResponse> topUpRes = restTemplate.postForEntity(
                "/api/users/me/top-up", topUpEntity, UserProfileResponse.class);
        assertThat(topUpRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(topUpRes.getBody().balance()).isEqualTo(20_000_000L);

        // BƯỚC 4: Người mua tiến hành đặt giá 12 triệu (Hợp lệ vì Giá sàn 10tr + Bước giá tối thiểu 1tr)
        PlaceBidRequest bidRequest = new PlaceBidRequest(auctionId, 12_000_000L);
        HttpEntity<PlaceBidRequest> bidEntity = new HttpEntity<>(bidRequest, bidderHeaders);

        ResponseEntity<AuctionResponse> bidResponse = restTemplate.postForEntity(
                "/api/auctions/" + auctionId + "/bid", bidEntity, AuctionResponse.class);

        // Xác thực kết quả trả về từ REST API đặt giá
        assertThat(bidResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bidResponse.getBody().currentPrice()).isEqualTo(12_000_000L);
        assertThat(bidResponse.getBody().currentWinnerUsername()).isEqualTo("bidder01");

        // BƯỚC 5: Xác thực WebSocket đã nhận tin nhắn cập nhật giá theo thời gian thực
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            BidUpdateMessage wsMessage = webSocketQueue.poll();
            assertThat(wsMessage).isNotNull();
            assertThat(wsMessage.auctionId()).isEqualTo(auctionId);
            assertThat(wsMessage.newPrice()).isEqualTo(12_000_000L);
            assertThat(wsMessage.winnerUsername()).isEqualTo("bidder01");
            assertThat(wsMessage.winnerId()).isEqualTo(bidderId);
            assertThat(wsMessage.status()).isEqualTo("RUNNING");
        });

        // Đóng kết nối sau khi test xong
        stompSession.disconnect();
    }
}