package tickr.application.serialised.requests.user;

import com.google.gson.annotations.SerializedName;

public class UserLogoutRequest {
    @SerializedName("auth_token")
    public String authToken;

    public UserLogoutRequest () {}

    public UserLogoutRequest (String authToken) {
        this.authToken = authToken;
    }
}
