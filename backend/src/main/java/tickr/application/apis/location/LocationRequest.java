package tickr.application.apis.location;

import tickr.application.serialised.SerializedLocation;

import java.util.Locale;

public class LocationRequest {
    private int streetNum;
    private String streetName;
    private String city;
    private String postcode;
    private String state;

    private String country;

    public LocationRequest () {
        streetNum = -1;
        streetName = null;
        city = null;
        postcode = null;
        state = null;
        country = null;
    }

    public LocationRequest withStreetNum (int streetNum) {
        this.streetNum = streetNum;
        return this;
    }

    public LocationRequest withStreetName (String streetName) {
        this.streetName = streetName;
        return this;
    }

    public LocationRequest withCity (String city) {
        this.city = city;
        return this;
    }

    public LocationRequest withPostcode (String postcode) {
        this.postcode = postcode;
        return this;
    }

    public LocationRequest withState (String state) {
        this.state = state;
        return this;
    }

    public LocationRequest withCountry (String country) {
        this.country = country;
        return this;
    }

    public int getStreetNum () {
        return streetNum;
    }

    public String getStreetName () {
        return streetName;
    }

    public String getCity () {
        return city;
    }

    public String getPostcode () {
        return postcode;
    }

    public String getCountry () {
        return country;
    }

    public String getState () {
        return state;
    }

    public String buildSearchString () {
        return String.format("%d %s %s %s %s %s", streetNum, streetName.toLowerCase(Locale.ROOT), city.toLowerCase(Locale.ROOT), postcode.toLowerCase(Locale.ROOT), state.toLowerCase(Locale.ROOT), country.toLowerCase(Locale.ROOT));
    }

    public static LocationRequest fromSerialised (SerializedLocation location) {
        return new LocationRequest()
                .withStreetNum(location.streetNo)
                .withStreetName(location.streetName)
                .withCity(location.suburb)
                .withPostcode(location.postcode)
                .withState(location.state)
                .withCountry(location.country);
    }
}
