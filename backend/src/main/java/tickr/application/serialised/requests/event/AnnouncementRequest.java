package tickr.application.serialised.requests.event;

import com.google.gson.annotations.SerializedName;

public class AnnouncementRequest {
    @SerializedName("auth_token")
    public String authToken;
    @SerializedName("event_id")
    public String eventId;
    @SerializedName("announcement")
    public String announcement;

    public AnnouncementRequest () {}

    public AnnouncementRequest (String authToken, String eventId, String announcement) {
        this.authToken = authToken;
        this.eventId = eventId;
        this.announcement = announcement;
    }
}
