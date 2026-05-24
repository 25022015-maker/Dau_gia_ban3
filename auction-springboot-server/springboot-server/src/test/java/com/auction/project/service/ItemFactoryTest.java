package com.auction.project.service;

import com.auction.project.dto.CreateAuctionRequest;
import com.auction.project.entity.ArtItem;
import com.auction.project.entity.ElectronicsItem;
import com.auction.project.entity.Item;
import com.auction.project.entity.VehicleItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    @Test
    @DisplayName("Khởi tạo đúng đối tượng ElectronicsItem")
    void create_ElectronicsType_ReturnsElectronicsItem() {
        CreateAuctionRequest req = new CreateAuctionRequest(
                "IPhone 15 Pro", "Mô tả", null, "ELECTRONICS",
                20_000_000L, 1_000_000L, "2024-01-01T00:00:00", "2024-01-02T00:00:00", 60,
                "Apple", 12, null, null, null, null, null
        );

        Item item = ItemFactory.create(req, 1L);

        assertNotNull(item);
        assertTrue(item instanceof ElectronicsItem);
        ElectronicsItem electronics = (ElectronicsItem) item;
        assertEquals("Apple", electronics.getBrand());
        assertEquals(12, electronics.getWarrantyMonths());
        assertEquals("IPhone 15 Pro", electronics.getName());
    }

    @Test
    @DisplayName("Khởi tạo đúng đối tượng ArtItem")
    void create_ArtType_ReturnsArtItem() {
        CreateAuctionRequest req = new CreateAuctionRequest(
                "Mona Lisa", "Tranh sơn dầu", null, "ART",
                50_000_000L, 5_000_000L, "2024-01-01T00:00:00", "2024-01-02T00:00:00", 60,
                null, null, "Da Vinci", 1503, null, null, null
        );

        Item item = ItemFactory.create(req, 1L);

        assertNotNull(item);
        assertTrue(item instanceof ArtItem);
        ArtItem art = (ArtItem) item;
        assertEquals("Da Vinci", art.getArtist());
        assertEquals(1503, art.getYearCreated());
    }

    @Test
    @DisplayName("Khởi tạo đúng đối tượng VehicleItem")
    void create_VehicleType_ReturnsVehicleItem() {
        CreateAuctionRequest req = new CreateAuctionRequest(
                "Civic 2023", "Xe lướt", null, "VEHICLE",
                700_000_000L, 10_000_000L, "2024-01-01T00:00:00", "2024-01-02T00:00:00", 60,
                null, null, null, null, "Honda", 2023, 15000
        );

        Item item = ItemFactory.create(req, 1L);

        assertNotNull(item);
        assertTrue(item instanceof VehicleItem);
        VehicleItem vehicle = (VehicleItem) item;
        assertEquals("Honda", vehicle.getVehicleBrand());
        assertEquals(2023, vehicle.getManufactureYear());
        assertEquals(15000, vehicle.getMileageKm());
    }
}