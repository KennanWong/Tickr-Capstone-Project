package tickr.application.serialised.responses.user;

import com.google.gson.annotations.SerializedName;

public class UserIdResponse {
    @SerializedName("user_id")
    public String userId;

    public UserIdResponse () {

    }

    public UserIdResponse (String userId) {
        this.userId = userId;
    }
}
