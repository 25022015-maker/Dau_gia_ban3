package com.example.uinew.model;
//Product newProduct = new Product(name, price, txtDescription.getText());
public class Product {
    public String name;
    public double price;
    public String description;
    public String productType;
    public Product(String name, double price, String description, String productType){
         this.name = name;
         this.price = price;
         this.description = description;
         this.productType = productType;
    }
}
