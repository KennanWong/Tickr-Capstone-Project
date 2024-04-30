package tickr.application.serialised.requests.ticket;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class ReserveCancelRequest {
    @SerializedName("auth_token")
    public String authToken;
    public List<String> reservations = new ArrayList<>();
}
