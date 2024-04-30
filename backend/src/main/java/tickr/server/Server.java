package tickr.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;
import spark.Spark;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.purchase.IPurchaseAPI;
import tickr.application.serialised.combined.comments.ReplyCreate;
import tickr.application.serialised.combined.comments.ReviewCreate;
import tickr.application.serialised.combined.tickets.TicketPurchase;
import tickr.application.serialised.combined.tickets.TicketReserve;
import tickr.application.serialised.combined.user.NotificationManagement;
import tickr.application.serialised.requests.comment.ReactRequest;
import tickr.application.serialised.requests.event.*;
import tickr.application.serialised.requests.group.*;
import tickr.application.serialised.requests.ticket.ReserveCancelRequest;
import tickr.application.serialised.requests.ticket.ReviewDeleteRequest;
import tickr.application.serialised.requests.ticket.TicketRefundRequest;
import tickr.application.serialised.requests.ticket.TicketViewEmailRequest;
import tickr.application.serialised.requests.user.*;
import tickr.persistence.DataModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ServerException;
import tickr.util.FileHelper;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class representing the server. The methods are static as Spark uses static functions to
 * control the server settings.
 */
public class Server {
    private static final int MIN_THREADS = 2;
    private static final int MAX_THREADS = 8;
    private static final int TIMEOUT_MS = 3000;

    static final Logger logger = LogManager.getLogger();

    private static DataModel dataModel;

    private static Gson gson;

