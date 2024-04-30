package tickr.application.serialised.responses.event;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class EventHostingFutureResponse {
    @SerializedName("event_ids")
    public List<String> eventIds;

    @SerializedName("num_results")
    public int numResults;

    public EventHostingFutureResponse(List<String> eventIds, int numResults) {
        this.eventIds = eventIds;
        this.numResults = numResults;
    }
}
