package tickr.application.apis;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ApiLocator {
    private static ApiLocator INSTANCE = null;

    private final Map<Class<?>, CachedLocator> locators;

    private static synchronized ApiLocator getInstance() {
        // Gets locator singleton, must be synchronised to avoid double allocation
        if (INSTANCE == null) {
            INSTANCE = new ApiLocator();
        }

        return INSTANCE;
    }


    public static <T> void addLocator (Class<T> tClass, Supplier<T> locator) {
        getInstance().addLocatorInt(tClass, locator::get);
    }

    public static <T> T locateApi (Class<T> tClass) {
        return getInstance().locateApiInt(tClass);
    }

    public static <T> void clearLocator (Class<T> tClass) {
        // Removes locator from list
        getInstance().clearLocatorInt(tClass);
    }


    private ApiLocator () {
        locators = new HashMap<>();
    }

    private void addLocatorInt (Class<?> tClass, Supplier<Object> locator) {
        // Insert locator into list
        synchronized (locators) {
            locators.put(tClass, new CachedLocator(locator));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T locateApiInt (Class<T> tClass) {
        synchronized (locators) {
            if (locators.containsKey(tClass)) {
                // Get locator, cast is ok as it is mapped to correct class type
                return (T)locators.get(tClass).locate();
            } else {
                throw new RuntimeException("Failed to locate implementation for service " + tClass.getCanonicalName() + "!");
            }
        }
    }

    private <T> void clearLocatorInt (Class<T> tClass) {
        synchronized (locators) {
            locators.remove(tClass);
        }
    }

    private static class CachedLocator {
        private Object api = null;
        private final Supplier<Object> locator;

        public CachedLocator (Supplier<Object> locator) {
            this.locator = locator;
        }

        public Object locate () {
            // Lazy allocating locator, unsynchronised is ok as it is always within a synchronised block
            if (api == null) {
                api = locator.get();
            }

            return api;
        }
    }
}
