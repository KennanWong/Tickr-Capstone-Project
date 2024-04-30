package tickr.application.serialised.requests.group;

import com.google.gson.annotations.SerializedName;

public class GroupAcceptRequest {
    @SerializedName("auth_token")
    public String authToken;

    @SerializedName("invite_id")
    public String inviteId;

    public GroupAcceptRequest(String authToken, String inviteId) {
        this.authToken = authToken;
        this.inviteId = inviteId;
    }
    
}
