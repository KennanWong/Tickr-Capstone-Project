package tickr.application.serialised.requests.event;

import com.google.gson.annotations.SerializedName;

public class EventDeleteRequest {
    @SerializedName("auth_token")
    public String authToken; 

    @SerializedName("event_id")
    public String eventId;

    public EventDeleteRequest(String authToken, String eventId) {
        this.authToken = authToken;
        this.eventId = eventId;
    }

    public boolean isValid () {
        return authToken != null && !authToken.isEmpty() && eventId != null && !eventId.isEmpty();
    }
}
