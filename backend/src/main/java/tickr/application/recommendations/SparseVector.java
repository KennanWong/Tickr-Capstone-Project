package tickr.application.recommendations;

import com.google.common.collect.Streams;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SparseVector<K extends Comparable<K>> {
    private List<VectorElement<K>> elements;

    @SuppressWarnings("UnstableApiUsage")
    public SparseVector (List<K> keys, List<Double> values) {
        if (keys.size() != values.size()) {
            throw new RuntimeException("Key and value lengths must be equal!");
        }
        // Combine keys and values into vector elements
        elements = Streams.zip(keys.stream(), values.stream(), VectorElement::new)
                .sorted(Comparator.comparing(VectorElement::getKey)) // Sort according to key
                .collect(Collectors.toList());
    }

    private SparseVector (List<VectorElement<K>> elements) {
        this.elements = elements;
    }

    public double length () {
        return Math.sqrt(sqLength());
    }

    public double sqLength () {
        // Find squared length of vector
        return elements.stream()
                .map(VectorElement::getVal)
                .map(t -> t * t)
                .mapToDouble(x -> x)
                .sum();
    }

    public SparseVector<K> normalised () {
        var len = length();

        if (len == 0) {
            return new SparseVector<>(new ArrayList<>(elements));
        }

        // Scale by inverse length to normalise vector
        return new SparseVector<>(elements.stream()
                .map(v -> v.multiply(1 / len))
                .collect(Collectors.toList()));
    }

    public SparseVector<K> cartesianProduct (SparseVector<K> other) {
        // Make the cartesian (elementwise product)
        // Since the elements lists are ordered we can iterate through both
        // lists simultaneously to construct the new element list
        int it1 = 0;
        int it2 = 0;

        var newElements = new ArrayList<VectorElement<K>>();

        while (it1 < elements.size() && it2 < other.elements.size()) {
            var val1 = elements.get(it1);
            var val2 = other.elements.get(it2);

            int cmp = val1.getKey().compareTo(val2.getKey());

            // As we iterate in ascending order, if an element is lower than the other then there
            // is guaranteed to be no match for the first element and hence the elementwise product
            // will be 0
            if (cmp < 0) {
                // Element does not match, so this column will be 0 and hence should be ignored
                it1++;
            } else if (cmp > 0) {
                // Element does not match, so this column will be 0 and hence should be ignored
                it2++;
            } else {
                // Elements match, so include this column
                newElements.add(val1.multiply(val2));
                it1++;
                it2++;
            }
        }

        return new SparseVector<>(newElements);
    }

    public double dot (SparseVector<K> other) {
        // Construct the dot product
        int it1 = 0;
        int it2 = 0;

        double result = 0;

        while (it1 < elements.size() && it2 < other.elements.size()) {
            var val1 = elements.get(it1);
            var val2 = other.elements.get(it2);

            int cmp = val1.getKey().compareTo(val2.getKey());

            if (cmp < 0) {
                // Element does not match, skip
                it1++;
            } else if (cmp > 0) {
                // Element does not match, skip
                it2++;
            } else {
                // Element matches so include product in the final result
                result += val1.getVal() * val2.getVal();
                it1++;
                it2++;
            }
        }

        return result;
    }

    public SparseVector<K> multiply (double val) {
        return new SparseVector<>(elements.stream().map(v -> v.multiply(val)).collect(Collectors.toList()));
    }

    public SparseVector<K> add (SparseVector<K> other) {
        // Add two vectors, ensuring that the resulting element list is still ordered
        int it1 = 0;
        int it2 = 0;

        List<VectorElement<K>> newElements = new ArrayList<>();
        while (it1 < elements.size() && it2 < other.elements.size()) {
            var val1 = elements.get(it1);
            var val2 = other.elements.get(it2);

            int cmp = val1.getKey().compareTo(val2.getKey());
            if (cmp < 0) {
                // First key is smaller, so add first
                newElements.add(val1);
                it1++;
            } else if (cmp > 0) {
                // Second key is smaller, so add first
                newElements.add(val2);
                it2++;
            } else {
                // Keys are the same, so add both columns and add as one
                newElements.add(val1.add(val2));
                it1++;
                it2++;
            }
        }

        // Add remaining elements
        while (it1 < elements.size()) {
            newElements.add(elements.get(it1++));
        }

        // Add remaining elements
        while (it2 < other.elements.size()) {
            newElements.add(other.elements.get(it2++));
        }

        return new SparseVector<>(newElements);
    }

    private static class VectorElement<K> {
        private K key;
        private double val;

        public VectorElement (K k, double t) {
            this.key = k;
            this.val = t;
        }

        public K getKey () {
            return key;
        }

        public double getVal () {
            return val;
        }

        public VectorElement<K> multiply (double d) {
            return new VectorElement<>(key, val * d);
        }

        public VectorElement<K> multiply (VectorElement<K> other) {
            assert this.key.equals(other.key); // Sanity check

            return new VectorElement<>(key, val * other.val);
        }

        public VectorElement<K> add (VectorElement<K> other) {
            assert this.key.equals(other.key); // Sanity check

            return new VectorElement<>(key, val + other.val);
        }
    }
}
