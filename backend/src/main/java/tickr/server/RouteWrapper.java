package tickr.server;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;
import tickr.application.TickrController;
import tickr.persistence.DataModel;
import tickr.persistence.ModelSession;

import java.util.function.Function;

/**
 * Class which encapsulates route handling logic, ensuring route handlers can be run
 * thread safely.
 * @param <T> Response object type
 */
public class RouteWrapper<T> implements Route {
    static final Logger logger = LogManager.getLogger();
    private Function<Context, T> route;
    private DataModel model;

    public RouteWrapper (DataModel model, Function<Context, T> route) {
        this.route = route;
        this.model = model;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Override
    public Object handle (Request request, Response response) throws Exception {
        var userSession = request.session(false);
        if (userSession == null) {
            // Create new user session
            userSession = request.session(true);
            // Initialise new tickr controller object
            userSession.attribute("controller", new TickrController());
        }
        var controller = (TickrController)userSession.attribute("controller");
        var modelSession = model.makeSession();

        T result;
        try {
            synchronized (controller) {
                // For if multiple threads access the same user session
                result = route.apply(new Context(request, controller, modelSession));
            }
            // Commit result and close session
            modelSession.commit();
            modelSession.close();
        } catch (Exception e) {
            // Rollback results and close session
            modelSession.rollback();
            modelSession.close();
            throw e;
        }

        // All responses will be json type
        response.type("application/json");
        return result;
    }

    /**
     * Class encapsulating the context given to route handling functions
     */
    public static class Context {
        public final Request request;
        public final TickrController controller;
        public final ModelSession session;

        Context (Request request, TickrController controller, ModelSession session) {
            this.request = request;
            this.controller = controller;
            this.session = session;
        }
    }
}
