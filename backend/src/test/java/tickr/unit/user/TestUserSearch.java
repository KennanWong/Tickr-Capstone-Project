package tickr.unit.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.util.CryptoHelper;

import java.util.Map;

public class TestUserSearch {
    private DataModel model;
    private TickrController controller;
    private ModelSession session;
    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        session = model.makeSession();
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
    }

    @Test
    public void testBadRequest () {
        assertThrows(BadRequestException.class, () -> controller.userSearch(session, Map.of()));

        assertThrows(BadRequestException.class, () -> controller.userSearch(session, Map.of("email", "test@")));
        assertThrows(BadRequestException.class, () -> controller.userSearch(session, Map.of("email", "@example.com")));
        assertThrows(BadRequestException.class, () -> controller.userSearch(session, Map.of("email", "test@example")));
    }


    @Test
    public void testNoUser () {
        assertThrows(ForbiddenException.class, () -> controller.userSearch(session, Map.of("email", "test@example.com")));
        controller.userRegister(session, new UserRegisterRequest("test", "Test", "User", "test2@example.com",
                "Password123!", "2000-10-07"));
        session = TestHelper.commitMakeSession(model, session);
        assertThrows(ForbiddenException.class, () -> controller.userSearch(session, Map.of("email", "test@example.com")));
    }

    @Test
    public void testUserSearch () {
        var authToken = controller.userRegister(session, new UserRegisterRequest("test", "Test", "User",
                "test@example.com", "Password123!", "2000-10-07")).authToken;

        var userId = CryptoHelper.makeJWTParserBuilder()
                .build()
                .parseClaimsJws(authToken)
                .getBody()
                .getSubject();

        var searchId = controller.userSearch(session, Map.of("email", "test@example.com")).userId;

        assertEquals(userId, searchId);

        var searchId2 = controller.userSearch(session, Map.of("email", "TEsT@eXamPLe.cOm")).userId;
        assertEquals(userId, searchId2);
    }

    @Test
    public void testTwoUser () {
        var authToken1 = controller.userRegister(session, new UserRegisterRequest("test", "Test", "User",
                "test@example.com", "Password123!", "2000-10-07")).authToken;
        session = TestHelper.commitMakeSession(model, session);


        var authToken2 = controller.userRegister(session, new UserRegisterRequest("test", "Test", "User",
                "test2@example.com", "Password123!", "2000-10-07")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        var userId1 = CryptoHelper.makeJWTParserBuilder()
                .build()
                .parseClaimsJws(authToken1)
                .getBody()
                .getSubject();

        var userId2 = CryptoHelper.makeJWTParserBuilder()
                .build()
                .parseClaimsJws(authToken2)
                .getBody()
                .getSubject();

        var searchId1 = controller.userSearch(session, Map.of("email", "test@example.com")).userId;
        var searchid2 = controller.userSearch(session, Map.of("email", "test2@example.com")).userId;

        assertEquals(userId1, searchId1);
        assertEquals(userId2, searchid2);
    }
}
