package tickr.application.serialised.combined.event;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import tickr.application.serialised.SerializedLocation;
import tickr.server.exceptions.BadRequestException;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class EventSearch {
    public static class Options {
        public SerializedLocation location = null;
        @SerializedName("max_distance")
        public Double maxDistance;

        @SerializedName("start_time")
        private String startTime = null;
        @SerializedName("end_time")
        private String endTime = null;

        public List<String> tags = new ArrayList<>();
        public List<String> categories = new ArrayList<>();

        public String text = null;

        public Options () {

        }

        public Options (SerializedLocation location, Double maxDistance, ZonedDateTime startTime, ZonedDateTime endTime, List<String> tags, List<String> categories, String text) {
            this.location = location;
            this.startTime = startTime != null ? startTime.format(DateTimeFormatter.ISO_DATE_TIME) : null;
            this.endTime = endTime != null ? endTime.format(DateTimeFormatter.ISO_DATE_TIME) : null;
            this.tags = tags;
            this.categories = categories;
            this.text = text;
            this.maxDistance = maxDistance;
        }

        public ZonedDateTime getStartTime () {
            if (startTime == null) {
                return null;
            }
            try {
                return ZonedDateTime.parse(startTime, DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid start time: " + startTime, e);
            }
        }

        public ZonedDateTime getEndTime () {
            if (endTime == null) {
                return null;
            }
            try {
                return ZonedDateTime.parse(endTime, DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid end time: " + endTime, e);
            }
        }

        public String serialise () {
            return Base64.getEncoder().encodeToString(new Gson().toJson(this).getBytes());
        }
    }

    public static class Response {
        @SerializedName("event_ids")
        public List<String> eventIds;

        @SerializedName("num_results")
        public int numResults;

        public Response () {

        }

        public Response (List<String> eventIds, int numResults) {
            this.eventIds = eventIds;
            this.numResults = numResults;
        }
    }

    public static Options fromParams (String queryString) {
        try {
            return new Gson().fromJson(new String(Base64.getDecoder().decode(queryString)), Options.class);
        } catch (IllegalArgumentException | JsonSyntaxException e) {
            throw new BadRequestException("Invalid query parameter!", e);
        }
    }
}
