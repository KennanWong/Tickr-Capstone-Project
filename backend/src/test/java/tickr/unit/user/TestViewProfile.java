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
import tickr.server.exceptions.BadRequestException;
import tickr.server.exceptions.ForbiddenException;
import tickr.server.exceptions.UnauthorizedException;
import tickr.util.CryptoHelper;

import java.util.Map;
import java.util.UUID;

public class TestViewProfile {
    private DataModel model;
    private TickrController controller;
    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
    }

    @Test
    public void testBadRequest () {
        var session = model.makeSession();

        var authToken = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
        session = TestHelper.commitMakeSession(model, session);
        var userId = CryptoHelper.makeJWTParserBuilder()
                .build()
                .parseClaimsJws(authToken)
                .getBody()
                .getSubject();

        var finalSession = session;
        assertThrows(BadRequestException.class, () -> controller.userGetProfile(finalSession, Map.of()));
        assertThrows(BadRequestException.class, () -> controller.userGetProfile(finalSession, Map.of("user_id", userId, "auth_token", authToken)));

        assertThrows(ForbiddenException.class, () -> controller.userGetProfile(finalSession, Map.of("user_id", UUID.randomUUID().toString())));
        assertThrows(UnauthorizedException.class, () -> controller.userGetProfile(finalSession,
                Map.of("auth_token", TestHelper.makeFakeJWT(UUID.fromString(userId)))));
    }

    @Test
    public void testOwnProfile () {
        var session = model.makeSession();
        var authToken = controller.userRegister(session, new UserRegisterRequest("test_username", "TestFirst", "TestLast",
                "test@example.com", "Testing123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        var response = controller.userGetProfile(session, Map.of("auth_token", authToken));

        assertEquals("test_username", response.userName);
        assertEquals("TestFirst", response.firstName);
        assertEquals("TestLast", response.lastName);
        assertEquals("test@example.com", response.email);
        assertEquals("", response.description);
        assertEquals("", response.profilePicture); // TODO
    }

    @Test
    public void testOtherProfile () {
        var session = model.makeSession();
        var authToken = controller.userRegister(session, new UserRegisterRequest("test_username", "TestFirst", "TestLast",
                "test@example.com", "Testing123!", "2022-04-14")).authToken;
        session = TestHelper.commitMakeSession(model, session);

        var userId = CryptoHelper.makeJWTParserBuilder()
                .build()
                .parseClaimsJws(authToken)
                .getBody()
                .getSubject();

        var response = controller.userGetProfile(session, Map.of("user_id", userId));

        assertEquals("test_username", response.userName);
        assertEquals("TestFirst", response.firstName);
        assertEquals("TestLast", response.lastName);
        assertEquals("test@example.com", response.email);
        assertEquals("", response.description);
        assertEquals("", response.profilePicture); // TODO
    }
}
