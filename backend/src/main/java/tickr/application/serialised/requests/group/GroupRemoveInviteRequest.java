package tickr.application.serialised.requests.group;

import com.google.gson.annotations.SerializedName;

public class GroupRemoveInviteRequest {
    @SerializedName("auth_token")
    public String authToken;

    @SerializedName("invite_id")
    public String inviteId;

    @SerializedName("group_id")
    public String groupId;

    public GroupRemoveInviteRequest(String authToken, String inviteId, String groupId) {
        this.authToken = authToken;
        this.inviteId = inviteId;
        this.groupId = groupId;
    }
}
