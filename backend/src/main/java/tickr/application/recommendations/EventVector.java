package tickr.application.recommendations;

import tickr.application.entities.Event;

import java.util.List;

public class EventVector {
    private SparseVector<String> termVector;
    private SparseVector<String> tagVector;
    private SparseVector<String> categoryVector;

    private SparseVector<String> hostVector;

    public EventVector (SparseVector<String> termVector, SparseVector<String> tagVector, SparseVector<String> categoryVector,
                        SparseVector<String> hostVector) {
        this.termVector = termVector;
        this.tagVector = tagVector;
        this.categoryVector = categoryVector;
        this.hostVector = hostVector;
    }

    public static EventVector identity () {
        // Get identity (0) event vector
        return new EventVector(new SparseVector<String>(List.of(), List.of()), new SparseVector<String>(List.of(), List.of()),
                new SparseVector<String>(List.of(), List.of()), new SparseVector<String>(List.of(), List.of()));
    }

    public EventVector add (EventVector other) {
        // Add together two event vectors
        return new EventVector(termVector.add(other.termVector), tagVector.add(other.tagVector), categoryVector.add(other.tagVector), hostVector.add(other.hostVector));
    }

    public EventVector multiply (double val) {
        // Multiply two event vectors by a scalar
        return new EventVector(termVector.multiply(val), tagVector.multiply(val), categoryVector.multiply(val), hostVector.multiply(val));
    }

    public Vector combine (EventVector other, double location) {
        // Combine two EventVectors with a dot product to make a Vector with a given location column
        return new Vector(List.of(termVector.dot(other.termVector), tagVector.dot(other.tagVector), categoryVector.dot(other.categoryVector),
                hostVector.dot(other.hostVector), location));
    }

    public EventVector applyIdfs (SparseVector<String> tagIdf, SparseVector<String> categoryIdf) {
        // Apply tag and category idfs to convert to Tf-Idfs
        return new EventVector(termVector, tagVector.cartesianProduct(tagIdf).normalised(), categoryVector.cartesianProduct(categoryIdf).normalised(), hostVector);
    }

    public EventVector normalise () {
        // Normalise the components, this intentionally does not normalise the entire vector so that combine can use normalised components
        return new EventVector(termVector.normalised(), tagVector.normalised(), categoryVector.normalised(), hostVector.normalised());
    }
}
