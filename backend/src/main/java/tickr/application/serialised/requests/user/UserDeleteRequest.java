package tickr.application.serialised.requests.user;

import com.google.gson.annotations.SerializedName;

public class UserDeleteRequest {
    @SerializedName("auth_token")
    public String authToken = "";

    public String password;

    public boolean isValid () {
        return password != null && authToken != null;
    }

    public UserDeleteRequest () {

    }

    public UserDeleteRequest (String authToken, String password){
        this.authToken = authToken;
        this.password = password;
    }
}
