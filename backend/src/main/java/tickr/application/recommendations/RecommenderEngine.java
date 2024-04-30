package tickr.application.recommendations;

import tickr.application.entities.*;
import tickr.persistence.ModelSession;
import tickr.util.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class RecommenderEngine {
    private static final double TERM_WEIGHT = 1;
    private static final double TAG_WEIGHT = 0.4;
    private static final double CATEGORY_WEIGHT = 0.6;
    private static final double HOST_WEIGHT = 0.7;
    private static final double DISTANCE_WEIGHT = 1.5;

    private static final Vector WEIGHT_VECTOR = new Vector(List.of(TERM_WEIGHT, TAG_WEIGHT, CATEGORY_WEIGHT, HOST_WEIGHT, DISTANCE_WEIGHT))
            .normalised();

    public static void forceRecalculate (ModelSession session) {
        session.clear(TfIdf.class);
        session.clear(DocumentTerm.class);

        calculateTfIdfs(session);
    }

    private static void calculateTfIdfs (ModelSession session) {
        // Calculate tf idfs for all events
        var docTerms = new HashMap<String, DocumentTerm>();
        for (var i : session.getAll(Event.class)) {
            // Get word counts of event
            var wordCounts = i .getWordCounts();

            var tfidfs = new ArrayList<TfIdf>();
            for (var j : wordCounts.entrySet()) {
                // Get or create document term associated with word
                DocumentTerm term;
                if (docTerms.containsKey(j.getKey())) {
                    term = docTerms.get(j.getKey());
                } else {
                    term = new DocumentTerm(j.getKey(), 0);
                    session.save(term);
                    docTerms.put(j.getKey(), term);
                }

                // Increment term count
                term.incrementCount();

                // Create tf idf instance for this event
                var tfIdf = new TfIdf(term, i, j.getValue().intValue());
                session.save(tfIdf);
                tfidfs.add(tfIdf);
            }

            // Set tf idfs for this event
            i.setTfIdfs(tfidfs);
        }
    }

    public static double calculateSimilarity (ModelSession session, Event e1, Event e2) {
        if (e1.equals(e2)) {
            return 1;
        }
        var similarityVector = buildSimilarityVector(session, e1, e2);

        return similarityVector.dotProduct(WEIGHT_VECTOR);
    }

    public static double calculateUserScore (ModelSession session, Event event, EventVector userProfile) {
        return calculateUserScoreVector(session, event, userProfile).dotProduct(WEIGHT_VECTOR);
    }

    public static double calculateUserEventScore (ModelSession session, Event testEvent, Event currEvent, EventVector userProfile) {
        var eventEventVec = buildSimilarityVector(session, testEvent, currEvent);
        var userEventVec = calculateUserScoreVector(session, testEvent, userProfile);

        return eventEventVec.add(userEventVec).multiply(0.5).dotProduct(WEIGHT_VECTOR);
    }

    private static Vector calculateUserScoreVector (ModelSession session, Event event, EventVector userProfile) {
        int numEvents = session.getAll(Event.class).size();
        var tagIdf = getTagIdf(session, numEvents);
        var categoryIdf = getCategoryIdf(session, numEvents);

        var userVector = userProfile.applyIdfs(tagIdf, categoryIdf);
        var eventVector = event.getEventVector(numEvents).applyIdfs(tagIdf, categoryIdf);

        // Combine (dot components) user with event vector to produce final score vector
        // Distance is taken to be 0 to ignore it
        return userVector.combine(eventVector, 0.0);
    }


    private static Vector buildSimilarityVector (ModelSession session, Event e1, Event e2) {
        int numEvents = session.getAll(Event.class).size();
        var e1Vec = e1.getEventVector(numEvents);
        var e2Vec = e2.getEventVector(numEvents);

        var tagIdf = getTagIdf(session, numEvents);
        var categoryIdf = getCategoryIdf(session, numEvents);

        // Apply idfs to vectors
        e1Vec = e1Vec.applyIdfs(tagIdf, categoryIdf);
        e2Vec = e2Vec.applyIdfs(tagIdf, categoryIdf);

        // Inverse distance adds 1 to ensure that invDistance <= 1
        var invDistance = 1.0 / (e1.getDistance(e2) + 1);

        // Combine the two event vectors with the distance component
        return e1Vec.combine(e2Vec, invDistance);
    }

    public static EventVector buildUserProfile (ModelSession session, User user) {
        int numEvents = session.getAll(Event.class).size();
        var profile = EventVector.identity();
        for (var i : user.getInteractions()) {
            // Add together vectors associated with each interaction
            profile = profile.add(i.getVector(numEvents));
        }

        // Normalise to get overall profile
        return profile.normalise();
    }

    public static void recordInteraction (ModelSession session, User user, Event event, InteractionType type) {
        // Cannot record review this way
        assert type != InteractionType.REVIEW;
        var interaction = new UserInteraction(user, event, type, null);
        session.save(interaction);
    }

    public static void recordRating (ModelSession session, User user, Event event, double rating) {
        var interaction = new UserInteraction(user, event, InteractionType.REVIEW, rating);
        session.save(interaction);
    }

    private static SparseVector<String> getTagIdf (ModelSession session, int numEvents) {
        // Get all tags and convert to type, num map
        var tagMap = session.getAllStream(Tag.class)
                .map(Tag::getTags)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        var entries = new ArrayList<>(tagMap.entrySet());

        // Convert type, num map to idfs
        return new SparseVector<>(entries.stream().map(Map.Entry::getKey).collect(Collectors.toList()),
                entries.stream()
                        .map(Map.Entry::getValue)
                        .map(x -> Utils.getIdf(x.intValue(), numEvents))
                        .collect(Collectors.toList()))
                .normalised();
    }

    private static SparseVector<String> getCategoryIdf (ModelSession session, int numEvents) {
        // Get all categories and convert to type, num map
        var categoryMap = session.getAllStream(Category.class)
                .map(Category::getCategory)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        var entries = new ArrayList<>(categoryMap.entrySet());

        // Convert type, num map to idfs
        return new SparseVector<>(entries.stream().map(Map.Entry::getKey).collect(Collectors.toList()),
                entries.stream()
                        .map(Map.Entry::getValue)
                        .map(x -> Utils.getIdf(x.intValue(), numEvents))
                        .collect(Collectors.toList()))
                .normalised();
    }
}
