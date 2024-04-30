package tickr.application.serialised.responses.comment;

import com.google.gson.annotations.SerializedName;
import tickr.application.serialised.SerialisedReply;

import java.util.List;

public class RepliesViewResponse {
    public List<SerialisedReply> replies;
    @SerializedName("num_results")
    public int numResults;

    public RepliesViewResponse () {}

    public RepliesViewResponse (List<SerialisedReply> replies, int numResults) {
        this.replies = replies;
        this.numResults = numResults;
    }
}
