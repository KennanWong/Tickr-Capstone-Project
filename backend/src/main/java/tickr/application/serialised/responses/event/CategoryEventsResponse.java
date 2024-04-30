package tickr.application.serialised.responses.event;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CategoryEventsResponse {
    @SerializedName("event_ids")
    public List<String> eventIds;

    @SerializedName("num_results")
    public int numResults;

    public CategoryEventsResponse () {}

    public CategoryEventsResponse (List<String> eventIds, int numResults) {
        this.eventIds = eventIds;
        this.numResults = numResults;
    }
}
