package com.auction.project.entitiesclasses;

public class Admin extends User {
    public Admin(String id,String username,String password){
        super(id,username,password);
    }

    @Override
    public String getInfo(){
        return "Admin: "+ getUsername() +" - id:  "+getId();
    }
    public void banUser(User user){
        System.out.println("Da cam nguoi dung: "+user.getUsername());
    }
    public void monitorAuction(int auctionId){
        //quan sat phien dau gia cu the
    }

}
