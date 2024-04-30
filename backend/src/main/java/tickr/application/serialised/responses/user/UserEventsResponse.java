package tickr.application.serialised.responses.user;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class UserEventsResponse {
    public List<String> eventIds;

    @SerializedName("num_results")
    public int numResults;

    public UserEventsResponse(List<String> eventIds, int numResults) {
        this.eventIds = eventIds;
        this.numResults = numResults;
    }

    public List<String> getEventIds() {
        return eventIds;
    }

    public void setEventIds(List<String> eventIds) {
        this.eventIds = eventIds;
    }

    public int getNumResults() {
        return numResults;
    }

    public void setNumResults(int numResults) {
        this.numResults = numResults;
    }

    
}
