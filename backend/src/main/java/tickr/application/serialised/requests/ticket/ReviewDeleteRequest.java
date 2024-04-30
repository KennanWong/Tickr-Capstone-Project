package tickr.application.serialised.requests.ticket;

import com.google.gson.annotations.SerializedName;

public class ReviewDeleteRequest {
    @SerializedName("auth_token")
    public String authToken;

    @SerializedName("comment_id")
    public String commentId;

    public ReviewDeleteRequest(String authToken, String commentId) {
        this.authToken = authToken;
        this.commentId = commentId;
    }

    
}
