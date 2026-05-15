package com.example.uinew.model;
//Product newProduct = new Product(name, price, txtDescription.getText());
public class Product {
    private String name;
    private double price;
    private String description;
    private String productType;
    public Product(String name, double price, String description, String productType){
         this.name = name;
         this.price = price;
         this.description = description;
         this.productType = productType;
    }

    public String getName(){return name;}
    public double getPrice(){return price;}
    public String getDescription(){return description;}
    public String getProductType(){return productType;}
    public double setPrice(double number){return number;}
}
