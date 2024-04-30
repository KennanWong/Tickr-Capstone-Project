package tickr.application.apis.location;

import com.google.gson.annotations.SerializedName;

public class NominatimPlace {
    @SerializedName("place_id")
    public String placeId;
    @SerializedName("osm_id")
    public String osmId;

    @SerializedName("lat")
    public String latitude;
    @SerializedName("lon")
    public String longitude;

    @SerializedName("class")
    public String placeClass;
}