    /**
     * Adds routes to the server
     */
    private static void addRoutes () {
        logger.info("Adding routes!");

        post("/api/user/register", TickrController::userRegister, UserRegisterRequest.class);
        post("/api/user/login", TickrController::userLogin, UserLoginRequest.class);
        delete("/api/user/logout", TickrController::userLogout, UserLogoutRequest.class);
        post("/api/user/reset/request", TickrController::unloggedChangePassword, UserRequestChangePasswordRequest.class);
        put("/api/user/reset", TickrController::loggedChangePassword, UserChangePasswordRequest.class);
        put("/api/user/reset/complete", TickrController::unloggedComplete, UserCompleteChangePasswordRequest.class);
        delete("/api/user/logout", TickrController::userLogout, UserLogoutRequest.class);

        get("/api/user/settings", TickrController::userGetSettings);
        put("/api/user/settings/update", TickrController::userUpdateSettings, NotificationManagement.UpdateRequest.class);

        get("/api/user/profile", TickrController::userGetProfile);
        put("/api/user/editprofile", TickrController::userEditProfile, EditProfileRequest.class);
        delete("/api/user/delete", TickrController::userDeleteAccount, UserDeleteRequest.class);
        get("/api/user/search", TickrController::userSearch);


        post("/api/test/event/create", TickrController::createEventUnsafe, CreateEventRequest.class);
        post("/api/event/create", TickrController::createEvent, CreateEventRequest.class);
        put("/api/event/edit", TickrController::editEvent, EditEventRequest.class);
        put("/api/event/make_host", TickrController::makeHost, EditHostRequest.class);
        get("/api/event/view", TickrController::eventView);
        get("/api/event/search", TickrController::searchEvents);
        delete("/api/event/cancel", TickrController::eventDelete, EventDeleteRequest.class);

        post("/api/event/announce", TickrController::makeAnnouncement, AnnouncementRequest.class);
        get("/api/event/notifications", TickrController::checkEventNotifications);
        put("/api/event/notifications/update", TickrController::eventNotificationsUpdate, EventNotificationsUpdateRequest.class);

        post("/api/event/review/create", TickrController::reviewCreate, ReviewCreate.Request.class);
        get("/api/event/reviews", TickrController::reviewsView);
        post("/api/event/review/reply", TickrController::replyCreate, ReplyCreate.Request.class);
        get("/api/event/reviews/replies", TickrController::repliesView);
        post("/api/event/review/react", TickrController::commentReact, ReactRequest.class);

        get("/api/event/hosting", TickrController::eventHostings);
        get("/api/event/hosting/past", TickrController::eventHostingsPast);
        get("/api/event/reserved", TickrController::eventReservedSeats);
        get("/api/event/attendees", TickrController::getEventAttendees);

        get("/api/events/categories/list", TickrController::categoriesList);
        get("/api/events/category", TickrController::eventsByCategory);

        post("/api/ticket/reserve", TickrController::ticketReserve, TicketReserve.Request.class);
        post("/api/ticket/purchase", TickrController::ticketPurchase, TicketPurchase.Request.class);
        post("/api/ticket/refund", TickrController::ticketRefund, TicketRefundRequest.class);
        get("/api/ticket/view", TickrController::ticketView);
        get("/api/event/bookings", TickrController::ticketBookings);
        post("api/ticket/send", TickrController::ticketViewSendEmail, TicketViewEmailRequest.class);
        delete("/api/ticket/reserve/cancel", TickrController::reservationCancel, ReserveCancelRequest.class);
        delete("/api/event/review/delete", TickrController::reviewDelete, ReviewDeleteRequest.class);

        get("/api/home", TickrController::userEvents);
        get("/api/home/past", TickrController::userEventsPast);
        get("/api/user/bookings", TickrController::customerBookings);
        get("/api/user/bookings/past", TickrController::customerBookingsPast);

        get("/api/user/hosting/future", TickrController::eventHostingFuture);
        get("/api/user/hosting/past", TickrController::eventHostingPast);

        post("/api/group/create", TickrController::groupCreate, GroupCreateRequest.class);
        get("/api/groups/get", TickrController::getGroupIds);
        post("/api/group/invite", TickrController::groupInvite, GroupInviteRequest.class);
        post("/api/group/accept", TickrController::groupAccept, GroupAcceptRequest.class);
        post("/api/group/deny", TickrController::groupDeny, GroupDenyRequest.class);
        get("/api/group/details", TickrController::groupDetails);
        delete("/api/group/remove", TickrController::groupRemoveMember, GroupRemoveMemberRequest.class);
        delete("api/group/cancel", TickrController::groupCancel, GroupCancelRequest.class);
        delete("/api/group/invite/remove", TickrController::groupRemoveInvite, GroupRemoveInviteRequest.class);
        get("/api/reserve/details", TickrController::getReserveDetails);

        get("/api/recommendations/event", TickrController::recommendEventEvent);
        get("/api/user/recommendations/home", TickrController::recommendUserEvent);
        get("/api/user/recommendations/event", TickrController::recommendEventUserEvent);

        Spark.get("/api/payment/cancel", (req, response) -> {
            // Route for payment cancellation
            var wrapper = new RouteWrapper<>(dataModel, ctx -> {
                var paramMap = ctx.request.queryParams()
                        .stream()
                        .collect(Collectors.toMap(Function.identity(), ctx.request::queryParams));

                ctx.controller.onPaymentCancel(ctx.session, paramMap);

                return new Object();
            });

            // Handle route
            wrapper.handle(req, response);

            // Return redirection
            response.redirect(req.queryParams("url"));

            return "";
        });

        Spark.post("/api/payment/webhook", new RouteWrapper<>(dataModel, ctx -> {
            // Route for payment webhook
            var paymentAPI = ApiLocator.locateApi(IPurchaseAPI.class);
            var sigHeader = ctx.request.headers(paymentAPI.getSignatureHeader());

            paymentAPI.handleWebhookEvent(ctx.controller, ctx.session, ctx.request.body(), sigHeader);

            return new Object();
        }));

        post("/api/test/event/create", TickrController::createEventUnsafe, CreateEventRequest.class);
        delete("/api/test/clear", TickrController::clearDatabase, Object.class);
    }

    /**
     * Starts the server
     * @param port the port to start on
     * @param frontendUrl the URL to accept CORS requests from
     * @param model the DataModel to use for server logic
     */
    public static void start (int port, String frontendUrl, DataModel model) {
        dataModel = model;
        gson = new Gson();

        Spark.externalStaticFileLocation(FileHelper.getStaticPath());

        Spark.port(port);
        Spark.threadPool(MAX_THREADS, MIN_THREADS, TIMEOUT_MS);

        Spark.before(((request, response) -> {
            // Log request
            if (request.queryString() != null) {
                logger.info("{} {}?{}", request.requestMethod(), request.pathInfo(), request.queryString());
            } else {
                logger.info("{} {}", request.requestMethod(), request.pathInfo());
            }
        }));

        addRoutes();

        Spark.exception(ServerException.class, ((exception, request, response) -> {
            // Catch server exception, log and convert to error response
            logger.debug("Server exception: ", exception);
            logger.info("\t{}: {}", exception.getStatusString(), exception.getMessage());
            response.status(exception.getStatusCode());
            response.type("application/json");
            response.body(gson.toJson(exception.getSerialised()));
        }));
        Spark.exception(Exception.class, (exception, request, response) -> {
            // Unexpected exception, catch and return internal server error
            logger.error("Uncaught exception: ", exception);
            response.status(500);
            response.body("Internal server error");
            logger.info("\t500: Internal Server Error");
        });

        Spark.afterAfter((request, response) -> {
            // Log successful responses
            if (response.status() == 200) {
                logger.info("\t200 OK");
                logger.debug(response.body());
            } else if (response.status() == 404) {
                logger.info("\t404: Not Found");
            }
        });

        logger.info("Finished server startup!");
    }



