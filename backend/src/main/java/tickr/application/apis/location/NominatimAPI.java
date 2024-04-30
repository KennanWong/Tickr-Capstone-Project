package tickr.application.apis.location;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tickr.persistence.DataModel;
import tickr.persistence.ModelSession;
import tickr.util.HTTPHelper;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NominatimAPI implements ILocationAPI {
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org";
    private static final String USER_AGENT = "tickr/0.1";
    private static final long REQUEST_TIMEOUT = 5000;

    private static final long MIN_REQUEST_PERIOD = 1500;
    static Logger logger = LogManager.getLogger();
    private final DataModel model;

    private final NominatimWorker worker;
    private LocationCache cache;

    public NominatimAPI (DataModel model) {
        this.model = model;
        cache = new LocationCache("location_cache.csv");
        worker = new NominatimWorker(cache);
    }

    @Override
    public LocationPoint getLocation (LocationRequest request) {
        // Try cache
        var locationOpt = cache.lookupPoint(request.buildSearchString());
        if (locationOpt.isPresent()) {
            // Cache hit, return result
            return locationOpt.orElseThrow();
        }

        // Create future we can wait on
        var future = new CompletableFuture<LocationPoint>();

        logger.debug("Queuing immediate job for request \"{}\"", request.buildSearchString());
        // Queue job with completeAfter 0 so it goes as soon as possible
        worker.queueJob(new NominatimJob(request, 0, future::complete, null));

        try {
            // Wait on future
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.warn("Received exception while waiting on future in Nominatim API for \"{}\"!", request.buildSearchString(), e);
            return null;
        }
    }

    @Override
    public void getLocationAsync (LocationRequest request, BiConsumer<ModelSession, LocationPoint> callback, long delayMs) {
        // Get complete after time
        long completeAfter = System.currentTimeMillis() + delayMs;
        logger.debug("Queuing async job for request \"{}\" for completion after {}", request.buildSearchString(), completeAfter);

        // Queue async job
        worker.queueJob(new NominatimJob(request, completeAfter, p -> {
            // Complete async job
            var session = model.makeSession();
            try {
                callback.accept(session, p);
                session.commit();
                session.close();
            } catch (Exception e) {
                logger.warn("Received exception when executing callback for request \"{}\"", request.buildSearchString(), e);
                session.rollback();
                session.close();
            }
        }, null));
    }


    private static class NominatimWorker {
        private final PriorityQueue<NominatimJob> jobQueue;
        private Thread runnerThread;
        private HTTPHelper httpHelper;
        private LocationCache cache;
        private long lastJobStart = 0;

        public NominatimWorker (LocationCache cache) {
            jobQueue = new PriorityQueue<>(16, Comparator.comparing(NominatimJob::getCompleteAfter));
            httpHelper = new HTTPHelper(NOMINATIM_URL);
            this.cache = cache;
            runnerThread = new Thread(this::runnerLoop);
            runnerThread.start();
        }

        public synchronized void queueJob (NominatimJob job) {
            // Queue job and notify worker thread if necessary
            jobQueue.add(job);
            this.notify();
        }


        private void runnerLoop () {
            while (true) {
                var job = waitOnJob();
                var newJobStart = System.currentTimeMillis();
                if (job.executeQuery(httpHelper, cache)) {
                    // Update job start only if necessary
                    lastJobStart = newJobStart;
                }
            }
        }

        @SuppressWarnings("BusyWait")
        private synchronized NominatimJob waitOnJob () {
            while (jobQueue.isEmpty()) {
                try {
                    // No job, wait for notification
                    this.wait();
                } catch (InterruptedException ignored) {

                }
            }

            // Wait until can start next job to avoid rate limits
            while (System.currentTimeMillis() < lastJobStart + MIN_REQUEST_PERIOD) {
                try {
                    Thread.sleep(lastJobStart + MIN_REQUEST_PERIOD - System.currentTimeMillis());
                } catch (InterruptedException ignored) {}
            }

            return jobQueue.poll();
        }
    }

    private static class NominatimJob {
        private LocationRequest request;
        private long completeAfter;
        private Consumer<LocationPoint> pointConsumer;
        private Object tag;

        public NominatimJob (LocationRequest request, long completeAfter, Consumer<LocationPoint> pointConsumer, Object tag) {
            this.request = request;
            this.completeAfter = completeAfter;
            this.pointConsumer = pointConsumer;
            this.tag = tag;
        }

        @SuppressWarnings("BusyWait")
        public boolean executeQuery (HTTPHelper httpHelper, LocationCache cache) {
            logger.debug("Executing location query \"{}\"", request.buildSearchString());
            boolean requestSent;
            var cacheResult = cache.lookupPoint(request.buildSearchString());

            LocationPoint result;
            if (cacheResult.isPresent()) {
                // Cache hit (likely request ran after job was added) so simply return
                logger.debug("Cache hit inside job for \"{}\"", request.buildSearchString());
                result = cacheResult.orElseThrow();
                requestSent = false;
            } else {
                // Query api, and if result
                result = queryAPI(httpHelper);
                if (result != null) {
                    cache.cachePoint(request.buildSearchString(), result);
                }
                requestSent = true;
            }

            new Thread(() -> {
                // Delay until complete after
                while (System.currentTimeMillis() < completeAfter) {
                    try {
                        Thread.sleep(completeAfter - System.currentTimeMillis());
                    } catch (InterruptedException ignored) {

                    }
                }

                // Run callback
                pointConsumer.accept(result);
            }).start();
            return requestSent;
        }

        public boolean matchesTag (Object query) {
            return Objects.equals(tag, query);
        }

        public long getCompleteAfter () {
            return completeAfter;
        }

        private LocationPoint queryAPI (HTTPHelper httpHelper) {
            logger.info("Performing Nominatim query for \"{}\"", request.buildSearchString());
            // Construct search query
            var response = httpHelper.get("/search", Map.of(
                    "street", String.format("%d %s", request.getStreetNum(), request.getStreetName()),
                    "city", request.getCity(),
                    "state", request.getState(),
                    "country", request.getCountry(),
                    "postalcode", request.getPostcode(),
                    "format", "jsonv2",
                    "addressdetails", "1"
            ), Map.of("User-Agent", USER_AGENT), REQUEST_TIMEOUT);

            if (response.getStatus() != 200) {
                logger.error("Nominatim query failed with code {}:\n{}", response.getStatus(), response.getBodyRaw());
                return null;
            }

            logger.debug("Nominatim response for \"{}\": \n{}", request.buildSearchString(), response.getBodyRaw());

            NominatimPlace[] results;
            try {
                results = response.getBody(NominatimPlace[].class);
            } catch (JsonSyntaxException | IllegalArgumentException e) {
                logger.error("Parsing nominatim response failed with exception, body:\n{}", response.getBodyRaw(), e);
                return null;
            }
            if (results.length == 0) {
                logger.info("Empty response to Nominatim query.");
                return null;
            }

            // Take first place, which is most relevant
            return new LocationPoint(results[0].latitude, results[0].longitude);
        }
    }
}
