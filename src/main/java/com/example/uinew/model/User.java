package com.example.uinew.model;

public class User { //thu nghiem
   String username;
   String password;
   public User(String username, String password){
       this.username = username;
       this.password = password;
   }

    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password){
       this.password = password;
    }
    public String getName(){return username;}
}
