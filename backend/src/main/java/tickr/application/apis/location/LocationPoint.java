package tickr.application.apis.location;

public class LocationPoint {
    private static final String POINT_SEPARATOR = ",";
    private static final double RADIUS_OF_EARTH = 6378.1;
    private final double latitude;
    private final double longitude;

    public LocationPoint (String latitude, String longitude) {
        // Convert lat/lon to radians from degree strings
        this.latitude = Math.toRadians(Double.parseDouble(latitude));
        this.longitude = Math.toRadians(Double.parseDouble(longitude));
    }

    public LocationPoint (double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude () {
        return latitude;
    }

    public double getLongitude () {
        return longitude;
    }

    public String serialise () {
        return latitude + POINT_SEPARATOR + longitude;
    }
    public static LocationPoint deserialise (String serialisedPoint) {
        // Deserialise longitude and latitude
        var pointParts = serialisedPoint.split(POINT_SEPARATOR);
        return new LocationPoint(pointParts[0], pointParts[1]);
    }

    public double getDistance (LocationPoint other) {
        // Uses the Haversine formula: https://en.wikipedia.org/wiki/Haversine_formula
        double hav = haversine(other.getLatitude() - getLatitude())
                + Math.cos(getLatitude()) * Math.cos(other.getLatitude()) * haversine(other.getLongitude() - getLongitude());

        return RADIUS_OF_EARTH * archaversine(hav);
    }

    private static double haversine (double theta) {
        return (1.0 - Math.cos(theta)) / 2;
    }

    private static double archaversine (double a) {
        return 2 * Math.asin(Math.sqrt(a));
    }
}
