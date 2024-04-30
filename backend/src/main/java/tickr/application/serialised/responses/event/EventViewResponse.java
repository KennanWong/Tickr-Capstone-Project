package tickr.application.serialised.responses.event;

import java.util.List;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import tickr.application.serialised.SerializedLocation;

public class EventViewResponse {
    public String host_id; 

    @SerializedName("event_name")
    public String eventName; 

    public String picture;

    public SerializedLocation location; 

    @SerializedName("start_date")
    public String startDate;

    @SerializedName("end_date")
    public String endDate;

    public String description; 

    @SerializedName("seating_details")
    public List<SeatingDetails> seatingDetails; 

    public Set<String> admins; 

    public Set<String> categories;

    public Set<String> tags;

    public boolean published;

    @SerializedName("seat_availability")
    public int seatAvailability = 0;

    @SerializedName("seat_capacity")
    public int seatCapacity = 0;

    @SerializedName("spotify_playlist")
    public String spotifyPlaylist;

    public static class SeatingDetails {
        public String section;

        @SerializedName("ticket_price") 
        public float ticketPrice; 

        @SerializedName("available_seats")
        public int availableSeats;

        @SerializedName("total_seats")
        public int totalSeats; 

        @SerializedName("has_seats")
        public boolean hasSeats;
        
        public SeatingDetails(String section, int availableSeats, float cost, int totalSeats, boolean hasSeats) {
            this.section = section;
            this.ticketPrice = cost;
            this.availableSeats = availableSeats;
            this.totalSeats = totalSeats;
            this.hasSeats = hasSeats;
        }
    }

    public EventViewResponse () {}

    public EventViewResponse(String host_id, String eventName, String picture, SerializedLocation location, String startDate,
            String endDate, String description, List<SeatingDetails> seatingDetails, Set<String> admins,
            Set<String> categories, Set<String> tags, boolean published, int seatAvailability, int seatCapacity, String spotifyPlaylist) {
        this.eventName = eventName;
        this.picture = picture;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.seatingDetails = seatingDetails;
        this.admins = admins;
        this.categories = categories;
        this.tags = tags;
        this.host_id = host_id;
        this.published = published;
        this.seatAvailability = seatAvailability;
        this.seatCapacity = seatCapacity;
        this.spotifyPlaylist = spotifyPlaylist;
    }

    
}
