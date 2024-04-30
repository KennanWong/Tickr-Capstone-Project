package tickr.application.serialised.responses.user;

import com.google.gson.annotations.SerializedName;

public class AuthTokenResponse {
    @SerializedName("auth_token")
    public String authToken = "";

    public AuthTokenResponse () {

    }

    public AuthTokenResponse (String authToken) {
        this.authToken = authToken;
    }
}
