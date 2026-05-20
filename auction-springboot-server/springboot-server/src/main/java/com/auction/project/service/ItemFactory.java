package com.auction.project.service;

import com.auction.project.dto.CreateAuctionRequest;
import com.auction.project.entity.*;

/**
 * Factory Method Pattern - tạo đúng loại Item dựa theo itemType trong request.
 *
 * Tương đương ItemFactory trong auction-common nhưng dùng DTO thay vì varargs.
 * Đây là ví dụ điển hình của Factory Method Pattern:
 *   - Client (AuctionService) chỉ gọi ItemFactory.create(req, ownerId)
 *   - Không biết cụ thể object nào được tạo
 *   - Dễ mở rộng thêm loại item mới mà không sửa code cũ (Open/Closed Principle)
 */
public class ItemFactory {

    /**
     * Tạo Item đúng loại theo itemType.
     *
     * @param req     request từ client chứa thông tin item
     * @param ownerId ID người bán tạo item này
     * @return Item instance đúng subtype
     */
    public static Item create(CreateAuctionRequest req, Long ownerId) {
        return switch (req.itemType().toUpperCase()) {
            case "ART" -> {
                ArtItem art = new ArtItem(
                        ownerId,
                        req.itemName(),
                        req.description(),
                        req.artist(),
                        req.yearCreated()
                );
                art.setImageBase64(req.imageBase64());
                yield art;
            }
            case "VEHICLE" -> {
                VehicleItem v = new VehicleItem(
                        ownerId,
                        req.itemName(),
                        req.description(),
                        req.vehicleBrand(),
                        req.manufactureYear(),
                        req.mileageKm()
                );
                v.setImageBase64(req.imageBase64());
                yield v;
            }
            default -> { // ELECTRONICS (default)
                ElectronicsItem e = new ElectronicsItem(
                        ownerId,
                        req.itemName(),
                        req.description(),
                        req.brand(),
                        req.warrantyMonths()
                );
                e.setImageBase64(req.imageBase64());
                yield e;
            }
        };
    }
}
