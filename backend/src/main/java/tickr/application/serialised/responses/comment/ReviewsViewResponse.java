package tickr.application.serialised.responses.comment;

import com.google.gson.annotations.SerializedName;
import tickr.application.serialised.SerialisedReview;

import java.util.List;

public class ReviewsViewResponse {
    public List<SerialisedReview> reviews;
    @SerializedName("num_results")
    public int numResults;

    public ReviewsViewResponse () {}

    public ReviewsViewResponse (List<SerialisedReview> reviews, int numResults) {
        this.reviews = reviews;
        this.numResults = numResults;
    }
}
