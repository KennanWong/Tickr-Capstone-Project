package tickr.application.serialised.requests.comment;

import com.google.gson.annotations.SerializedName;

public class ReactRequest {
    @SerializedName("auth_token")
    public String authToken;
    @SerializedName("comment_id")
    public String commentId;
    @SerializedName("react_type")
    public String reactType;

    public ReactRequest () {}

    public ReactRequest (String authToken, String commentId, String reactType) {
        this.authToken = authToken;
        this.commentId = commentId;
        this.reactType = reactType;
    }
}
