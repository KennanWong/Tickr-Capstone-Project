package tickr.application.serialised;

import com.google.gson.annotations.SerializedName;

public class SerialisedReaction {
    @SerializedName("react_type")
    public String reactType;
    @SerializedName("react_num")
    public int reactNum;

    public SerialisedReaction () {}

    public SerialisedReaction (String reactType, int reactNum) {
        this.reactType = reactType;
        this.reactNum = reactNum;
    }
}
