package tickr;

import tickr.application.serialised.SerializedLocation;
import tickr.application.serialised.requests.event.CreateEventRequest;
import tickr.util.FileHelper;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CreateEventReqBuilder {
    private String authTokenString;
    private String eventName = "testing";
    private String picture = null;
    private SerializedLocation location = new SerializedLocation.Builder().build();
    private ZonedDateTime startDate = ZonedDateTime.now(ZoneId.of("UTC")).plus(Duration.ofDays(1));
    private ZonedDateTime endDate = ZonedDateTime.now(ZoneId.of("UTC")).plus(Duration.ofDays(1)).plus(Duration.ofHours(1));
    private String description = "";
    private List<CreateEventRequest.SeatingDetails> seatingDetails = new ArrayList<>();
    private Set<String> admins = new HashSet<>();
    private Set<String> categories = new HashSet<>();
    private Set<String> tags = new HashSet<>();
    private String spotifyPlaylist = null;

    public CreateEventReqBuilder withEventName (String eventName) {
        this.eventName = eventName;
        return this;
    }
    public CreateEventReqBuilder withSpotifyPlaylist (String spotifyPlaylist) {
        this.spotifyPlaylist = spotifyPlaylist;
        return this;
    }
    public CreateEventReqBuilder withPicture (String pictureLocation) {
        this.picture = FileHelper.readToDataUrl(pictureLocation);
        return this;
    }
    public CreateEventReqBuilder withLocation (SerializedLocation location) {
        this.location = location;
        return this;
    }
    public CreateEventReqBuilder withStartDate (ZonedDateTime startDate) {
        this.startDate = startDate.withZoneSameInstant(ZoneId.of("UTC"));
        return this;
    }
    public CreateEventReqBuilder withEndDate (ZonedDateTime endDate) {
        this.endDate = endDate.withZoneSameInstant(ZoneId.of("UTC"));
        return this;
    }
    public CreateEventReqBuilder withDescription (String description) {
        this.description = description;
        return this;
    }
    public CreateEventReqBuilder withSeatingDetails (List<CreateEventRequest.SeatingDetails> seatingDetails) {
        this.seatingDetails = seatingDetails;
        return this;
    }
    public CreateEventReqBuilder withAdmins (Set<String> admins) {
        this.admins = admins;
        return this;
    }
    public CreateEventReqBuilder withCategories (Set<String> categories) {
        this.categories = categories;
        return this;
    }
    public CreateEventReqBuilder withTags (Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public CreateEventRequest build (String authToken) {
        return new CreateEventRequest(authToken, eventName, picture, location, startDate.format(DateTimeFormatter.ISO_INSTANT),
                endDate.format(DateTimeFormatter.ISO_INSTANT), description, seatingDetails, admins, categories, tags, spotifyPlaylist);
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
    public ZonedDateTime getStartDate() {
        return startDate;
    }
    public void setStartDate(ZonedDateTime startDate) {
        this.startDate = startDate;
    }
    public ZonedDateTime getEndDate() {
        return endDate;
    }
    public void setEndDate(ZonedDateTime endDate) {
        this.endDate = endDate;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public List<CreateEventRequest.SeatingDetails> getSeatingDetails() {
        return seatingDetails;
    }
    public void setSeatingDetails(List<CreateEventRequest.SeatingDetails> seatingDetails) {
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

    public String getAuthTokenString() {
        return authTokenString;
    }

    public void setAuthTokenString(String authTokenString) {
        this.authTokenString = authTokenString;
    }
    
    
}
