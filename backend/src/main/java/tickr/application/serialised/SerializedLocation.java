package tickr.application.serialised;

import com.google.gson.annotations.SerializedName;

public class SerializedLocation {
    @SerializedName ("street_name")
    public String streetName;

    @SerializedName ("street_no")
    public int streetNo;

    @SerializedName ("unit_no")
    public String unitNo;
    public String suburb;

    public String postcode;
    public String state;
    public String country;
    public String longitude;
    public String latitude;

    public SerializedLocation() {
    }

    public SerializedLocation(String streetName, int streetNo, String unitNo, String suburb, String postcode, String state, String country, String longitude, String latitude) {
        this.streetName = streetName;
        this.streetNo = streetNo;
        this.unitNo = unitNo;
        this.suburb = suburb;
        this.postcode = postcode;
        this.state = state;
        this.country = country;
        this.longitude = longitude; 
        this.latitude = latitude;
    } 

    public static class Builder {
        private String streetName = "High St";
        private int streetNo = 11;
        private String unitNo = "";
        private String suburb = "Kensington";
        private String postcode = "2052";
        private String state = "NSW";
        private String country = "Australia";
        private String longitude = "1.18231";
        private String latitude = "15.12731";

        public Builder withStreetName (String streetName) {
            this.streetName = streetName;
            return this;
        }

        public Builder withStreetNo (int streetNo) {
            this.streetNo = streetNo;
            return this;
        }

        public Builder withUnitNo (String unitNo) {
            this.unitNo = unitNo;
            return this;
        }

        public Builder withSuburb (String suburb) {
            this.suburb = suburb;
            return this;
        }

        public Builder withPostcode (String postcode) {
            this.postcode = postcode;
            return this;
        }

        public Builder withState (String state) {
            this.state = state;
            return this;
        }

        public Builder withCountry (String country) {
            this.country = country;
            return this;
        }

        public Builder withLongitude (String longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder withLatitude (String latitude) {
            this.latitude = latitude;
            return this;
        }

        public SerializedLocation build () {
            return new SerializedLocation(streetName, streetNo, unitNo, suburb, postcode, state, country, longitude, latitude);
        }
    }
}
