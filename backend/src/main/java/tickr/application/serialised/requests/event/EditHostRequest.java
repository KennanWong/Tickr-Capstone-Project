package tickr.application.serialised.requests.event;

import com.google.gson.annotations.SerializedName;

public class EditHostRequest {
    @SerializedName ("auth_token")
    public String authToken;

    @SerializedName ("event_id")
    public String eventId; 

    @SerializedName ("new_host_email")
    public String newHostEmail; 

    public EditHostRequest(String authToken, String eventId, String newHostEmail) {
        this.authToken = authToken;
        this.eventId = eventId;
        this.newHostEmail = newHostEmail;
    }
}
