package tickr.application.serialised.requests.event;

import com.google.gson.annotations.SerializedName;

public class EventNotificationsUpdateRequest {
    @SerializedName("auth_token")
    public String authToken;
    @SerializedName("event_id")
    public String eventId;

    public boolean notifications;

    public EventNotificationsUpdateRequest () {}

    public EventNotificationsUpdateRequest (String authToken, String eventId, boolean notifications) {
        this.authToken = authToken;
        this.eventId = eventId;
        this.notifications = notifications;
    }
}
