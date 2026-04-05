package entitiesclasses;

public abstract class User extends Entity{
    private String username;
    private String password;
    private String email;
    private boolean isLogin = false;
    public User(String username,String password, String email){
        this.username = username;
        this.password = password;
        this.email = email;
    }
    public boolean login(String inputusername, String inputpassword){
        if (inputusername.equals(username) && inputpassword.equals(password)){
            isLogin = true;
            System.out.println("Chao mung "+username+" quay tro lai.");
            return true;
        }else
            System.out.println("Ten dang nhap hoac mat khau khong dung.");
            return false;
    }
    public void logout(){
        isLogin = false;
        System.out.println("Nguoi dung "+username+" da dang xuat.");
    }
    public boolean isLogin(){return isLogin;}
    //tu them ham getter sau

    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getEmail() {
        return email;
    }
}
