package com.auction.project.Factory;

import com.auction.project.Entities.Electronics;
import com.auction.project.Entities.Item;

public class ItemFactory {
    public static Item createItem(String type, String id, String name, double price, int extraParam) {
        switch (type.toLowerCase()) {
            case "electronics":
                return new Electronics(id, name, price, extraParam);
            default:
                throw new IllegalArgumentException("Unknown item type");
        }
    }
}