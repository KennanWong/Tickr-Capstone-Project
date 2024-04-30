package tickr.application.serialised.combined.tickets;

import com.google.gson.annotations.SerializedName;
import tickr.server.exceptions.BadRequestException;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class TicketReserve {
    public static class Request {
        @SerializedName("auth_token")
        public String authToken = null;

        @SerializedName("event_id")
        public String eventId = null;

        @SerializedName("ticket_datetime")
        public String ticketDateTime = null;

        @SerializedName("ticket_details")
        public List<TicketDetails> ticketDetails = null;

        public Request () {

        }

        public Request (String authToken, String eventId, String ticketDateTime, List<TicketDetails> ticketDetails) {
            this.authToken = authToken;
            this.eventId = eventId;
            this.ticketDateTime = ticketDateTime;
            this.ticketDetails = ticketDetails;
        }

        public Request (String authToken, String eventId, ZonedDateTime ticketDateTime, List<TicketDetails> ticketDetails) {
            this(authToken, eventId, ticketDateTime.format(DateTimeFormatter.ISO_DATE_TIME), ticketDetails);
        }

        public ZonedDateTime getTicketTime () {
            try {
                return ZonedDateTime.parse(ticketDateTime, DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid datetime!", e);
            }
        }
    }

    public static class Response {
        @SerializedName("reserve_tickets")
        public List<ReserveDetails> reserveTickets;

        public Response () {

        }

        public Response (List<ReserveDetails> reserveTickets) {
            this.reserveTickets = reserveTickets;
        }
    }

    public static class TicketDetails {
        public String section;
        public int quantity;
        @SerializedName("seat_numbers")
        public List<Integer> seatNums;


        public TicketDetails () {

        }

        public TicketDetails (String section, int quantity, List<Integer> seatNums) {
            this.section = section;
            this.quantity = quantity;
            this.seatNums = seatNums;
        }
    }

    public static class ReserveDetails {
        @SerializedName("reserve_id")
        public String reserveId;

        @SerializedName("seat_number")
        public int seatNum;
        public String section;
        public float price;

        public ReserveDetails () {

        }

        public ReserveDetails (String reserveId, int seatNum, String section, float price) {
            this.reserveId = reserveId;
            this.seatNum = seatNum;
            this.section = section;
            this.price = price;
        }

        public String getReserveId() {
            return reserveId;
        }

        public void setReserveId(String reserveId) {
            this.reserveId = reserveId;
        }

        
    }
}
