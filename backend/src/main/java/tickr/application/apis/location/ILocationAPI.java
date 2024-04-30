package tickr.application.apis.location;

import tickr.persistence.ModelSession;

import java.util.function.BiConsumer;

public interface ILocationAPI {
    LocationPoint getLocation (LocationRequest request);

    void getLocationAsync (LocationRequest request, BiConsumer<ModelSession, LocationPoint> callback, long delayMs);
}
