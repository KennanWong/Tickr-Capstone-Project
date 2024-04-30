package tickr.application.serialised.responses.event;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class EventAttendeesResponse {
    public List<Attendee> attendees; 

    static public class Attendee {
        @SerializedName("user_id")
        public String userId;
        
        public List<String> tickets = new ArrayList<String>();

        public Attendee(String userId, List<String> ticketsIds) {
            this.userId = userId;
            this.tickets = ticketsIds;
        }    

        public Attendee (String userId) {
            this.userId = userId;
        }

        public void addTicketId(String ticket) {
            tickets.add(ticket);
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public List<String> getTickets() {
            return tickets;
        }

        public void setTickets(List<String> tickets) {
            this.tickets = tickets;
        }

        
    }

    public EventAttendeesResponse(List<Attendee> attendees) {
        this.attendees = attendees;
    }

    public List<Attendee> getAttendees() {
        return attendees;
    }

    public void setAttendees(List<Attendee> attendees) {
        this.attendees = attendees;
    }

}
