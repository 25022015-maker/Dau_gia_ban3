package com.auction.project.Common.entitiesclasses;

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

    public abstract static class Item extends Entity {
        private String name;
        private double startingPrice;

        public Item(String name, double startingPrice) {
            this.name = name;
            this.startingPrice = startingPrice;
        }

        public String getName() {return name; }
    }
}