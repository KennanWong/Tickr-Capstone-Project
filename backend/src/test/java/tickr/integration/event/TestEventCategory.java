package tickr.integration.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Spark;
import tickr.CreateEventReqBuilder;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.serialised.SerializedLocation;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.application.serialised.responses.event.CategoriesResponse;
import tickr.application.serialised.responses.event.CategoryEventsResponse;
import tickr.application.serialised.responses.event.CreateEventResponse;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;
import tickr.util.HTTPHelper;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEventCategory {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;
    private String authToken;

    private List<String> eventIds;

    @BeforeEach
    public void setup () {
        hibernateModel = new HibernateModel("hibernate-test.cfg.xml");
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(hibernateModel));

        Server.start(8080, null, hibernateModel);
        httpHelper = new HTTPHelper("http://localhost:8080");
        Spark.awaitInitialization();

        var response = httpHelper.post("/api/user/register", new UserRegisterRequest("TestUsername", "Test", "User", "test@example.com",
                "Password123!", "2010-10-07"));
        assertEquals(200, response.getStatus());
        authToken = response.getBody(AuthTokenResponse.class).authToken;

        eventIds = addTestEvents();
    }

    @AfterEach
    public void cleanup () {
        Spark.stop();
        hibernateModel.cleanup();
        Spark.awaitStop();
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test
    public void testBadRequests () {
        var response = httpHelper.get("/api/events/category");
        assertEquals(400, response.getStatus());

        response = httpHelper.get("/api/events/category", Map.of("category", "Free"));
        assertEquals(400, response.getStatus());

        response = httpHelper.get("/api/events/category",
                Map.of("category", "Expensive", "page_start", "0", "max_results", "10"));
        assertEquals(403, response.getStatus());

        response = httpHelper.get("/api/events/category",
                Map.of("category", "fREe", "page_start", "0", "max_results", "10"));
        assertEquals(403, response.getStatus());

        response = httpHelper.get("/api/events/category",
                Map.of("category", "Free", "page_start", "-1", "max_results", "10"));
        assertEquals(400, response.getStatus());

        response = httpHelper.get("/api/events/category",
                Map.of("category", "Free", "page_start", "0", "max_results", "0"));
        assertEquals(400, response.getStatus());

        response = httpHelper.get("/api/events/category",
                Map.of("category", "Free", "page_start", "0", "max_results", "-1"));
        assertEquals(400, response.getStatus());

        response = httpHelper.get("/api/events/category",
                Map.of("category", "Free", "page_start", "0", "max_results", "257"));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testCategoriesList () {
        var categories = List.of("Food", "Music", "Travel & Outdoor", "Health", "Sport & Fitness", "Hobbies", "Business", "Free", "Tourism", "Education");
        var response = httpHelper.get("/api/events/categories/list");
        assertEquals(200, response.getStatus());
        assertArrayEquals(categories.toArray(), response.getBody(CategoriesResponse.class).categories.toArray());
    }

    @Test
    public void testCategories () {
        var response = httpHelper.get("/api/events/category",
                Map.of("category", "Food", "page_start", "0", "max_results", "10"));
        assertEquals(200, response.getStatus());
        var catResponse = response.getBody(CategoryEventsResponse.class);
        assertEquals(2, catResponse.numResults);
        assertEquals(2, catResponse.eventIds.size());

        assertEquals(eventIds.get(2), catResponse.eventIds.get(0));
        assertEquals(eventIds.get(0), catResponse.eventIds.get(1));

        response = httpHelper.get("/api/events/category",
                Map.of("category", "Travel & Outdoor", "page_start", "0", "max_results", "10"));
        assertEquals(200, response.getStatus());
        catResponse = response.getBody(CategoryEventsResponse.class);
        assertEquals(1, catResponse.numResults);
        assertEquals(1, catResponse.eventIds.size());

        assertEquals(eventIds.get(0), catResponse.eventIds.get(0));

        response = httpHelper.get("/api/events/category",
                Map.of("category", "Hobbies", "page_start", "0", "max_results", "10"));

        assertEquals(200, response.getStatus());
        catResponse = response.getBody(CategoryEventsResponse.class);
        assertEquals(0, catResponse.numResults);
        assertEquals(0, catResponse.eventIds.size());
    }


    private List<String> addTestEvents () {
        var eventIds = new ArrayList<String>();

        var response = httpHelper.post("/api/event/create", new CreateEventReqBuilder()
                .withEventName("Test event testing")
                .withDescription("apple banana banana cookie testing")
                .withLocation(new SerializedLocation.Builder()
                        .withStreetNo(1)
                        .withStreetName("High St")
                        .withSuburb("Kensington")
                        .withPostcode("2052")
                        .withState("NSW")
                        .withCountry("Australia")
                        .build())
                .withCategories(Set.of("Food", "Travel & Outdoor"))
                .withTags(Set.of("yummy", "delicious", "test"))
                .withStartDate(ZonedDateTime.now().plusDays(5))
                .withEndDate(ZonedDateTime.now().plusDays(6))
                .build(authToken));
        assertEquals(200, response.getStatus());
        eventIds.add(response.getBody(CreateEventResponse.class).event_id);

        response = httpHelper.put("/api/event/edit", new EditEventRequest(eventIds.get(0), authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/event/create", new CreateEventReqBuilder()
                .withEventName("Test three testing")
                .withDescription("metal hydrogen force hydrogen hydrogen hydrogen cookie testing")
                .withLocation(new SerializedLocation.Builder()
                        .withStreetNo(1)
                        .withStreetName("High St")
                        .withSuburb("Kensington")
                        .withPostcode("2052")
                        .withState("NSW")
                        .withCountry("Australia")
                        .build())
                .withCategories(Set.of("Business", "Education"))
                .withTags(Set.of("Science", "Experiment", "Test"))
                .withStartDate(ZonedDateTime.now().plusDays(1))
                .withEndDate(ZonedDateTime.now().plusDays(2))
                .build(authToken));
        assertEquals(200, response.getStatus());
        eventIds.add(response.getBody(CreateEventResponse.class).event_id);

        response = httpHelper.put("/api/event/edit", new EditEventRequest(eventIds.get(1), authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/event/create", new CreateEventReqBuilder()
                .withEventName("sweet testing")
                .withDescription("sugar chocolate cookie jelly banana cookie cookie cookie cookie testing")
                .withLocation(new SerializedLocation.Builder()
                        .withStreetNo(235)
                        .withStreetName("Anzac Parade")
                        .withSuburb("Kensington")
                        .withPostcode("2033")
                        .withState("NSW")
                        .withCountry("Australia")
                        .build())
                .withCategories(Set.of("Food"))
                .withTags(Set.of("sweet", "delicious", "test"))
                .withStartDate(ZonedDateTime.now().plusDays(3))
                .withEndDate(ZonedDateTime.now().plusDays(4))
                .build(authToken));
        assertEquals(200, response.getStatus());
        eventIds.add(response.getBody(CreateEventResponse.class).event_id);

        response = httpHelper.put("/api/event/edit", new EditEventRequest(eventIds.get(2), authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/event/create", new CreateEventReqBuilder()
                .withCategories(Set.of("Food"))
                .build(authToken));
        assertEquals(200, response.getStatus());

        return eventIds;
    }
}
