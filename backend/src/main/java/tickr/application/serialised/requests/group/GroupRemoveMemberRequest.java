package tickr.application.serialised.requests.group;

import com.google.gson.annotations.SerializedName;

public class GroupRemoveMemberRequest {
    @SerializedName("auth_token")
    public String authToken;

    @SerializedName("group_id")
    public String groupId;

    public String email;

    public GroupRemoveMemberRequest(String authToken, String groupId, String email) {
        this.authToken = authToken;
        this.groupId = groupId;
        this.email = email;
    }


}
