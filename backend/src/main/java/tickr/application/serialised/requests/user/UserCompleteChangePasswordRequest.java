package tickr.application.serialised.requests.user;

import com.google.gson.annotations.SerializedName;
 
public class UserCompleteChangePasswordRequest {
    @SerializedName("reset_token")
    public String resetToken = "";
 
    @SerializedName("email")
    public String email;
 
    @SerializedName("new_password")
    public String newPassword;
 
    public boolean isValid () {
        return newPassword != null && email != null && resetToken != null;
    }
 
    public UserCompleteChangePasswordRequest () {

    }
 
    public UserCompleteChangePasswordRequest (String email, String newPassword, String resetToken){
        this.resetToken = resetToken;
        this.newPassword = newPassword;
        this.email = email;
    }
}