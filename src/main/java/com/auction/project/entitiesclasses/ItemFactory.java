package com.auction.project.entitiesclasses;

import org.controlsfx.control.PropertySheet;

public class ItemFactory {

    public static PropertySheet.Item createItem(String type, String id, String name, double price) {
        if (type == null) return null;

        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                return new Electronics(id, name, price);
            case "ART":
                return new Art(id, name, price);
            case "VEHICLE":
                return new Vehicle(id, name, price);
            default:
                throw new IllegalArgumentException("Unknown item type: " + type);
        }
    }
}