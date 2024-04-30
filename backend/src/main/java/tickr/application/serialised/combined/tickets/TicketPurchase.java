package tickr.application.serialised.combined.tickets;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TicketPurchase {
    public static class Request {
        @SerializedName("auth_token")
        public String authToken;

        @SerializedName("ticket_details")
        public List<TicketDetails> ticketDetails;

        @SerializedName("success_url")
        public String successUrl;

        @SerializedName("cancel_url")
        public String cancelUrl;

        public Request () {

        }

        public Request (String authToken, String successUrl, String cancelUrl, List<TicketDetails> ticketDetails) {
            this.authToken = authToken;
            this.ticketDetails = ticketDetails;
            this.successUrl = successUrl;
            this.cancelUrl = cancelUrl;
        }
    }

    public static class Response {
        @SerializedName("redirect_url")
        public String redirectUrl;

        public Response () {}

        public Response (String redirectUrl) {
            this.redirectUrl = redirectUrl;
        }
    }

    public static class TicketDetails {
        @SerializedName("request_id")
        public String requestId;

        @SerializedName("first_name")
        public String firstName = null;
        @SerializedName("last_name")
        public String lastName = null;
        public String email = null;

        public TicketDetails () {

        }

        public TicketDetails (String requestId) {
            this.requestId = requestId;
        }

        public TicketDetails (String requestId, String firstName, String lastName, String email) {
            this.requestId = requestId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }
    }
}
