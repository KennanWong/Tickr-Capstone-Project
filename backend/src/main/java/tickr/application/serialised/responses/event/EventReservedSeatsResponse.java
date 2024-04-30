package tickr.application.serialised.responses.event;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class EventReservedSeatsResponse {
    public List<Reserved> reserved;

    static public class Reserved {
        @SerializedName("seat_number")
        public int seatNumber;

        public String section;

        public Reserved(int seatNumber, String section) {
            this.seatNumber = seatNumber;
            this.section = section;
        } 
    }

    public EventReservedSeatsResponse(List<Reserved> reserved) {
        this.reserved = reserved;
    }
    
}
