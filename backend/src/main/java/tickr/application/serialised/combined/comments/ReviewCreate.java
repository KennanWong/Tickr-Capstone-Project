package tickr.application.serialised.combined.comments;

import com.google.gson.annotations.SerializedName;

public class ReviewCreate {
    public static class Request {
        @SerializedName("auth_token")
        public String authToken;
        @SerializedName("event_id")
        public String eventId;

        public String title;
        public String text;
        public float rating;
        //String parentId

        public Request () {}

        public Request (String authToken, String eventId, String title, String text, float rating) {
            this.authToken = authToken;
            this.eventId = eventId;
            this.title = title;
            this.text = text;
            this.rating = rating;
        }
    }

    public static class Response {
        @SerializedName("review_id")
        public String reviewId;

        public Response () {}

        public Response (String reviewId) {
            this.reviewId = reviewId;
        }
    }
}
