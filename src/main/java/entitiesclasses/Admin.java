package entitiesclasses;

public class Admin extends User {
    public Admin(int id,String username,String password){
        super(id,username,password);
    }

    @Override
    public String getInfo(){
        System.out.println("Vai tro: Quan tri vien");
        System.out.println("Ten tai khoan: "+getUsername());
    }
    public void banUser(User user){
        System.out.println("Da cam nguoi dung: "+user.getUsername());
    }
    public void monitorAuction(int auctionId){
        //quan sat phien dau gia cu the
    }

}
