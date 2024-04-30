package tickr.util;

public class Pair <T, U> {
    private T t;
    private U u;

    public Pair (T first, U second) {
        t = first;
        u = second;
    }

    public T getFirst () {
        return t;
    }

    public U getSecond () {
        return u;
    }
}
