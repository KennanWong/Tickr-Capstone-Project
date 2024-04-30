package tickr.application.recommendations;

import com.google.common.collect.Streams;

import java.util.List;
import java.util.stream.Collectors;

public class Vector {
    private List<Double> elements;

    public Vector (List<Double> elements) {
        this.elements = elements;
    }

    public int getDimensions () {
        return elements.size();
    }

    @SuppressWarnings("UnstableApiUsage")
    public double dotProduct (Vector other) {
        if (getDimensions() != other.getDimensions()) {
            throw new RuntimeException("Vector lengths do not match!");
        }

        // Pair up columns and then multiply and add them together
        return Streams.zip(elements.stream(), other.elements.stream(), (x, y) -> x * y)
                .mapToDouble(x -> x)
                .sum();
    }

    public double length () {
        return Math.sqrt(elements.stream().mapToDouble(x -> x * x).sum());
    }

    public Vector normalised () {
        var normFactor = 1 / length();
        return multiply(normFactor);
    }

    @SuppressWarnings("UnstableApiUsage")
    public Vector add (Vector other) {
        if (getDimensions() != other.getDimensions()) {
            throw new RuntimeException("Vector lengths do not match!");
        }


        // Pair up columns and add them to each other
        return new Vector(Streams.zip(elements.stream(), other.elements.stream(), Double::sum).collect(Collectors.toList()));
    }

    public Vector multiply (double val) {
        return new Vector(elements.stream().map(d -> d * val).collect(Collectors.toList()));
    }
}
