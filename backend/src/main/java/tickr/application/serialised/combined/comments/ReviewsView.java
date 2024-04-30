package tickr.application.serialised.combined.comments;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ReviewsView {
    public static class Request {
        @SerializedName("auth_token")
        public String authToken;
        @SerializedName("event_id")
        public String eventId;

        @SerializedName("page_start")
        public int pageStart;
        @SerializedName("max_results")
        public int maxResults;

        public Request () {}

        public Request (String authToken, String eventId, int pageStart, int maxResults) {
            this.authToken = authToken;
            this.eventId = eventId;
            this.pageStart = pageStart;
            this.maxResults = maxResults;
        }
    }

    public static class Response {
        public List<Review> reviews;
        @SerializedName("num_results")
        public int numResults;

        public Response () {}

        public Response (List<Review> reviews, int numResults) {
            this.reviews = reviews;
            this.numResults = numResults;
        }
    }

    public static class Review {
        public String reviewId;
        public String authorId;
        public String title;
        public String text;
        public float rating;
        List<Reaction> reactions;

        public Review () {}

        public Review (String reviewId, String authorId, String title, String text, float rating, List<Reaction> reactions) {
            this.reviewId = reviewId;
            this.authorId = authorId;
            this.title = title;
            this.text = text;
            this.rating = rating;
            this.reactions = reactions;
        }
    }

    public static class Reaction {
        @SerializedName("react_type")
        public String reactType;
        @SerializedName("react_num")
        public int reactNum;

        public Reaction () {}

        public Reaction (String reactType, int reactNum) {
            this.reactType = reactType;
            this.reactNum = reactNum;
        }
    }
}
