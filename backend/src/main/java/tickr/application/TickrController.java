package tickr.application;

import io.jsonwebtoken.JwtException;
import jakarta.persistence.PersistenceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tickr.application.apis.ApiLocator;
import tickr.application.apis.email.IEmailAPI;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.apis.location.LocationRequest;
import tickr.application.apis.purchase.IPurchaseAPI;
import tickr.application.entities.*;
import tickr.application.recommendations.InteractionType;
import tickr.application.recommendations.RecommenderEngine;
import tickr.application.serialised.combined.comments.ReplyCreate;
import tickr.application.serialised.combined.comments.ReviewCreate;
import tickr.application.serialised.combined.event.EventSearch;
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
import tickr.application.serialised.responses.comment.RepliesViewResponse;
import tickr.application.serialised.responses.comment.ReviewsViewResponse;
import tickr.application.serialised.responses.event.*;
import tickr.application.serialised.responses.group.*;
import tickr.application.serialised.responses.ticket.ReserveDetailsResponse;
import tickr.application.serialised.responses.ticket.TicketBookingsResponse;
import tickr.application.serialised.responses.ticket.TicketViewEmailResponse;
import tickr.application.serialised.responses.ticket.TicketViewResponse;
import tickr.application.serialised.responses.user.*;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.server.exceptions.UnauthorizedException;
import tickr.util.CryptoHelper;
import tickr.util.FileHelper;
import tickr.util.Pair;
import tickr.util.Utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class encapsulating business logic. Is created once per user session, which is distinct to ModelSession instances -
 * TickrController instances may persist for the duration a user interacts with the service, though this should not
 * be relied upon
 */
public class TickrController {
    // From https://stackoverflow.com/questions/201323/how-can-i-validate-an-email-address-using-a-regular-expression
    private static final Pattern EMAIL_REGEX = Pattern.compile("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])");
    private final Pattern PASS_REGEX = Pattern.compile("(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,}$");

    private static final Duration AUTH_TOKEN_EXPIRY = Duration.ofDays(30);

    static final Logger logger = LogManager.getLogger();
    public TickrController () {

    }

    private AuthToken getTokenFromStr (ModelSession session, String authTokenStr) {
        AuthToken token;
        try {
            // Parse token
            var parsedToken = CryptoHelper.makeJWTParserBuilder()
                    .build()
                    .parseClaimsJws(authTokenStr);

            // Get id from token
            var tokenId = UUID.fromString(parsedToken.getBody().getId());

            // Lookup token
            token = session.getById(AuthToken.class, tokenId).orElseThrow(() -> new UnauthorizedException("Invalid auth token!"));
        } catch (JwtException | IllegalArgumentException e) {
            // Token parse failed
            throw new UnauthorizedException("Invalid auth token!", e);
        }

        if (!token.makeJWT().equals(authTokenStr.trim())) {
            // Invalid JWT
            throw new UnauthorizedException("Invalid auth token!");
        }

        return token;
    }

    public User authenticateToken (ModelSession session, String authTokenStr) {
        return getTokenFromStr(session, authTokenStr).getUser();
    }

