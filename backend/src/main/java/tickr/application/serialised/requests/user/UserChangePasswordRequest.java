package tickr.application.serialised.requests.user;
import com.google.gson.annotations.SerializedName;

public class UserChangePasswordRequest {
    @SerializedName("auth_token")
    public String authToken = "";

    public String password;

    @SerializedName("new_password")
    public String newPassword;

    public boolean isValid () {
        return password != null && newPassword != null && authToken != null;
    }

    public UserChangePasswordRequest () {

    }

    public UserChangePasswordRequest (String password, String newPassword, String authToken){
        this.authToken = authToken;
        this.password = password;
        this.newPassword = newPassword;
    }
}
