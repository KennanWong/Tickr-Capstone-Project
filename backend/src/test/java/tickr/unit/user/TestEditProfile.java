package tickr.unit.user;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.serialised.requests.user.EditProfileRequest;
import tickr.application.serialised.requests.user.UserRegisterRequest;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;
import tickr.server.exceptions.ForbiddenException;
import tickr.server.exceptions.UnauthorizedException;
import tickr.util.FileHelper;

import java.util.Map;

public class TestEditProfile {
    private DataModel model;
    private TickrController controller;

    private ModelSession session;
    private String authToken;
    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();

        session = model.makeSession();
        authToken = controller.userRegister(session, new UserRegisterRequest("TestUsername", "Test", "User", "test@example.com",
                "Password123!", "2010-10-07")).authToken;
        session = TestHelper.commitMakeSession(model, session);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
    }

    @AfterAll
    public static void clearStaticFiles () {
        TestHelper.clearStaticFiles();
    }

    @Test
    public void testBadRequest () {
        assertThrows(UnauthorizedException.class, () -> controller.userEditProfile(session,
                new EditProfileRequest(null, null, null, null, null, null, null)));

        assertThrows(UnauthorizedException.class, () -> controller.userEditProfile(session,
                new EditProfileRequest(TestHelper.makeFakeJWT(), null, null, null, null, "test@", null)));

        assertThrows(ForbiddenException.class, () -> controller.userEditProfile(session,
                new EditProfileRequest(authToken, null, null, null, null, "test@", null)));
        assertThrows(ForbiddenException.class, () -> controller.userEditProfile(session,
                new EditProfileRequest(authToken, null, null, null, null, "@example.com", null)));
        assertThrows(ForbiddenException.class, () -> controller.userEditProfile(session,
                new EditProfileRequest(authToken, null, null, null, null, "test@example", null)));

        assertThrows(ForbiddenException.class, () -> controller.userEditProfile(session,
                new EditProfileRequest(authToken, null, null, null, "test bad url", null, null)));
        assertThrows(ForbiddenException.class, () -> controller.userEditProfile(session,
                new EditProfileRequest(authToken, null, null, null, "data:aksdhasjd;base64,askdasdasdas=", null, null)));
        assertThrows(ForbiddenException.class, () -> controller.userEditProfile(session,
                new EditProfileRequest(authToken, null, null, null, "data:image/png,askdasdasdas=", null, null)));
        assertThrows(ForbiddenException.class, () -> controller.userEditProfile(session,
                new EditProfileRequest(authToken, null, null, null, "data:image/png;stuff,askdasdasdas=", null, null)));
        assertThrows(ForbiddenException.class, () -> controller.userEditProfile(session,
                new EditProfileRequest(authToken, null, null, null, "data:image/png;base64,askdasdasdas=", null, null)));
    }

    @Test
    public void testNoChanges () {
        assertDoesNotThrow(() -> controller.userEditProfile(session,
                new EditProfileRequest(authToken, null, null, null, null, null, null)));
        session = TestHelper.commitMakeSession(model, session);
        var response = controller.userGetProfile(session, Map.of("auth_token", authToken));

        assertEquals("TestUsername", response.userName);
        assertEquals("Test", response.firstName);
        assertEquals("User", response.lastName);
        assertEquals("test@example.com", response.email);
        assertEquals("", response.description);
        assertEquals("", response.profilePicture);
    }

    @Test
    public void testChangesNoPfp () {
        assertDoesNotThrow(() -> controller.userEditProfile(session,
                new EditProfileRequest(authToken, "TestNew", "John", "Doe", null, "test2@gmail.com", "testing123")));

        session = TestHelper.commitMakeSession(model, session);
        var response = controller.userGetProfile(session, Map.of("auth_token", authToken));

        assertEquals("TestNew", response.userName);
        assertEquals("John", response.firstName);
        assertEquals("Doe", response.lastName);
        assertEquals("test2@gmail.com", response.email);
        assertEquals("testing123", response.description);
        assertEquals("", response.profilePicture);
    }

    @Test
    public void testUploadPfp () {
        assertDoesNotThrow(() -> controller.userEditProfile(session,
                new EditProfileRequest(authToken, null, null, null, FileHelper.readToDataUrl("/test_images/smile.jpg"), null, null)));

        session = TestHelper.commitMakeSession(model, session);
        var response = controller.userGetProfile(session, Map.of("auth_token", authToken));

        assertNotEquals("", response.profilePicture);
        var newFilePath = FileHelper.getStaticPath() + "/" + response.profilePicture;

        assertTrue(TestHelper.fileDiff("/test_images/smile.jpg", newFilePath));

        assertDoesNotThrow(() -> controller.userEditProfile(session,
                new EditProfileRequest(authToken, null, null, null, FileHelper.readToDataUrl("/test_images/smile.png"), null, null)));

        session = TestHelper.commitMakeSession(model, session);
        response = controller.userGetProfile(session, Map.of("auth_token", authToken));

        newFilePath = FileHelper.getStaticPath() + "/" + response.profilePicture;

        assertTrue(TestHelper.fileDiff("/test_images/smile.png", newFilePath));
    }

}
