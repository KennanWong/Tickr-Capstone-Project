package tickr.application.serialised.requests.ticket;

import com.google.gson.annotations.SerializedName;

public class TicketRefundRequest {
    @SerializedName("auth_token")
    public String authToken;
    @SerializedName("ticket_id")
    public String ticketId;

    public TicketRefundRequest () {

    }

    public TicketRefundRequest (String authToken, String ticketId) {
        this.authToken = authToken;
        this.ticketId = ticketId;
    }
}
