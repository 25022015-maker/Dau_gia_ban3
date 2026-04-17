package com.auction.project.Common.entitiesclasses;

import com.auction.project.Common.entitiesclasses.User;
import java.util.ArrayList;
import java.util.List;

public class Seller extends User {
    private List<String> listedItems;
    private double rating;

    public Seller(String username, String password, String email) {
        super(username, password, email);
        this.listedItems = new ArrayList<>();
        this.rating = 0.0;
    }

    public void createAuction(String itemName) {
        if (isLogin()) {
            listedItems.add(itemName);
            System.out.println("Nguoi ban hang  " + getUsername() + " len hang thanh cong: " + itemName);
        } else {
            System.out.println("Phai dang nhap");
        }
    }

    public List<String> getListedItems() {
        return listedItems;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
    public String getInfo(){return "Seller: "+getUsername();}
}