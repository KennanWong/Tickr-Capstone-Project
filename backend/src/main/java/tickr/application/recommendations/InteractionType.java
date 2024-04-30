package tickr.application.recommendations;

public enum InteractionType {
    VIEW(0.05), TICKET_PURCHASE(0.7), REVIEW(2), COMMENT(0.4), REACT(0.2);

    private final double weight;

    InteractionType (double weight) {
        this.weight = weight;
    }

    public double getWeight () {
        return weight;
    }
}
