package tickr.server;

public interface TriConsumer <T, U, V> {
    void consume (T t, U u, V v);
}
