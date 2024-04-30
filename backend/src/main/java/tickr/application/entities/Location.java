package tickr.application.entities;

import jakarta.persistence.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.apis.location.LocationPoint;
import tickr.application.apis.location.LocationRequest;
import tickr.application.serialised.SerializedLocation;

import java.text.DecimalFormat;
import java.util.UUID;

@Entity
@Table(name = "locations")
public class Location {
    static Logger logger = LogManager.getLogger();
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "street_no")
    private int streetNo;

    @Column(name = "street_name")
    private String streetName;

    private String suburb;

    @Column(name = "unit_no")
    private String unitNo;

    private String postcode;

    private String state;

    private String country;

    // Longitude in radians
    private Double longitude;
    // Latitude in radians
    private Double latitude;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "location")
    private Event event;

    public Location() {}

    public Location(int streetNo, String streetName, String unitNo, String postcode, String suburb, String state, String country) {
        this.streetNo = streetNo;
        this.streetName = streetName;
        this.unitNo = unitNo;
        this.postcode = postcode;
        this.state = state;
        this.country = country;
        this.suburb = suburb;
        this.longitude = null;
        this.latitude = null;
    }

    public Location (SerializedLocation location) {
        this(location.streetNo, location.streetName, location.unitNo, location.postcode, location.suburb, location.state, location.country);
    }

    public UUID getId () {
        return id;
    }

    public int getStreetNo () {
        return streetNo;
    }

    public String getUnitNo () {
        return unitNo;
    }

    public String getPostcode () {
        return postcode;
    }

    public String getState () {
        return state;
    }

    public String getCountry () {
        return country;
    }

    public String getLongitudeStr () {
        // Convert longitude into degree string
        return longitude != null ? new DecimalFormat("#.#######").format(Math.toDegrees(longitude)) : null;
    }

    private Double getLongitude () {
        return longitude;
    }

    public String getLatitudeStr () {
        // Convert latitude into degree string
        return latitude != null ? new DecimalFormat("#.#######").format(Math.toDegrees(latitude)) : null;
    }

    private Double getLatitude () {
        return latitude;
    }

    public String getStreetName() {
        return streetName;
    }

    public void lookupLongitudeLatitude () {
        var uuid = UUID.fromString(id.toString());
        var locationAPI = ApiLocator.locateApi(ILocationAPI.class);

        // Make location request
        var request = new LocationRequest()
                .withStreetNum(streetNo)
                .withStreetName(streetName)
                .withCity(suburb)
                .withPostcode(postcode)
                .withState(state)
                .withCountry(country);

        // Add async location request. Get by id, and only sends if location exists as it may be deleted in the meantime
        locationAPI.getLocationAsync(request,
                ((session, locationPoint) -> session.getById(Location.class, uuid).ifPresent(l -> l.setLongitudeLatitude(locationPoint))), 300);
    }

    public void setLongitudeLatitude (LocationPoint point) {
        if (point == null) {
            logger.warn("Failed to get longitude and latitude for location {}!", id);
        } else {
            this.longitude = point.getLongitude();
            this.latitude = point.getLatitude();
        }
    }

    public double getDistance (LocationPoint point) {
        if (point == null) {
            // Other point does not exist
            return -1;
        } else if (latitude == null || longitude == null) {
            // Own point does not exist, return infinite distance
            return Double.POSITIVE_INFINITY;
        } else {
            // Compare points
            return point.getDistance(new LocationPoint(latitude, longitude));
        }
    }

    public double getDistance (Location other) {
        if (latitude == null || longitude == null || other.getLongitude() == null || other.getLatitude() == null) {
            // One point does not exist, return infinite distance
            return Double.POSITIVE_INFINITY;
        } else {
            return new LocationPoint(latitude, longitude).getDistance(new LocationPoint(other.getLatitude(), other.getLongitude()));
        }
    }

    public SerializedLocation getSerialisedLocation () {
        return new SerializedLocation.Builder()
                .withStreetNo(streetNo)
                .withStreetName(streetName)
                .withUnitNo(unitNo)
                .withSuburb(suburb)
                .withPostcode(postcode)
                .withState(state)
                .withCountry(country)
                .withLatitude(getLatitudeStr())
                .withLongitude(getLongitudeStr())
                .build();
    }
}
