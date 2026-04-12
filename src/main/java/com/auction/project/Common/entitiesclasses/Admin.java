package com.auction.project.Common.entitiesclasses;

public class Admin extends User {
    private Admin(String id,String username,String password){
        super(id,username,password);
    }

    Admin(){}

    @Override
    public String getInfo(){
        return "Admin: "+ getUsername() +" - id:  "+getId();
    }
    public void banUser(User user){
        System.out.println("Da cam nguoi dung: "+user.getUsername());
    }

    private void monitorAuction(int auctionId){ //ngăn mod kiểm soát phiên
        //quan sat phien dau gia cu the
    }

}
