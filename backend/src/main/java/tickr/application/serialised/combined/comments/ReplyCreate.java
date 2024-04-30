package tickr.application.serialised.combined.comments;

import com.google.gson.annotations.SerializedName;

public class ReplyCreate {
    public static class Request {
        @SerializedName("auth_token")
        public String authToken;
        @SerializedName("review_id")
        public String reviewId;
        public String reply;

        public Request () {}
        public Request (String authToken, String reviewId, String reply) {
            this.authToken = authToken;
            this.reviewId = reviewId;
            this.reply = reply;
        }
    }

    public static class Response {
        @SerializedName("reply_id")
        public String replyId;

        public Response () {}

        public Response (String replyId) {
            this.replyId = replyId;
        }
    }
}
