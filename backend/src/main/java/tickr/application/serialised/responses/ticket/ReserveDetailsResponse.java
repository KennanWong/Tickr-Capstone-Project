package tickr.application.serialised.responses.ticket;

import com.google.gson.annotations.SerializedName;

public class ReserveDetailsResponse {
    public String section;

    @SerializedName("seat_number")
    public int seatNum;

    public float price;

    @SerializedName("event_id")
    public String eventId;

    public ReserveDetailsResponse(String section, int seatNum, float price, String eventId) {
        this.section = section;
        this.seatNum = seatNum;
        this.price = price;
        this.eventId = eventId;
    }
}
