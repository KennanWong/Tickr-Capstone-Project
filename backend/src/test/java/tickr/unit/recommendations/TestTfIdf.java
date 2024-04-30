package tickr.unit.recommendations;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import tickr.CreateEventReqBuilder;
import tickr.TestHelper;
import tickr.application.TickrController;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.entities.DocumentTerm;
import tickr.application.entities.TfIdf;
import tickr.application.recommendations.RecommenderEngine;
import tickr.mock.MockLocationApi;
import tickr.persistence.DataModel;
import tickr.persistence.HibernateModel;
import tickr.persistence.ModelSession;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TestTfIdf {
    static Logger logger = LogManager.getLogger();
    private DataModel model;
    private TickrController controller;
    private ModelSession session;

    private String authToken;

    @BeforeEach
    public void setup () {
        model = new HibernateModel("hibernate-test.cfg.xml");
        controller = new TickrController();
        ApiLocator.addLocator(ILocationAPI.class, () -> new MockLocationApi(model));

        session = model.makeSession();
        authToken = controller.userRegister(session, TestHelper.makeRegisterRequest()).authToken;
        session = TestHelper.commitMakeSession(model, session);
    }

    @AfterEach
    public void cleanup () {
        model.cleanup();
        ApiLocator.clearLocator(ILocationAPI.class);
    }

    @Test
    public void testNoEvents () {
        assertEquals(0, session.getAll(DocumentTerm.class).size());
        assertEquals(0, session.getAll(TfIdf.class).size());
    }

    @Test
    public void testOneEvent () {
        var eventId = controller.createEvent(session, new CreateEventReqBuilder()
                .withEventName("Test event")
                .withDescription("apple apPLe cOOkIe bear")
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);
        //RecommenderEngine.forceRecalculate(session);
        //session = TestHelper.commitMakeSession(model, session);

        var terms = Set.of("test", "event", "apple", "cookie", "bear");
        var totalWords = 6;
        var termCounts = Map.of(
                "test", 1,
                "event", 1,
                "apple", 2,
                "cookie", 1,
                "bear", 1
        );

        var docTerms = session.getAll(DocumentTerm.class);
        assertEquals(terms.size(), docTerms.size());
        for (var i : docTerms) {
            assertTrue(terms.contains(i.getTerm()));
            assertEquals(1, i.getTermCount());
        }

        var tfIdfs = session.getAll(TfIdf.class);
        assertEquals(tfIdfs.size(), termCounts.size());

        for (var i : tfIdfs) {
            var term = i.getTerm().getTerm();
            assertTrue(termCounts.containsKey(term));

            assertEquals(calculateTfIdf(termCounts.get(term), 1, 1), i.getTfIdf(1));
            logger.info("1 doc tfidf of {}: {}", term, i.getTfIdf(1));
        }

    }

    @Test
    public void testTwoEvents () {
        logger.info("Start of twoEvents!");
        var eventId1 = controller.createEvent(session, new CreateEventReqBuilder()
                .withEventName("Test event")
                .withDescription("apple apple cookie cookie bear")
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);

        var eventId2 = controller.createEvent(session, new CreateEventReqBuilder()
                .withEventName("Test,    two!!!!")
                .withDescription("apple bread")
                .build(authToken)).event_id;
        session = TestHelper.commitMakeSession(model, session);
        logger.info("After event creation!");
        //RecommenderEngine.forceRecalculate(session);
        //session = TestHelper.commitMakeSession(model, session);
        logger.info("After recalculation!");
        var totalWords1 = 6;
        var totalWords2 = 4;


        var documentFreqs = Map.of(
                "test", 2,
                "event", 1,
                "apple", 2,
                "cookie", 1,
                "bear", 1,
                "two", 1,
                "bread", 1
        );

        var termCounts1 = Map.of(
                "test", 1,
                "event", 1,
                "apple", 2,
                "cookie", 2,
                "bear", 1
        );

        var termCounts2 = Map.of(
                "test", 1,
                "two", 1,
                "apple", 1,
                "bread", 1
        );

        var terms = session.getAll(DocumentTerm.class);
        assertEquals(documentFreqs.size(), terms.size());
        for (var i : terms) {
            assertTrue(documentFreqs.containsKey(i.getTerm()));
            assertEquals(documentFreqs.get(i.getTerm()), i.getTermCount());
        }
        logger.info("After term assert!");

        var tfIdfs1 = session.getAllWith(TfIdf.class, "termId.event.id", UUID.fromString(eventId1));
        assertEquals(termCounts1.size(), tfIdfs1.size());
        for (var i : tfIdfs1) {
            var term = i.getTerm().getTerm();
            assertTrue(termCounts1.containsKey(term));

            assertEquals(calculateTfIdf(termCounts1.get(term), documentFreqs.get(term), 2), i.getTfIdf(2));
            logger.info("2 doc e1 tfidf of {}: {}", term, i.getTfIdf(2));
        }
        logger.info("After tfIdf1!");

        var tfIdfs2 = session.getAllWith(TfIdf.class, "termId.event.id", UUID.fromString(eventId2));
        assertEquals(termCounts2.size(), tfIdfs2.size());
        for (var i : tfIdfs2) {
            var term = i.getTerm().getTerm();
            assertTrue(termCounts2.containsKey(term));

            assertEquals(calculateTfIdf(termCounts2.get(term), documentFreqs.get(term), 2), i.getTfIdf(2));
            logger.info("2 doc e2 tfidf of {}: {}", term, i.getTfIdf(2));
        }
        session = TestHelper.commitMakeSession(model, session);
        logger.info("After tfidf2!");
    }

    private double calculateTfIdf (int eventTermCount, int documentCount, int numDocuments) {
        return (1 + Math.log10(eventTermCount)) * Math.log10((double)numDocuments  / (documentCount));
    }
}
