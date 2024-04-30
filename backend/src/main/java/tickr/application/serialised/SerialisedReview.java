package tickr.application.serialised;

import java.util.List;

public class SerialisedReview {
    public String reviewId;
    public String authorId;
    public String title;
    public String text;
    public float rating;
    public List<SerialisedReaction> reactions;

    public SerialisedReview () {}

    public SerialisedReview (String reviewId, String authorId, String title, String text, float rating, List<SerialisedReaction> reactions) {
        this.reviewId = reviewId;
        this.authorId = authorId;
        this.title = title;
        this.text = text;
        this.rating = rating;
        this.reactions = reactions;
    }
}
