package tickr.mock;

import jakarta.transaction.RollbackException;
import org.junit.jupiter.api.Assertions;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.apis.location.LocationCache;
import tickr.application.apis.location.LocationPoint;
import tickr.application.apis.location.LocationRequest;
import tickr.persistence.DataModel;
import tickr.persistence.ModelSession;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class MockLocationApi implements ILocationAPI {
    private DataModel model;
    private LocationCache cache;
    private final List<Thread> threads;

    public MockLocationApi (DataModel model) {
        this.model = model;
        this.cache = new LocationCache("/location_cache.csv");
        threads = new ArrayList<>();
    }

    @Override
    public LocationPoint getLocation (LocationRequest request) {
        return cache.lookupPoint(request.buildSearchString())
                .orElse(null);
    }

    @Override
    public void getLocationAsync (LocationRequest request, BiConsumer<ModelSession, LocationPoint> callback, long delayMs) {
        long reqTime = System.currentTimeMillis() + delayMs;
        var thread = new Thread(() -> {
            try {
                Thread.sleep(Math.max(0, reqTime - System.currentTimeMillis()));
            } catch (InterruptedException ignored) {

            }
            if (model.isClosed()) {
                return;
            }
            var session = model.makeSession();
            try {
                callback.accept(session, getLocation(request));
                session.commit();
                session.close();
            } catch (Exception e) {
                session.rollback();
                session.close();
                Assertions.fail(e);
            }
        });
        thread.start();

        synchronized (threads) {
            threads.add(thread);
        }
    }

    public void awaitLocations () {
        synchronized (threads) {
            for (var i : threads) {
                boolean joined = false;
                while (!joined) {
                    try {
                        i.join();
                        joined = true;
                    } catch (InterruptedException ignored) {}
                }
            }
            threads.clear();
        }
    }
}