    private UUID parseUUID (String uuidStr) {
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            // Parse error
            throw new BadRequestException("Invalid uuid!", e);
        }
    }

    public AuthTokenResponse userRegister (ModelSession session, UserRegisterRequest request) {
        if (!request.isValid()) {
            logger.debug("Missing parameters!");
            throw new BadRequestException("Invalid register request!");
        }

        LocalDate dob;
        try {
            dob = LocalDate.parse(request.dateOfBirth, DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            logger.debug("Date is in incorrect format!");
            throw new ForbiddenException("Invalid date of birth string!");
        }

        var user = new User(request.email.trim().toLowerCase(Locale.ROOT), request.password.trim(),
                request.userName.trim(), request.firstName.trim(), request.lastName.trim(), dob);

        // Save the user, this is necessary because we need to check that the email is unique
        // and that only occurs on commit
        try {
            session.save(user);
            session.commit();
            session.newTransaction();
        } catch (PersistenceException e) {
            throw new ForbiddenException(String.format("Email %s is already in use!", request.email.trim()), e);
        }

        var authToken = user.makeToken(session, AUTH_TOKEN_EXPIRY);

        return new AuthTokenResponse(authToken.makeJWT());
    }

    public AuthTokenResponse userLogin (ModelSession session, UserLoginRequest request) {
        if (!request.isValid()) {
            throw new BadRequestException("Invalid request!");
        }

        var user = session.getByUnique(User.class, "email", request.email)
                .orElseThrow(() -> new ForbiddenException(String.format("Unknown account \"%s\".", request.email)));


        return new AuthTokenResponse(user.authenticatePassword(session, request.password, AUTH_TOKEN_EXPIRY).makeJWT());
    }

    public void userLogout (ModelSession session, UserLogoutRequest request) {
        var token = getTokenFromStr(session, request.authToken);
        var user = token.getUser();
        user.invalidateToken(session, token);
    }

    public NotificationManagement.GetResponse userGetSettings (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("auth_token")) {
            throw new UnauthorizedException("Missing auth token!");
        }
        var user = authenticateToken(session, params.get("auth_token"));
        return new NotificationManagement.GetResponse(user.getSettings());
    }

    public NotificationManagement.GetResponse userUpdateSettings (ModelSession session, NotificationManagement.UpdateRequest request) {
        if (request.authToken == null) {
            throw new UnauthorizedException("Missing auth token!");
        }

        if (request.settings == null) {
            throw new BadRequestException("Missing settings!");
        }

        var user = authenticateToken(session, request.authToken);
        user.setSettings(request.settings);
        return new NotificationManagement.GetResponse(user.getSettings());
    }

    public ViewProfileResponse userGetProfile (ModelSession session, Map<String, String> params) {
        if (params.containsKey("auth_token") == params.containsKey("user_id")) {
            throw new BadRequestException("Invalid request!");
        }

        User user;
        if (params.containsKey("auth_token")) {
            user = authenticateToken(session, params.get("auth_token"));
        } else {
            user = session.getById(User.class, UUID.fromString(params.get("user_id")))
                    .orElseThrow(() -> new ForbiddenException("Unknown user."));
        }

        return user.getProfile();
    }

    private CreateEventResponse createEventInternal (ModelSession session, CreateEventRequest request, boolean checkDates) {
        // Internal function for both test and non-test events
        if (request.authToken == null) {
            throw new UnauthorizedException("Missing auth token!");
        }

        if (!request.isValid() ) {
            throw new BadRequestException("Invalid event request!");
        }

        if (!request.isSeatingDetailsValid()) {
            throw new BadRequestException("Invalid seating details!");
        }

        if (request.location != null && !request.isLocationValid()) {
            throw new BadRequestException("Invalid location details!");
        }

        ZonedDateTime startDate;
        ZonedDateTime endDate;
        try {
            startDate = ZonedDateTime.parse(request.startDate, DateTimeFormatter.ISO_DATE_TIME);
            endDate = ZonedDateTime.parse(request.endDate, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new ForbiddenException("Invalid date time string!", e);
        }

        // getting user from token
        var user = authenticateToken(session, request.authToken);

        // creating event from request
        Event event;
        if (request.picture == null) {
            // No picture uploaded
            event = new Event(request.eventName, user, startDate, endDate, request.description, request.location, request.getSeatCapacity(),
                    "", request.spotifyPlaylist, checkDates);
        } else {
            // Picture uploaded
            event = new Event(request.eventName, user, startDate, endDate, request.description, request.location, request.getSeatCapacity(),
                    FileHelper.uploadFromDataUrl("event", UUID.randomUUID().toString(), request.picture)
                            .orElseThrow(() -> new ForbiddenException("Invalid event image!")), request.spotifyPlaylist, checkDates);
        }
        session.save(event);
        event.onUpdate();
        // creating seating plan for each section
        if (request.seatingDetails != null) {
            for (CreateEventRequest.SeatingDetails seats : request.seatingDetails) {
                SeatingPlan seatingPlan = new SeatingPlan(event, event.getLocation(), seats.section, seats.availability, seats.ticketPrice, seats.hasSeats);
                session.save(seatingPlan);
            }
        }

        // Creating tags
        if (request.tags != null) {
            for (String tagStr : request.tags) {
                Tag newTag = new Tag(event, tagStr);
                event.addTag(newTag);
                session.save(newTag);
            }
        }

        // Creating categories
        if (request.categories != null) {
            for (String catStr : request.categories) {
                Category newCat = new Category(event, catStr);
                event.addCategory(newCat);
                session.save(newCat);
            }
        }

        // Adding admins
        if (request.admins != null) {
            for (String admin : request.admins) {
                User userAdmin;
                try {
                    userAdmin = session.getById(User.class, UUID.fromString(admin))
                            .orElseThrow(() -> new ForbiddenException(String.format("Unknown account \"%s\".", admin)));
                } catch (IllegalArgumentException e) {
                    throw new ForbiddenException("invalid admin Id");
                }

                event.addAdmin(userAdmin);
            }
        }

        RecommenderEngine.forceRecalculate(session);

        return new CreateEventResponse(event.getId().toString());
    }

    public CreateEventResponse createEvent (ModelSession session, CreateEventRequest request) {
        return createEventInternal(session, request, true);
    }

    public CreateEventResponse createEventUnsafe (ModelSession session, CreateEventRequest request) {
        return createEventInternal(session, request, false);
    }

    public void userEditProfile (ModelSession session, EditProfileRequest request) {
        var user = authenticateToken(session, request.authToken);

        if (request.email != null && !EMAIL_REGEX.matcher(request.email.trim().toLowerCase()).matches()) {
            logger.debug("Email did not match regex!");
            throw new ForbiddenException("Invalid email!");
        }

        if (request.pfpDataUrl == null) {
            // No picture uploaded
            user.editProfile(request.username, request.firstName, request.lastName, request.email, request.profileDescription, null);
        } else {
            // Picture uploaded
            user.editProfile(request.username, request.firstName, request.lastName, request.email, request.profileDescription,
                    FileHelper.uploadFromDataUrl("profile", UUID.randomUUID().toString(), request.pfpDataUrl)
                            .orElseThrow(() -> new ForbiddenException("Invalid data url!")));
        }
    }

    public AuthTokenResponse loggedChangePassword (ModelSession session, UserChangePasswordRequest request) {
        if (!request.isValid()) {
            throw new BadRequestException("Invalid request!");
        }

        if (!PASS_REGEX.matcher(request.password.trim()).matches()) {
            logger.debug("Password did not match regex!");
            throw new ForbiddenException("Invalid password!");
        }

        if (!PASS_REGEX.matcher(request.newPassword.trim()).matches()) {
            logger.debug("New password did not match regex!");
            throw new ForbiddenException("Invalid new password!");
        }

        var user = authenticateToken(session, request.authToken);

        user.authenticatePassword(session, request.password, AUTH_TOKEN_EXPIRY);
        user.changePassword(session, request.newPassword);

        var newAuthToken = user.makeToken(session, AUTH_TOKEN_EXPIRY).makeJWT();
        return new AuthTokenResponse(newAuthToken);
    }

    public RequestChangePasswordResponse unloggedChangePassword (ModelSession session, UserRequestChangePasswordRequest userRequestChangePasswordRequest) {
        if (!userRequestChangePasswordRequest.isValid()) {
            throw new BadRequestException("Invalid request!");
        }

        if (!EMAIL_REGEX.matcher(userRequestChangePasswordRequest.email.trim()).matches()) {
            logger.debug("Email did not match regex!");
            throw new ForbiddenException("Invalid email!");
        }

        var user = session.getByUnique(User.class, "email", userRequestChangePasswordRequest.email)
                .orElseThrow(() -> new ForbiddenException("Account does not exist."));

        user.sendPasswordReset(session);

        return new RequestChangePasswordResponse(true);
    }

    public AuthTokenResponse unloggedComplete (ModelSession session, UserCompleteChangePasswordRequest request) {
        if (!request.isValid()) {
            throw new BadRequestException("Invalid request!");
        }

        if (!EMAIL_REGEX.matcher(request.email.trim()).matches()) {
            logger.debug("Email did not match regex!");
            throw new ForbiddenException("Invalid email!");
        }

        if (!PASS_REGEX.matcher(request.newPassword.trim()).matches()) {
            logger.debug("Password did not match regex!");
            throw new ForbiddenException("Invalid password!");
        }

        var user = session.getByUnique(User.class, "email", request.email)
                .orElseThrow(() -> new ForbiddenException("Account does not exist."));

        user.changePassword(session, request.newPassword);

        var newAuthToken = user.makeToken(session, AUTH_TOKEN_EXPIRY).makeJWT();
        return new AuthTokenResponse(newAuthToken);
    }

    public UserIdResponse userSearch (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("email")) {
            throw new BadRequestException("Missing email parameter!");
        }

        var email = params.get("email");

        if (!EMAIL_REGEX.matcher(email.trim().toLowerCase()).matches()) {
            throw new BadRequestException("Invalid email!");
        }

        var user = session.getByUnique(User.class, "email", email.toLowerCase())
                .orElseThrow(() -> new ForbiddenException("There is no user with email " + email + "."));

        return new UserIdResponse(user.getId().toString());
    }

    public void editEvent (ModelSession session, EditEventRequest request) {
        Event event = session.getById(Event.class, UUID.fromString(request.getEventId()))
                        .orElseThrow(() -> new ForbiddenException("Invalid event"));
        User user = authenticateToken(session, request.getAuthToken());
        if (!user.getId().equals(event.getHost().getId()) && !event.getAdmins().contains(user)) {
            throw new ForbiddenException("User is not a host/admin of the event!");
        }

        if (!request.isSeatingDetailsValid()) {
            throw new BadRequestException("Invalid seating details!");
        }

        if (request.picture == null) {
            // No picture upload
            event.editEvent(user, request, session,null);
        } else {
            // Picture upload
            event.editEvent(user, request, session, FileHelper.uploadFromDataUrl("profile", UUID.randomUUID().toString(), request.picture)
                    .orElseThrow(() -> new ForbiddenException("Invalid data url!")));
        }

        RecommenderEngine.forceRecalculate(session);
    }

    public EventViewResponse eventView (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("event_id")) {
            throw new BadRequestException("Missing event_id!");
        }
        Event event = session.getById(Event.class, UUID.fromString(params.get("event_id")))
                        .orElseThrow(() -> new ForbiddenException("Unknown event"));

        User user = null;
        if (params.containsKey("auth_token")) {
            user = authenticateToken(session, params.get("auth_token"));
            if (user != null && !event.getHost().equals(user)) {
                RecommenderEngine.recordInteraction(session, user, event, InteractionType.VIEW);
            }

        }

        return event.getViewResponse(user);
    }

    public void makeHost (ModelSession session, EditHostRequest request) {
        User newHost = session.getByUnique(User.class, "email", request.newHostEmail)
                            .orElseThrow(() -> new ForbiddenException("Invalid user"));

        User oldHost = authenticateToken(session, request.authToken);

        Event event = session.getById(Event.class, UUID.fromString(request.eventId))
                        .orElseThrow(() -> new ForbiddenException("Invalid event"));

        event.makeHost(oldHost, newHost);
    }

    public EventSearch.Response searchEvents (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Missing paging parameters!");
        }

        int pageStart; // Start item to paginate from
        int maxResults; // Maximum results to return
        try {
            pageStart = Integer.parseInt(params.get("page_start"));
            maxResults = Integer.parseInt(params.get("max_results"));
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid paging parameters!");
        }
        if (pageStart < 0 || maxResults <= 0 || maxResults > 256) {
            throw new BadRequestException("Invalid paging parameters!");
        }

        if (params.containsKey("auth_token")) {
            authenticateToken(session, params.get("auth_token"));
        }

        EventSearch.Options options = null;
        if (params.containsKey("search_options")) {
            // Extract option params
            options = EventSearch.fromParams(params.get("search_options"));
        }

        var eventStream = session.getAllStream(Event.class)
                .filter(x -> !x.endsBefore(ZonedDateTime.now())) // Ensure event isn't in past
                .filter(Event::isPublished); // Only return published events

        if (options != null) {
            var options1 = options; // Required because Java is dumb
            // Add options
            eventStream = eventStream
                    .filter(e -> e.startsAfter(options1.getStartTime())) // Ensures after start time
                    .filter(e -> e.endsBefore(options1.getEndTime())) // Ensures after end time
                    .filter(e -> e.matchesCategories(options1.categories)) // Ensures matches categories
                    .filter(e -> e.matchesTags(options1.tags)) // Ensures matches tags
                    .filter(e -> e.matchesDescription(Utils.toWords(options1.text))); // Ensure matches text

            if ((options.location != null && options.maxDistance == null) ||
                    (options.location == null && options.maxDistance != null) || (options.maxDistance != null && options.maxDistance < 0)) {
                // Must have none or both of location and maxDistance options, and distance cannot be negative
                throw new BadRequestException("Invalid location options!");
            }

            if (options.location != null) {
                // Add location options
                var queryLocation = ApiLocator.locateApi(ILocationAPI.class).getLocation(LocationRequest.fromSerialised(options.location));
                eventStream = eventStream.filter(e -> e.getLocation().getDistance(queryLocation) <= options1.maxDistance); // Ensure within distance
            }

        }

        // Total matching count. As .sorted() consumes all items of the stream, .peek() will be called once each,
        // thereby ensuring that numItems is the total amount even though subsequent calls only consume the required
        // number. Must be atomic because the stream could theoretically run over multiple threads (even though it doesn't)
        var numItems = new AtomicInteger();
        var eventList = eventStream
                .peek(x -> numItems.incrementAndGet()) // Increment num items
                .sorted(Comparator.comparing(Event::getEventStart)) // Sort by start time
                .skip(pageStart) // Skip to start of page
                .limit(maxResults) // Limit to amount being paged
                .map(Event::getId) // Get the id
                .map(UUID::toString)
                .collect(Collectors.toList()); // Convert to list

        return new EventSearch.Response(eventList, numItems.get());
    }

    public void eventDelete(ModelSession session, EventDeleteRequest request) {
        if (!request.isValid()) {
            throw new BadRequestException("Invalid request details!");
        }
        Event event = session.getById(Event.class, UUID.fromString(request.eventId))
                        .orElseThrow(() -> new ForbiddenException("Invalid event ID!"));
        User user = authenticateToken(session, request.authToken);

        if (!event.getHost().equals(user)) {
            throw new ForbiddenException("User is not the host of this event!"); 
        }

        event.onDelete(user);
        session.remove(event);
        RecommenderEngine.forceRecalculate(session); // TODO
    }

    public void userDeleteAccount(ModelSession session, UserDeleteRequest request) {
        if (!request.isValid()) {
            throw new BadRequestException("Invalid request!");
        }

        User user = authenticateToken(session, request.authToken);
        user.authenticatePassword(session, request.password, AUTH_TOKEN_EXPIRY);

        user.onDelete(session);
        session.remove(user);
    }

    public TicketReserve.Response ticketReserve (ModelSession session, TicketReserve.Request request) {
        var user = authenticateToken(session, request.authToken);
        if (request.eventId == null || request.ticketDateTime == null || request.ticketDetails == null || request.ticketDetails.size() == 0) {
            throw new BadRequestException("Invalid request!");
        }

        var eventId = parseUUID(request.eventId);
        Event event = session.getById(Event.class, UUID.fromString(request.eventId))
        .orElseThrow(() -> new ForbiddenException("Invalid event id!"));
        
        event.editNotificationMembers(session, user, user.doReminders());
        user.editEventNotifications(session, event, user.doReminders());

        return session.getById(Event.class, eventId)
                .map(e -> request.ticketDetails.stream() // Stream all ticket details
                        .map(t -> e.makeReservations(session, user, request.getTicketTime(), t.section, t.quantity, t.seatNums)) // Make reservations for each one
                        .flatMap(Collection::stream) // Flat map the stream of lists to a single stream
                        .map(TicketReservation::getDetails) // Get details of each reservation
                        .collect(Collectors.toList())) // Convert to list
                .map(TicketReserve.Response::new) // Make response from reservation list
                .orElseThrow(() -> new ForbiddenException("Invalid event!")); // Event did not exist (none of the .maps ran)

    }

    public TicketPurchase.Response ticketPurchase (ModelSession session, TicketPurchase.Request request) {
        var user = authenticateToken(session, request.authToken);
        if (request.ticketDetails == null || request.ticketDetails.size() == 0 || request.successUrl == null || request.cancelUrl == null
                || !Utils.isValidUrl(request.successUrl) || !Utils.isValidUrl(request.cancelUrl)) {
            throw new BadRequestException("Invalid request!");
        }

        
        var purchaseAPI = ApiLocator.locateApi(IPurchaseAPI.class);
        var orderId = UUID.randomUUID(); // Id associated with order for fulfillment use
        var builder = purchaseAPI.makePurchaseBuilder(orderId.toString());

        // Build order
        for (var i : request.ticketDetails) {
            builder = session.getById(TicketReservation.class, UUID.fromString(i.requestId))
                    .orElseThrow(() -> new ForbiddenException("Invalid ticket reservation!")) // Reservation does not exist
                    .registerPurchaseItem(session, builder, orderId, user, i.firstName, i.lastName, i.email); // Add purchase item to builder
        }

        // Register order and return the redirect url
        return new TicketPurchase.Response(purchaseAPI.registerOrder(builder.withUrls(request.successUrl, request.cancelUrl)));
    }

    public void reservationCancel (ModelSession session, ReserveCancelRequest request) {
        var user = authenticateToken(session, request.authToken);
        if (request.reservations.size() == 0) {
            throw new BadRequestException("Empty reservations!");
        }

        for (var i : request.reservations) {
            var entity = session.getById(TicketReservation.class, parseUUID(i))
                    .orElseThrow(() -> new ForbiddenException("Invalid ticket reservation!"));
            if (!entity.canCancel(user)) {
                throw new ForbiddenException("Unable to cancel reservation!");
            }
            session.remove(entity);
        }
    }

    public void ticketPurchaseSuccess (ModelSession session, String orderId, String paymentId) {
        logger.info("Ticket purchase {} success!", orderId);
        // Get all associated with order id
        var purchaseItems = session.getAllWith(PurchaseItem.class, "purchaseId", UUID.fromString(orderId));
        RecommenderEngine.recordInteraction(session, purchaseItems.get(0).getUser(), purchaseItems.get(0).getEvent(), InteractionType.TICKET_PURCHASE);
        for (var i : purchaseItems) {
            // Convert reservations to tickets
            session.save(i.convert(session, paymentId));
        }
    }

    public void ticketPurchaseCancel (ModelSession session, String reserveId) {
        logger.info("Order {} was cancelled!", reserveId);
        for (var i : session.getAllWith(PurchaseItem.class, "purchaseId", UUID.fromString(reserveId))) {
            i.cancel(session);
        }
    }

    public void ticketPurchaseFailure (ModelSession session, String reserveId) {
        logger.info("Order {} failed!", reserveId);
        for (var i : session.getAllWith(PurchaseItem.class, "purchaseId", UUID.fromString(reserveId))) {
            i.cancel(session);
        }
    }

    public TicketViewResponse ticketView (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("ticket_id")) {
            throw new BadRequestException("Missing ticket ID!");
        }
        Ticket ticket = session.getById(Ticket.class, UUID.fromString(params.get("ticket_id")))
                            .orElseThrow(() -> new ForbiddenException("Invalid ticket ID!"));
        return ticket.getTicketViewResponse();
    }

    public TicketBookingsResponse ticketBookings (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("auth_token")) {
            throw new BadRequestException("Missing token!");
        }
        if (!params.containsKey("event_id")) {
            throw new BadRequestException("Missing event ID!");
        }
        User user = authenticateToken(session, params.get("auth_token"));
        Event event = session.getById(Event.class, UUID.fromString(params.get("event_id")))
                        .orElseThrow(() -> new ForbiddenException("Invalid event ID!"));

        return new TicketBookingsResponse(event.getUserTicketIds(user));
    }

    public TicketViewEmailResponse ticketViewSendEmail (ModelSession session, TicketViewEmailRequest request) {
        if (!request.isValid()) {
            throw new BadRequestException("Invalid request details!");
        }

        User recipient = session.getByUnique(User.class, "email", request.email)
                            .orElseThrow(() -> new ForbiddenException("Invalid email!"));
        Ticket ticket = session.getById(Ticket.class, UUID.fromString(request.ticketId))
                            .orElseThrow(() -> new ForbiddenException("Invalid Ticket ID"));

        User user = authenticateToken(session, request.authToken);
        if (!ticket.isOwnedBy(user)) {
            throw new BadRequestException("User does not own this ticket!");
        };

        var message = String.format("Please click below to view your ticket <a href=\"%s\">here</a>.\n", ticket.getUrl());

        ApiLocator.locateApi(IEmailAPI.class).sendEmail(recipient.getEmail(), "View user ticket details", message);

        return new TicketViewEmailResponse(true);
    } 

    public EventAttendeesResponse getEventAttendees (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("auth_token")) {
            throw new BadRequestException("Missing token!");
        }
        if (!params.containsKey("event_id")) {
            throw new BadRequestException("Missing event ID!");
        }

        Event event = session.getById(Event.class, UUID.fromString(params.get("event_id")))
                            .orElseThrow(() -> new ForbiddenException("Invalid Event ID!")); 
        User user = authenticateToken(session, params.get("auth_token"));

        return new EventAttendeesResponse(event.getAttendees(user));
    }
    
    public ReviewCreate.Response reviewCreate (ModelSession session, ReviewCreate.Request request) {
        var user = authenticateToken(session, request.authToken);
        if (request.eventId == null) {
            throw new BadRequestException("Null event id!");
        }

        var event = session.getById(Event.class, UUID.fromString(request.eventId))
                .orElseThrow(() -> new ForbiddenException("Invalid event id!"));

        var comment = event.addReview(session, user, request.title, request.text, request.rating);
        RecommenderEngine.recordRating(session, user, event, request.rating);
        //session.save(comment);

        return new ReviewCreate.Response(comment.getId().toString());
    }

    public ReviewsViewResponse reviewsView (ModelSession session, Map<String, String> params) {
        if (params.containsKey("auth_token")) {
            authenticateToken(session, params.get("auth_token"));
        }

        if (!params.containsKey("event_id")) {
            throw new BadRequestException("Missing event id!");
        }

        var event = session.getById(Event.class, UUID.fromString(params.get("event_id")))
                .orElseThrow(() -> new ForbiddenException("Invalid event id!"));

        if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Missing paging params!");
        }

        int pageStart;
        int maxResults;
        try {
            pageStart = Integer.parseInt(params.get("page_start"));
            maxResults = Integer.parseInt(params.get("max_results"));
        } catch (NumberFormatException e) {
            throw new ForbiddenException("Invalid paging values!", e);
        }

        if (pageStart < 0 || maxResults <= 0 || maxResults > 256) {
            throw new ForbiddenException("Invalid paging values!");
        }

        var numItems = new AtomicInteger(); // Number of reviews in total
        var reviews = session.getAllWithStream(Comment.class, "event", event)
                .filter(Predicate.not(Comment::isReply)) // Filter out replies
                .peek(c -> numItems.getAndIncrement()) // Get number of reviews
                .sorted(Comparator.comparing(Comment::getCommentTime).reversed()) // Sort by time so earlier comments are first
                .skip(pageStart) // Skip to page
                .limit(maxResults) // Limit to num results requested
                .map(Comment::makeSerialisedReview) // Make review
                .collect(Collectors.toList()); // Convert to list

        return new ReviewsViewResponse(reviews, numItems.get());
    }

    public ReplyCreate.Response replyCreate (ModelSession session, ReplyCreate.Request request) {
        var user = authenticateToken(session, request.authToken);
        if (request.reviewId == null) {
            throw new BadRequestException("Null review id!");
        }

        var comment = session.getById(Comment.class, UUID.fromString(request.reviewId))
                .orElseThrow(() -> new ForbiddenException("Unknown review id!"));

        var reply = comment.addReply(user, request.reply);
        session.save(reply);

        if (!comment.getEvent().getHost().equals(user)) {
            // Record interaction if not host
            RecommenderEngine.recordInteraction(session, user, comment.getEvent(), InteractionType.COMMENT);
        }

        return new ReplyCreate.Response(reply.getId().toString());
    }

    public RepliesViewResponse repliesView (ModelSession session, Map<String, String> params) {
        if (params.containsKey("auth_token")) {
            authenticateToken(session, params.get("auth_token"));
        }
        if (!params.containsKey("review_id") || !params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Invalid request params!");
        }

        var review = session.getById(Comment.class, UUID.fromString(params.get("review_id")))
                .orElseThrow(() -> new ForbiddenException("Invalid review id!"));

        int pageStart;
        int maxResults;
        try {
            pageStart = Integer.parseInt(params.get("page_start"));
            maxResults = Integer.parseInt(params.get("max_results"));
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid paging values!", e);
        }

        if (pageStart < 0 || maxResults <= 0 || maxResults > 256) {
            throw new BadRequestException("Invalid paging values!");
        }


        var numResults = new AtomicInteger(); // Num of replies in total
        var replies = review.getReplies()
                .peek(i -> numResults.incrementAndGet()) // Get reply number
                .sorted(Comparator.comparing(Comment::getCommentTime).reversed()) // Sort by time
                .skip(pageStart) // Skip to page
                .limit(maxResults) // Limit to page size
                .map(Comment::makeSerialisedReply) // Make reply
                .collect(Collectors.toList()); // Collect to list

        return new RepliesViewResponse(replies, numResults.get());
    }

    public UserEventsResponse userEvents (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Invalid paging details!");
        }

        var pageStart = Integer.parseInt(params.get("page_start"));
        var maxResults = Integer.parseInt(params.get("max_results"));

        if (pageStart < 0 || maxResults <= 0) {
            throw new BadRequestException("Invalid paging values!");
        }

        ZonedDateTime beforeDate;
        if (params.get("before") != null) {
            try {
                beforeDate = ZonedDateTime.parse(params.get("before"), DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid date time string!");
            }
            if (beforeDate.isBefore(ZonedDateTime.now(ZoneId.of("UTC")))) {
                throw new ForbiddenException("Cannot find events in the past!");
            }

        } else {
            beforeDate = null;
        }

        List<Event> events = session.getAll(Event.class);

        var numResults = new AtomicInteger();

        var eventIds = events.stream()
                .filter(e -> e.endsBefore(beforeDate) && e.getEventStart().isAfter(ZonedDateTime.now(ZoneId.of("UTC")))) // Filter by date
                .filter(Event::isPublished) // Ensure is published
                .peek(i -> numResults.incrementAndGet()) // Get number of items
                .sorted(Comparator.comparing(Event::getEventStart)) // Sort by start date
                .skip(pageStart)
                .limit(maxResults)
                .map(Event::getId)
                .map(UUID::toString)
                .collect(Collectors.toList());
        
        return new UserEventsResponse(eventIds, numResults.get());
    }

    public UserEventsResponse userEventsPast (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Invalid paging details!");
        }

        var pageStart = Integer.parseInt(params.get("page_start"));
        var maxResults = Integer.parseInt(params.get("max_results"));

        if (pageStart < 0 || maxResults <= 0) {
            throw new BadRequestException("Invalid paging values!");
        }

        List<Event> events = session.getAll(Event.class);

        ZonedDateTime afterDate;
        if (params.get("after") != null) {
            try {
                afterDate = ZonedDateTime.parse(params.get("after"), DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid date time string!");
            }
        } else {
            afterDate = null;
        }

        var numResults = new AtomicInteger();

        var eventIds = events.stream()
                .filter(e -> e.startsAfter(afterDate) && e.getEventStart().isBefore(ZonedDateTime.now(ZoneId.of("UTC"))))
                .filter(Event::isPublished)
                .peek(i -> numResults.incrementAndGet())
                .sorted(Comparator.comparing(Event::getEventStart))
                .skip(pageStart)
                .limit(maxResults)
                .map(Event::getId)
                .map(UUID::toString)
                .collect(Collectors.toList());
        
        return new UserEventsResponse(eventIds, numResults.get());
    }

    public EventReservedSeatsResponse eventReservedSeats (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("auth_token")) {
            throw new BadRequestException("Missing auth_token!");
        }
        if (!params.containsKey("event_id")) {
            throw new BadRequestException("Missing event ID!");
        }
        authenticateToken(session, params.get("auth_token"));

        Event event = session.getById(Event.class, UUID.fromString(params.get("event_id")))
                .orElseThrow(() -> new ForbiddenException("Invalid event ID!"));

        return new EventReservedSeatsResponse(event.getReservedSeats());
    }

    public EventHostingsResponse eventHostings (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("auth_token")) {
            throw new BadRequestException("Missing auth_token!");
        }
        if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Invalid paging details!");
        }
        var pageStart = Integer.parseInt(params.get("page_start"));
        var maxResults = Integer.parseInt(params.get("max_results"));
        if (pageStart < 0 || maxResults <= 0) {
            throw new BadRequestException("Invalid paging values!");
        }

        User user = authenticateToken(session, params.get("auth_token"));

        ZonedDateTime beforeDate;
        if (params.get("before") != null) {
            try {
                beforeDate = ZonedDateTime.parse(params.get("before"), DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid date time string!");
            }
        } else {
            beforeDate = null;
        }

        var numResults = new AtomicInteger();
        var eventIds = user.getStreamHostingEvents()
                .filter(e -> e.endsBefore(beforeDate) && e.getEventStart().isAfter(ZonedDateTime.now(ZoneId.of("UTC"))))
                .peek(i -> numResults.incrementAndGet())
                .sorted(Comparator.comparing(Event::getEventStart))
                .skip(pageStart)
                .limit(maxResults)
                .map(Event::getId)
                .map(UUID::toString)
                .collect(Collectors.toList());

        return new EventHostingsResponse(eventIds, numResults.get());
    }

    public EventHostingsResponse eventHostingsPast (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("auth_token")) {
            throw new BadRequestException("Missing auth_token!");
        }
        if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Invalid paging details!");
        }
        var pageStart = Integer.parseInt(params.get("page_start"));
        var maxResults = Integer.parseInt(params.get("max_results"));
        if (pageStart < 0 || maxResults <= 0) {
            throw new BadRequestException("Invalid paging values!");
        }

        User user = authenticateToken(session, params.get("auth_token"));

        ZonedDateTime afterDate;
        if (params.get("after") != null) {
            try {
                afterDate = ZonedDateTime.parse(params.get("after"), DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid date time string!");
            }
        } else {
            afterDate = null;
        }

        var numResults = new AtomicInteger();
        var eventIds = user.getStreamHostingEvents()
                .filter(afterDate != null 
                    ? e -> e.getEventStart().isAfter(afterDate) && e.getEventStart().isBefore(ZonedDateTime.now(ZoneId.of("UTC"))) 
                    : e -> e.getEventStart().isBefore(ZonedDateTime.now(ZoneId.of("UTC")))
                )
                .peek(i -> numResults.incrementAndGet())
                .sorted(Comparator.comparing(Event::getEventStart))
                .skip(pageStart)
                .limit(maxResults)
                .map(Event::getId)
                .map(UUID::toString)
                .collect(Collectors.toList());

        return new EventHostingsResponse(eventIds, numResults.get());
    }

    public CustomerEventsResponse customerBookings (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("auth_token")) {
            throw new BadRequestException("Missing auth_token!");
        }
        if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Invalid paging details!");
        }

        User user = authenticateToken(session, params.get("auth_token"));

        var pageStart = Integer.parseInt(params.get("page_start"));
        var maxResults = Integer.parseInt(params.get("max_results"));
        if (pageStart < 0 || maxResults <= 0) {
            throw new BadRequestException("Invalid paging values!");
        }

        ZonedDateTime beforeDate;
        if (params.get("before") != null) {
            try {
                beforeDate = ZonedDateTime.parse(params.get("before"), DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid date time string!");
            }
        } else {
            beforeDate = null;
        }

        var numResults = new AtomicInteger();
        var eventIds = user.getTickets().stream()
                .map(Ticket::getEvent)
                .filter(beforeDate != null 
                    ? e -> e.getEventStart().isBefore(beforeDate) && e.getEventStart().isAfter(ZonedDateTime.now(ZoneId.of("UTC"))) 
                    : e -> e.getEventStart().isAfter(ZonedDateTime.now(ZoneId.of("UTC")))
                )
                .distinct()
                .peek(i -> numResults.incrementAndGet())
                .sorted(Comparator.comparing(Event::getEventStart))
                .skip(pageStart)
                .limit(maxResults)
                .map(Event::getId)
                .map(UUID::toString)
                .collect(Collectors.toList());

        
        return new CustomerEventsResponse(eventIds, numResults.get());
    }

    public CustomerEventsResponse customerBookingsPast (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("auth_token")) {
            throw new BadRequestException("Missing auth_token!");
        }
        if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Invalid paging details!");
        }

        User user = authenticateToken(session, params.get("auth_token"));

        var pageStart = Integer.parseInt(params.get("page_start"));
        var maxResults = Integer.parseInt(params.get("max_results"));
        if (pageStart < 0 || maxResults <= 0) {
            throw new BadRequestException("Invalid paging values!");
        }

        ZonedDateTime afterDate;
        if (params.get("after") != null) {
            try {
                afterDate = ZonedDateTime.parse(params.get("after"), DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid date time string!");
            }
        } else {
            afterDate = null;
        }

        var numResults = new AtomicInteger();
        var eventIds = user.getTickets().stream()
                .map(Ticket::getEvent)
                .filter(afterDate != null 
                    ? e -> e.getEventStart().isAfter(afterDate) && e.getEventStart().isBefore(ZonedDateTime.now(ZoneId.of("UTC"))) 
                    : e -> e.getEventStart().isBefore(ZonedDateTime.now(ZoneId.of("UTC")))
                )
                .distinct()
                .peek(i -> numResults.incrementAndGet())
                .sorted(Comparator.comparing(Event::getEventStart))
                .skip(pageStart)
                .limit(maxResults)
                .map(Event::getId)
                .map(UUID::toString)
                .collect(Collectors.toList());

        
        return new CustomerEventsResponse(eventIds, numResults.get());
    }

    public EventHostingFutureResponse eventHostingFuture (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("email")) {
            throw new BadRequestException("Missing email!");
        }
        if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Invalid paging details!");
        }

        User user = session.getByUnique(User.class, "email", params.get("email"))
                .orElseThrow(() -> new ForbiddenException("Invalid email!"));

        var pageStart = Integer.parseInt(params.get("page_start"));
        var maxResults = Integer.parseInt(params.get("max_results"));
        if (pageStart < 0 || maxResults <= 0) {
            throw new BadRequestException("Invalid paging values!");
        }

        var numResults = new AtomicInteger();
        var eventIds = user.getStreamHostingEvents()
                .filter(e -> e.getEventStart().isAfter(ZonedDateTime.now(ZoneId.of("UTC"))))
                .peek(i -> numResults.incrementAndGet())
                .sorted(Comparator.comparing(Event::getEventStart))
                .skip(pageStart)
                .limit(maxResults)
                .map(Event::getId)
                .map(UUID::toString)
                .collect(Collectors.toList());
        return new EventHostingFutureResponse(eventIds, numResults.get());
    }
    
    public EventHostingPastResponse eventHostingPast (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("email")) {
            throw new BadRequestException("Missing email!");
        }
        if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Invalid paging details!");
        }

        User user = session.getByUnique(User.class, "email", params.get("email"))
                .orElseThrow(() -> new ForbiddenException("Invalid email!"));

        var pageStart = Integer.parseInt(params.get("page_start"));
        var maxResults = Integer.parseInt(params.get("max_results"));
        if (pageStart < 0 || maxResults <= 0) {
            throw new BadRequestException("Invalid paging values!");
        }

        var numResults = new AtomicInteger();
        var eventIds = user.getStreamHostingEvents()
                .filter(e -> e.getEventStart().isBefore(ZonedDateTime.now(ZoneId.of("UTC"))))
                .peek(i -> numResults.incrementAndGet())
                .sorted(Comparator.comparing(Event::getEventStart))
                .skip(pageStart)
                .limit(maxResults)
                .map(Event::getId)
                .map(UUID::toString)
                .collect(Collectors.toList());
        return new EventHostingPastResponse(eventIds, numResults.get());
    }
    
    public void commentReact (ModelSession session, ReactRequest request) {
        var user = authenticateToken(session, request.authToken);
        if (request.commentId == null || request.reactType == null) {
            throw new BadRequestException("Missing comment id or react type!");
        }

        var comment = session.getById(Comment.class, UUID.fromString(request.commentId))
                .orElseThrow(() -> new ForbiddenException("Invalid comment id!"));

        comment.react(session, user, request.reactType);

        if (!comment.getEvent().getHost().equals(user)) {
            // Add reaction interaction
            RecommenderEngine.recordInteraction(session, user, comment.getEvent(), InteractionType.REACT);
        }
    }

    public void makeAnnouncement (ModelSession session, AnnouncementRequest request) {
        var user = authenticateToken(session, request.authToken);
        if (request.eventId == null) {
            throw new BadRequestException("Missing event id!");
        }

        var event = session.getById(Event.class, parseUUID(request.eventId))
                .orElseThrow(() -> new ForbiddenException("Invalid event id!"));

        event.makeAnnouncement(user, request.announcement);
    }

    public void onPaymentCancel (ModelSession session, Map<String, String> params) {
        var orderId = params.get("order_id");

       ticketPurchaseCancel(session, orderId);
    }

    public void reviewDelete (ModelSession session, ReviewDeleteRequest request) {
        if (request.commentId == null) {
            throw new BadRequestException("Missing comment ID!");
        }
        User user = authenticateToken(session, request.authToken);

        Comment review = session.getById(Comment.class, UUID.fromString(request.commentId))
                .orElseThrow(() -> new ForbiddenException("Invalid comment ID!"));

        if (!user.equals(review.getAuthor())) {
            throw new ForbiddenException("User is not author of this review!");
        }

        session.remove(review);
    }   

    public GroupCreateResponse groupCreate (ModelSession session, GroupCreateRequest request) {
        if (request.reservedIds == null) {
            throw new BadRequestException("Missing reserved ids!");
        }
        if (request.hostReserveId == null) {
            throw new BadRequestException("Missing host reserve id!");
        }
        User user = authenticateToken(session, request.authToken);

        var reservations = request.reservedIds.stream()
                .map(id -> session.getById(TicketReservation.class, parseUUID(id))
                        .orElseThrow(() -> new ForbiddenException("Invalid reservation id!")))
                .collect(Collectors.toSet());

        if (!reservations.stream().allMatch(r -> r.ownedBy(user)) || reservations.stream().anyMatch(TicketReservation::inGroup)) {
            throw new ForbiddenException("Cannot add this reservation to a group!");
        }

        TicketReservation reserve = session.getById(TicketReservation.class, parseUUID(request.hostReserveId))
                .orElseThrow(() -> new ForbiddenException("Reserve ID does not exist!"));

        if (!reservations.contains(reserve)) {
            throw new ForbiddenException("Invalid host reserve id!");
        }

        Group group = new Group(user, ZonedDateTime.now(ZoneId.of("UTC")), 1, reservations);

        session.save(group);
        reserve.setGroupAccepted(true);
        
        return new GroupCreateResponse(group.getId().toString());
    }

    public GroupIdsResponse getGroupIds (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("auth_token")) {
            throw new BadRequestException("Missing auth token!!");
        }
        if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Missing paging details!");
        }
        User user = authenticateToken(session, params.get("auth_token"));

        var pageStart = Integer.parseInt(params.get("page_start"));
        var maxResults = Integer.parseInt(params.get("max_results"));
        if (pageStart < 0 || maxResults <= 0) {
            throw new BadRequestException("Invalid paging values!");
        }

        var numResults = new AtomicInteger();
        var groupIds = user.getGroups().stream()
                .peek(i -> numResults.incrementAndGet())
                .skip(pageStart)
                .limit(maxResults)
                .map(Group::getId)
                .map(UUID::toString)
                .collect(Collectors.toList());
        return new GroupIdsResponse(groupIds, numResults.get());
    }

    public GroupInviteResponse groupInvite(ModelSession session, GroupInviteRequest request) {
        if (request.groupId == null) {
            throw new BadRequestException("Invalid group ID!");
        }
        if (request.reserveId == null) {
            throw new BadRequestException("Invalid reserve ID!");
        }
        if (request.email == null || !EMAIL_REGEX.matcher(request.email.trim().toLowerCase()).matches()) {
            throw new BadRequestException("Invalid Email!");
        }

        User user = authenticateToken(session, request.authToken);

        Group group = session.getById(Group.class, UUID.fromString(request.groupId))
                .orElseThrow(() -> new BadRequestException("Group ID does not exist!"));

        TicketReservation reserve = session.getById(TicketReservation.class, UUID.fromString(request.reserveId))
                .orElseThrow(() -> new BadRequestException("Reserve ID does not exist!"));

        if (reserve.isGroupAccepted()) {
            throw new BadRequestException("Cannot send invitation for a reserve ID that has been accepted!");
        }

        User inviteUser = session.getByUnique(User.class, "email", request.email)
                .orElseThrow(() -> new BadRequestException("User with email does not exist!"));
        if (user.equals(inviteUser)) {
            throw new BadRequestException("Host cannot send invite to themself!");
        }

        reserve.setExpiry(ZonedDateTime.now(ZoneId.of("UTC")).plus(Duration.ofHours(24)));

        Invitation invitation;
        if (reserve.getInvitation() == null) {
            invitation = new Invitation(group, reserve, inviteUser);
            session.save(invitation);
            invitation.handleInvitation(group, reserve, inviteUser);
        } else {
            invitation = session.getByUnique(Invitation.class, "ticketReservation", reserve)
                    .orElseThrow(() -> new BadRequestException("Invitation does not exist for this reserve ID!"));
            if (!invitation.getUser().equals(inviteUser)) {
                throw new BadRequestException("Invitation has already been sent to another user!");
            }
        }

        // logger.info("{}", invitation.getId().toString());

        var inviteUrl = String.format("http://localhost:3000/ticket/purchase/group/%s", invitation.getId().toString());
        var message = String.format("Please click below to view your group invitation <a href=\"%s\">here</a>.\n", inviteUrl);
        ApiLocator.locateApi(IEmailAPI.class).sendEmail(request.email, "User group invitation", message);

        return new GroupInviteResponse(true);
    }

    public GroupAcceptResponse groupAccept(ModelSession session, GroupAcceptRequest request) {
        if (request.inviteId == null) {
            throw new BadRequestException("Invalid invite ID!");
        }
        User user = authenticateToken(session, request.authToken);
        Invitation invitation = session.getById(Invitation.class, UUID.fromString(request.inviteId))
                .orElseThrow(() -> new BadRequestException("Invitation does not exist for this invite ID!"));

        invitation.acceptInvitation(user);
        session.remove(invitation);
        return new GroupAcceptResponse(invitation.getTicketReservation().getId().toString());
    }
    
    public void groupDeny(ModelSession session, GroupDenyRequest request) {
        if (request.inviteId == null) {
            throw new BadRequestException("Invalid invite ID!");
        }
        Invitation invitation = session.getById(Invitation.class, UUID.fromString(request.inviteId))
                .orElseThrow(() -> new BadRequestException("Invitation does not exist for this invite ID!"));

        invitation.denyInvitation();
        session.remove(invitation);
    }

    public GroupDetailsResponse groupDetails(ModelSession session, Map<String, String> params) {
        if (!params.containsKey("auth_token")) {
            throw new BadRequestException("Missing auth token!!");
        }
        if (!params.containsKey("group_id")) {
            throw new BadRequestException("Missing group ID!");
        }
        User host = authenticateToken(session, params.get("auth_token"));
        Group group = session.getById(Group.class, UUID.fromString(params.get("group_id")))
                .orElseThrow(() -> new ForbiddenException("Group ID doesn't exist!"));

        return group.getGroupDetailsResponse(host);
    }

    public RecommenderResponse recommendEventEvent (ModelSession session, Map<String, String> params) {
        // Do event-event recommendations e.g. on unlogged event view
        if (!params.containsKey("event_id")) {
            throw new BadRequestException("Missing event id!");
        }

        if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Missing paging details!");
        }

        var event = session.getById(Event.class, parseUUID(params.get("event_id")))
                .orElseThrow(() -> new ForbiddenException("Invalid event id!"));

        int pageStart = 0;
        int maxResults = 0;
        try {
            pageStart = Integer.parseInt(params.get("page_start"));
            maxResults = Integer.parseInt(params.get("max_results"));
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid paging details!", e);
        }

        if (pageStart < 0 || maxResults <= 0 || maxResults > 256) {
            throw new BadRequestException("Invalid paging values!");
        }

        var returnNum = new AtomicInteger();
        var recommendEvents = session.getAllStream(Event.class)
                .filter(e -> !e.equals(event)) // Cannot recommend own event
                .filter(Event::isPublished) // Must recommend published events
                .filter(e -> !e.getEventEnd().isBefore(ZonedDateTime.now())) // Cannot recommend events in the past
                .map(e -> new Pair<>(e.getId().toString(), RecommenderEngine.calculateSimilarity(session, event, e))) // Make event, similarity pairs
                .peek(p -> returnNum.getAndIncrement()) // Get number
                .sorted(Comparator.comparingDouble((Pair<String, Double> p) -> p.getSecond()).reversed()) // Sort by descending similarity
                .skip(pageStart) // Skip to page start
                .limit(maxResults) // Limit results
                .map(p -> new RecommenderResponse.Event(p.getFirst(), p.getSecond())) // Make recommendation response events
                .collect(Collectors.toList());

        return new RecommenderResponse(recommendEvents, returnNum.get());
    }

    public RecommenderResponse recommendUserEvent (ModelSession session, Map<String, String> params) {
        // Do user-event recommendations e.g. on home page recommend events
        if (!params.containsKey("auth_token")) {
            throw new UnauthorizedException("Missing auth token!");
        } else if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Missing paging details!");
        }

        var user = authenticateToken(session, params.get("auth_token"));

        int pageStart = 0;
        int maxResults = 0;
        try {
            pageStart = Integer.parseInt(params.get("page_start"));
            maxResults = Integer.parseInt(params.get("max_results"));
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid paging details!", e);
        }

        if (pageStart < 0 || maxResults <= 0 || maxResults > 256) {
            throw new BadRequestException("Invalid paging values!");
        }

        var profileVector = RecommenderEngine.buildUserProfile(session, user);
        var eventNum = new AtomicInteger();
        var recommendEvents = session.getAllStream(Event.class)
                .filter(e -> !e.getHost().equals(user)) // Cannot recommend hosted events
                .filter(Event::isPublished) // Must recommend published events
                .filter(e -> !e.getEventEnd().isBefore(ZonedDateTime.now())) // Cannot recommend events in the past
                .map(e -> new Pair<>(e.getId().toString(), RecommenderEngine.calculateUserScore(session, e, profileVector))) // Make event, score pairs
                .peek(p -> eventNum.getAndIncrement()) // Get number
                .sorted(Comparator.comparingDouble((Pair<String, Double> p) -> p.getSecond()).reversed()) // Sort by decreasing score
                .skip(pageStart) // Skip to start
                .limit(maxResults) // Limit to expected result number
                .map(p -> new RecommenderResponse.Event(p.getFirst(), p.getSecond())) // Map to response event
                .collect(Collectors.toList());

        return new RecommenderResponse(recommendEvents, eventNum.get());
    }

    public RecommenderResponse recommendEventUserEvent (ModelSession session, Map<String, String> params) {
        // Recommend based on both event-event and user-event e.g. when viewing a page while logged in
        // events will be recommended based on both event-event and user-event
        if (!params.containsKey("auth_token")) {
            throw new UnauthorizedException("Missing auth token!");
        } else if (!params.containsKey("event_id")) {
            throw new BadRequestException("Missing event id!");
        } else if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Missing paging details!");
        }

        var user = authenticateToken(session, params.get("auth_token"));
        var event = session.getById(Event.class, parseUUID(params.get("event_id")))
                .orElseThrow(() -> new ForbiddenException("Invalid event id!"));

        int pageStart = 0;
        int maxResults = 0;
        try {
            pageStart = Integer.parseInt(params.get("page_start"));
            maxResults = Integer.parseInt(params.get("max_results"));
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid paging details!", e);
        }

        if (pageStart < 0 || maxResults <= 0 || maxResults > 256) {
            throw new BadRequestException("Invalid paging values!");
        }

        var profileVector = RecommenderEngine.buildUserProfile(session, user);
        var eventNum = new AtomicInteger();
        var recommendEvents = session.getAllStream(Event.class)
                .filter(e -> !e.getId().equals(event.getId())) // Cannot recommend own event
                .filter(e -> !e.getHost().equals(user)) // Cannot recommend hosted events
                .filter(Event::isPublished) // Must recommend published events
                .filter(e -> !e.getEventEnd().isBefore(ZonedDateTime.now())) // Cannot recommend events in the past
                .map(e -> new Pair<>(e.getId().toString(), RecommenderEngine.calculateUserEventScore(session, e, event, profileVector))) // Make event, score pairs
                .peek(p -> eventNum.getAndIncrement()) // Get total number
                .sorted(Comparator.comparingDouble((Pair<String, Double> p) -> p.getSecond()).reversed()) // Sort by descending score
                .skip(pageStart) // Skip to start
                .limit(maxResults) // Limit to expected result number
                .map(p -> new RecommenderResponse.Event(p.getFirst(), p.getSecond())) // Map to response events
                .collect(Collectors.toList());

        return new RecommenderResponse(recommendEvents, eventNum.get());
    }

    public void clearDatabase (ModelSession session, Object request) {
        logger.info("Clearing database!");
        session.clear(AuthToken.class);
        session.clear(Category.class);
        session.clear(Comment.class);
        session.clear(Event.class);
        session.clear(Group.class);
        session.clear(Location.class);
        session.clear(PurchaseItem.class);
        session.clear(Reaction.class);
        session.clear(ResetToken.class);
        session.clear(SeatingPlan.class);
        session.clear(Tag.class);
        session.clear(Ticket.class);
        session.clear(TicketReservation.class);
        session.clear(User.class);
        session.clear(Invitation.class);
        session.clear(DocumentTerm.class);
        session.clear(TfIdf.class);
    }

    public void groupRemoveMember (ModelSession session, GroupRemoveMemberRequest request) {
        if (request.groupId == null) {
            throw new BadRequestException("Invalid group ID!");
        }
        if (request.authToken == null) {
            throw new BadRequestException("Invalid auth token!");
        }
        if (request.email == null || !EMAIL_REGEX.matcher(request.email.trim().toLowerCase()).matches()) {
            throw new BadRequestException("Invalid Email!");
        }

        Group group = session.getById(Group.class, UUID.fromString(request.groupId))
                .orElseThrow(() -> new ForbiddenException("Group does not exist!"));

        User leader = authenticateToken(session, request.authToken);

        User removeUser = session.getByUnique(User.class, "email", request.email)
                .orElseThrow(() -> new ForbiddenException(String.format("User with email %s does not exist!", request.email)));
        group.removeUser(leader, removeUser);
    }

    public void groupCancel (ModelSession session, GroupCancelRequest request) {
        if (request.groupId == null) {
            throw new BadRequestException("Invalid group ID!");
        }
        if (request.authToken == null) {
            throw new BadRequestException("Invalid auth token!");
        }

        Group group = session.getById(Group.class, UUID.fromString(request.groupId))
                .orElseThrow(() -> new ForbiddenException("Group does not exist!"));

        User leader = authenticateToken(session, request.authToken);

        if (!leader.equals(group.getLeader())) {
            throw new BadRequestException("Only the group leader can cancel the group!");
        }
        session.remove(group);
    }

    public void groupRemoveInvite (ModelSession session, GroupRemoveInviteRequest request) {
        if (request.authToken == null) {
            throw new BadRequestException("Invalid auth token!");
        }
        if (request.groupId == null) {
            throw new BadRequestException("Invalid group ID!");
        }
        if (request.inviteId == null) {
            throw new BadRequestException("Invalid invite ID!");
        }

        User user = authenticateToken(session, request.authToken);

        Group group = session.getById(Group.class, UUID.fromString(request.groupId))
                .orElseThrow(() -> new ForbiddenException("Group does not exist!"));

        if (!user.equals(group.getLeader())) {
            throw new BadRequestException("Only the group leader can remove invites!");               
        }

        groupDeny(session, new GroupDenyRequest(request.inviteId));
    }   

    public ReserveDetailsResponse getReserveDetails (ModelSession session, Map<String, String> params) {
        if (params.get("reserve_id") == null) {
            throw new BadRequestException("Missing reserve ID!");
        }

        TicketReservation reserve = session.getById(TicketReservation.class, UUID.fromString(params.get("reserve_id")))
                .orElseThrow(() -> new ForbiddenException("Ticket reservation does not exist!"));
        
        return reserve.getReserveDetailsResponse();
    }

     // changes the notifcations for the user for an event
     public void eventNotificationsUpdate (ModelSession session, EventNotificationsUpdateRequest request) {
        if (request.authToken == null) {
            throw new UnauthorizedException("Missing auth token!");
        }

        Event event = session.getById(Event.class, parseUUID(request.eventId))
                .orElseThrow(() -> new ForbiddenException("Invalid event id!"));
        var user = authenticateToken(session, request.authToken);

        event.editNotificationMembers(session, user, request.notifications);
        user.editEventNotifications(session, event, request.notifications);
    }

    // checks the notifications of user for an event
    public EventNotificationsResponse checkEventNotifications (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("event_id")) {
            throw new BadRequestException("Missing event_id!");
        }
        Event event = session.getById(Event.class, UUID.fromString(params.get("event_id")))
                        .orElseThrow(() -> new ForbiddenException("Unknown event"));

        User user = null;
        if (params.containsKey("auth_token")) {
            user = authenticateToken(session, params.get("auth_token"));
        } else {
            throw new BadRequestException("Missing auth token!");
        }
        var notification = user.doReminders();

        if (event.getNotificationMembers().contains(user)) {
            if (user.getNotificationEvents().contains(event)) {
                notification = true;
            } 
        } else {
            notification = false;
        }

        return new EventNotificationsResponse(notification);
    }

    public void ticketRefund (ModelSession session, TicketRefundRequest request) {
        var user = authenticateToken(session, request.authToken);

        var ticket = session.getById(Ticket.class, parseUUID(request.ticketId))
                .orElseThrow(() -> new ForbiddenException("Invalid ticket id!"));

        ticket.refund(user);
        session.remove(ticket);
    }

    public CategoriesResponse categoriesList (ModelSession session) {
        return new CategoriesResponse(Category.getValidCategories());
    }

    public CategoryEventsResponse eventsByCategory (ModelSession session, Map<String, String> params) {
        if (!params.containsKey("category")) {
            throw new BadRequestException("Missing category!");
        } else if (!params.containsKey("page_start") || !params.containsKey("max_results")) {
            throw new BadRequestException("Missing paging values!");
        }

        var category = params.get("category");
        if (!Category.validCategory(category)) {
            throw new ForbiddenException("Invalid category: \"" + category + "\" (ensure case is correct!)");
        }

        int pageStart = 0;
        int maxResults = 0;
        try {
            pageStart = Integer.parseInt(params.get("page_start"));
            maxResults = Integer.parseInt(params.get("max_results"));
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid paging details!", e);
        }

        if (pageStart < 0 || maxResults <= 0 || maxResults > 256) {
            throw new BadRequestException("Invalid paging values!");
        }

        var eventCount = new AtomicInteger();
        var events = session.getAllWithStream(Category.class, "category", category) // Get all Category records with correct category
                .map(Category::getEvent) // Map to event
                .filter(Event::isPublished) // Only return published events
                .filter(e -> e.getEventEnd().isAfter(ZonedDateTime.now(ZoneId.of("UTC")))) // Only return events in the future
                .peek(c -> eventCount.getAndIncrement()) // Get total
                .sorted(Comparator.comparing(Event::getEventStart)) // Sort by event start date
                .skip(pageStart) // Skip to page start
                .limit(maxResults) // Limit to expected results
                .map(Event::getId) // Convert to ids
                .map(UUID::toString)
                .collect(Collectors.toList());
        return new CategoryEventsResponse(events, eventCount.get());
    }
}
