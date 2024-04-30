package tickr.application.serialised.responses.event;

import java.util.List;

import javax.servlet.annotation.ServletSecurity;

import com.google.gson.annotations.SerializedName;

public class EventHostingsResponse {
    public List<String> eventIds; 

    @SerializedName("num_results")
    public int numResults;

    public EventHostingsResponse(List<String> eventIds, int numResults) {
        this.eventIds = eventIds;
        this.numResults = numResults;
    }

    
}