    /**
     * Add a GET route to the server
     * @param path
     * @param route function to run upon receiving a request
     * @param <R> Response object type
     */
    private static <R> void get (String path, BiFunction<TickrController, ModelSession, R> route) {
        Spark.get(path, new RouteWrapper<>(dataModel, ctx -> route.apply(ctx.controller, ctx.session)), gson::toJson);
    }

    /**
     * Add a GET route to the server
     * @param path
     * @param route function to run upon receiving a request
     * @param <R> Response object type
     */
    private static <R> void get (String path, TriFunction<TickrController, ModelSession, Map<String, String>, R> route) {
        Spark.get(path, new RouteWrapper<>(dataModel, ctx -> {
            var paramMap = ctx.request.queryParams()
                    .stream()
                    .collect(Collectors.toMap(Function.identity(), ctx.request::queryParams));

            return route.apply(ctx.controller, ctx.session, paramMap);
        }), gson::toJson);
    }

    /**
     * Adds a POST route to the server
     * @param path
     * @param route function to run upon receiving a request
     * @param reqClass class object of the deserialised request type
     * @param <T> Request object type
     * @param <R> Response object type
     */
    private static <T, R> void post (String path, TriFunction<TickrController, ModelSession, T, R> route, Class<T> reqClass) {
        Spark.post(path, new RouteWrapper<>(dataModel, ctx -> route.apply(ctx.controller, ctx.session, safeDeserialise(ctx.request, reqClass))),
                gson::toJson);
    }

    /**
     * Adds a POST route to the server with an empty response
     * @param path
     * @param route function to run upon receiving a request
     * @param reqClass class object of the deserialised request type
     * @param <T> Request object type
     */
    private static <T> void post (String path, TriConsumer<TickrController, ModelSession, T> route, Class<T> reqClass) {
        post(path, (t, u, v) -> {
            route.consume(t, u, v);
            return new Object();
        }, reqClass);
    }

    /**
     * Adds a PUT route to the server
     * @param path
     * @param route function to run upon receiving a request
     * @param reqClass class object of the deserialised request type
     * @param <T> Request object type
     * @param <R> Response object type
     */
    private static <T, R> void put (String path, TriFunction<TickrController, ModelSession, T, R> route, Class<T> reqClass) {
        Spark.put(path, new RouteWrapper<>(dataModel, ctx -> route.apply(ctx.controller, ctx.session, safeDeserialise(ctx.request, reqClass))),
                gson::toJson);
    }

    /**
     * Adds a PUT route to the server with an empty response
     * @param path
     * @param route function to run upon receiving a request
     * @param reqClass class object of the deserialised request type
     * @param <T> Request object type
     */
    private static <T> void put (String path, TriConsumer<TickrController, ModelSession, T> route, Class<T> reqClass) {
        put(path, (t, u, v) -> {
            route.consume(t, u, v);
            return new Object();
        }, reqClass);
    }

    /**
     * Adds a DELETE route to the server
     * @param path
     * @param route function to run upon receiving a request
     * @param reqClass class object of the deserialised request type
     * @param <T> Request object type
     * @param <R> Response object type
     */
    private static <T, R> void delete (String path, TriFunction<TickrController, ModelSession, T, R> route, Class<T> reqClass) {
        Spark.delete(path, new RouteWrapper<>(dataModel, ctx -> route.apply(ctx.controller, ctx.session, safeDeserialise(ctx.request, reqClass))),
                gson::toJson);
    }

    /**
     * Adds a DELETE route to the server with an empty response
     * @param path
     * @param route function to run upon receiving a request
     * @param reqClass class object of the deserialised request type
     * @param <T> Request object type
     */
    private static <T> void delete (String path, TriConsumer<TickrController, ModelSession, T> route, Class<T> reqClass) {
        delete(path, (t, u, v) -> {
            route.consume(t, u, v);
            return new Object();
        }, reqClass);
    }

    private static <T> T safeDeserialise (Request request, Class<T> reqClass) {
        try {
            logger.debug(request.body());
            return gson.fromJson(request.body(), reqClass);
        } catch (IllegalStateException | JsonSyntaxException e) {
            // JSON parse error
            throw new BadRequestException("Invalid request body!", e);
        }
    }
}
