package tickr.application.serialised.responses.event;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class EventHostingPastResponse {
    @SerializedName("event_ids")
    public List<String> eventIds;

    @SerializedName("num_results")
    public int numResults;

    public EventHostingPastResponse(List<String> eventIds, int numResults) {
        this.eventIds = eventIds;
        this.numResults = numResults;
    }
}
