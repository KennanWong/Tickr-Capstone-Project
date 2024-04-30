package tickr.application.serialised.responses.event;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RecommenderResponse {
    public List<Event> events;
    @SerializedName("num_results")
    public int numResults;

    public RecommenderResponse () {

    }

    public RecommenderResponse (List<Event> events, int numResults) {
        this.events = events;
        this.numResults = numResults;
    }

    public static class Event {
        public String id;
        @SerializedName("recommend_value")
        public double recommendValue;

        public Event () {

        }

        public Event (String id, double recommendValue) {
            this.id = id;
            this.recommendValue = recommendValue;
        }
    }
}
