package tickr.application.serialised.requests.group;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class GroupInviteRequest {
    @SerializedName("auth_token")
    public String authToken;

    @SerializedName("group_id")
    public String groupId;

    @SerializedName("reserve_id")
    public String reserveId;

    public String email;

    public GroupInviteRequest(String authToken, String groupId, String reserveId, String email) {
        this.authToken = authToken;
        this.groupId = groupId;
        this.reserveId = reserveId;
        this.email = email;
    }

}
