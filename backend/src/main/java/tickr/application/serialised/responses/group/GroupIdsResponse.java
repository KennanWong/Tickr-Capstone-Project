package tickr.application.serialised.responses.group;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class GroupIdsResponse {
    public List<String> groups; 

    @SerializedName("num_results")
    public int numResults;

    public GroupIdsResponse(List<String> groups, int numResults) {
        this.groups = groups;
        this.numResults = numResults;
    }
}
