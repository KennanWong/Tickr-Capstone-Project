package tickr.application.serialised.requests.ticket;

import com.google.gson.annotations.SerializedName;

public class TicketViewEmailRequest {
    @SerializedName("auth_token")
    public String authToken; 

    @SerializedName("ticket_id")
    public String ticketId;

    public String email;

    public TicketViewEmailRequest(String authToken, String ticketId, String email) {
        this.authToken = authToken;
        this.ticketId = ticketId;
        this.email = email;
    }

    public boolean isValid() {
        return authToken != null && !authToken.isEmpty() && ticketId != null && !ticketId.isEmpty() 
                && email != null && !email.isEmpty();
    }
}
