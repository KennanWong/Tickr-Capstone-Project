package tickr.application.serialised.responses.ticket;

import org.checkerframework.checker.index.qual.SearchIndexBottom;

import com.google.gson.annotations.SerializedName;

public class TicketViewResponse {
    @SerializedName("event_id")
    public String eventId; 

    @SerializedName("user_id")
    public String userId;

    @SerializedName("seat_num")
    public int seatNum;

    public String section;

    @SerializedName("first_name")
    public String firstName;

    @SerializedName("last_name")
    public String lastName;

    public String email;

    @SerializedName("group_id")
    public String groupId;

    public TicketViewResponse(String eventId, String userId, String section, int seatNum, String firstName, String lastName, String email, String groupId) {
        this.eventId = eventId;
        this.userId = userId;
        this.seatNum = seatNum;
        this.section = section;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.groupId = groupId;
    } 
}
