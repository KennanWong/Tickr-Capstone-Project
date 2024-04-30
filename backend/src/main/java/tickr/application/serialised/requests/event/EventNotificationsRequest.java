package tickr.application.serialised.requests.event;

import com.google.gson.annotations.SerializedName;

public class EventNotificationsRequest {
    @SerializedName("auth_token")
    public String authToken;
    @SerializedName("event_id")
    public String eventId;

    public EventNotificationsRequest () {}

    public EventNotificationsRequest (String authToken, String eventId) {
        this.authToken = authToken;
        this.eventId = eventId;
    }
}
