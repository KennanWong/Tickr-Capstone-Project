package tickr.integration.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Spark;
import tickr.CreateEventReqBuilder;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.serialised.combined.event.EventSearch;
import tickr.application.serialised.requests.event.EditEventRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.application.serialised.responses.user.AuthTokenResponse;
import tickr.application.serialised.responses.event.CreateEventResponse;
import tickr.mock.MockLocationApi;
import tickr.util.HTTPHelper;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.server.Server;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEventSearch {
    private DataModel hibernateModel;
    private HTTPHelper httpHelper;
    private String authToken;

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
    }

    @AfterEach
    public void cleanup () {
        Spark.stop();
        hibernateModel.cleanup();
        Spark.awaitStop();
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test
    public void testNoOptions () {
        var searchResponse = makeSearch(0, 100, null);
        assertEquals(0, searchResponse.numResults);


        var response = httpHelper.post("/api/event/create", new CreateEventReqBuilder().build(authToken));
        assertEquals(200, response.getStatus());
        var eventId = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(eventId, authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        searchResponse = makeSearch(0, 100, null);
        assertEquals(1, searchResponse.numResults);
        assertEquals(eventId, searchResponse.eventIds.get(0));
    }

    @Test
    public void testSearchOptions () {
        var response = httpHelper.post("/api/event/create", new CreateEventReqBuilder()
                .withTags(Set.of("test1", "test2")).build(authToken));
        assertEquals(200, response.getStatus());
        var e1 = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(e1, authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        response = httpHelper.post("/api/event/create", new CreateEventReqBuilder()
                .withDescription("pizza apples").build(authToken));
        assertEquals(200, response.getStatus());
        var e2 = response.getBody(CreateEventResponse.class).event_id;

        response = httpHelper.put("/api/event/edit", new EditEventRequest(e2, authToken, null, null, null, null,
                null, null, null, null, null, null, true, null));
        assertEquals(200, response.getStatus());

        var ids = makeSearch(0, 100,
                new EventSearch.Options(null, null, null, null, List.of("test1"), null, null)).eventIds;
        assertEquals(1, ids.size());
        assertEquals(e1, ids.get(0));

        ids = makeSearch(0, 100,
                new EventSearch.Options(null, null, null, null, null, null, "aPpLeS pears")).eventIds;
        assertEquals(1, ids.size());
        assertEquals(e2, ids.get(0));

        ids = makeSearch(0, 100,
                new EventSearch.Options(null, null, null, null, null, null, "money money money")).eventIds;
        assertEquals(0, ids.size());
    }



    private EventSearch.Response makeSearch (int pageStart, int maxResults, EventSearch.Options options) {
        var paramsMap = new HashMap<String, String>();

        paramsMap.put("page_start", Integer.toString(pageStart));
        paramsMap.put("max_results", Integer.toString(maxResults));

        if (options != null) {
            paramsMap.put("search_options", options.serialise());
        }

        //var response = controller.searchEvents(session, paramsMap);
        var response = httpHelper.get("/api/event/search", paramsMap);
        assertEquals(200, response.getStatus());

        return response.getBody(EventSearch.Response.class);
    }
}
