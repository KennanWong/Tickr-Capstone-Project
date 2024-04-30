package tickr.application.serialised.requests.event;

import java.util.List;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import tickr.application.serialised.SerializedLocation;

public class EditEventRequest {
    @SerializedName("event_id")
    public String eventId;

    @SerializedName("auth_token")
    public String authToken; 

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

    @SerializedName("spotify_playlist")
    public String spotifyPlaylist;

    public static class SeatingDetails {
        public String section;

        public int availability; 

        @SerializedName("ticket_price") 
        public float ticketPrice; 

        @SerializedName("has_seats")
        public boolean hasSeats;

        public SeatingDetails(String section, int availability, float cost, boolean hasSeats) {
            this.section = section;
            this.availability = availability;
            this.ticketPrice = cost;
            this.hasSeats = hasSeats;
        }
    }
    public EditEventRequest () {}

    public EditEventRequest(String eventId, String authToken, String eventName, String picture,
            SerializedLocation location, String startDate, String endDate, String description,
            List<SeatingDetails> seatingDetails, Set<String> admins, Set<String> categories, Set<String> tags, boolean published, String spotifyPlaylist) {
        this.eventId = eventId;
        this.authToken = authToken;
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
        this.published = published;
        this.spotifyPlaylist = spotifyPlaylist;
    }

    public boolean isSeatingDetailsValid() {
        if (seatingDetails != null) {
            for (SeatingDetails seats : seatingDetails) {
                if (seats.section == null || seats.section.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getSeatCapacity() {
        if (seatingDetails == null) {
            return 0;
        }
        int count = 0;
        for (SeatingDetails details : seatingDetails) {
            count += details.availability;
        }
        return count; 
    }

    public String getEventId() {
        return eventId;
    }
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    public String getAuthToken() {
        return authToken;
    }
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
    public String getEventName() {
        return eventName;
    }
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
    public String getPicture() {
        return picture;
    }
    public void setPicture(String picture) {
        this.picture = picture;
    }
    public SerializedLocation getLocation() {
        return location;
    }
    public void setLocation(SerializedLocation location) {
        this.location = location;
    }
    public String getStartDate() {
        return startDate;
    }
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
    public String getEndDate() {
        return endDate;
    }
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public List<SeatingDetails> getSeatingDetails() {
        return seatingDetails;
    }
    public void setSeatingDetails(List<SeatingDetails> seatingDetails) {
        this.seatingDetails = seatingDetails;
    }
    public Set<String> getAdmins() {
        return admins;
    }
    public void setAdmins(Set<String> admins) {
        this.admins = admins;
    }
    public Set<String> getCategories() {
        return categories;
    }
    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }
    public Set<String> getTags() {
        return tags;
    }
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    
}
