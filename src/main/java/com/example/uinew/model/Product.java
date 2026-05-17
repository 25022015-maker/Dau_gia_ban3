package com.example.uinew.model;
//Item newItem = new Item(name, price, txtDescription.getText());
public class Item {
    private String name;
    private double price;
    private String description;
    private String ItemType;
    public Item(String name, double price, String description, String ItemType){
         this.name = name;
         this.price = price;
         this.description = description;
         this.ItemType = ItemType;
    }

    public String getName(){return name;}
    public double getPrice(){return price;}
    public String getDescription(){return description;}
    public String getItemType(){return ItemType;}
    public double setPrice(double number){return number;}
}
