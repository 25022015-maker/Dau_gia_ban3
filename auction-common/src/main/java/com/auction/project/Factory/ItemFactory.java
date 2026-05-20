package com.auction.project.Factory;

import com.auction.project.Entities.*;

public class ItemFactory {
    public static Item createItem(String type, String name, double startPrice, String... extraParams) {
        if (type == null || type.isEmpty()) return null;

        switch (type.toUpperCase()) {
            case "VEHICLE": {
                String make = (extraParams.length > 0) ? extraParams[0] : "Unknown";
                VehicleItem item = new VehicleItem(make);
                item.setName(name);
                return item;
            }
            case "ELECTRONICS": {
                String brand = (extraParams.length > 0) ? extraParams[0] : "Unknown";
                ElectronicsItem item = new ElectronicsItem(brand);
                item.setName(name);
                return item;
            }
            case "ART": {
                String artist = (extraParams.length > 0) ? extraParams[0] : "Unknown Artist";
                int year = (extraParams.length > 1) ? Integer.parseInt(extraParams[1]) : 2024;
                ArtItem item = new ArtItem(artist, year);
                item.setName(name);
                return item;
            }
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
        }
    }
}
