package tickr.persistence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Interface representing a single session of a data source. The semantics of this interface
 * guarantee that there is no observable change to the database until commit() is called,
 * and all present changes may be rolled back using rollback().
 * Sessions are created once per request and are unsychronised, so they must only be used on
 * one thread.
 */
public interface ModelSession {
    /**
     * Get all entities of a given type
     * @param entityClass
     * @return
     * @param <T>
     */
    <T> List<T> getAll (Class<T> entityClass);
    <T> Stream<T> getAllStream (Class<T> entityClass);

    /**
     * Gets all entities with a column equal to a given object
     * @param entityClass
     * @param col the column to compare
     * @param data the object to compare to
     * @return
     * @param <T>
     * @param <I>
     */
    <T, I> List<T> getAllWith (Class<T> entityClass, String col, I data);
    <T, I> Stream<T> getAllWithStream (Class<T> entityClass, String col, I data);

    <T, I> Optional<T> getByUnique (Class<T> entityClass, String col, I data);

    /**
     * Gets a managed entity with a given id, if it exists
     * @param entityClass
     * @param id
     * @return
     * @param <T>
     * @param <I>
     */
    <T, I> Optional<T> getById (Class<T> entityClass, I id);

    /**
     * Saves an entity, adding a new row to the respective table
     * @param entity
     * @param <T>
     */
    <T> void save (T entity);

    /**
     * Removes an entity from a table
     * @param entity
     * @param <T>
     */
    <T> void remove (T entity);

    default <T> void clear (Class<T> tClass) {
        for (var i : getAll(tClass)) {
            remove(i);
        }
    }


    /**
     * Creates a new transaction with the same session
     */
    void newTransaction ();

    /**
     * Commits a session to the database, syncing any updates
     */
    void commit ();

    /**
     * Rolls back any changes made to the session
     */
    void rollback ();

    /**
     * Closes the session
     */
    void close ();
}
