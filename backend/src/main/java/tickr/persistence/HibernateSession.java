package tickr.persistence;

import jakarta.persistence.RollbackException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HibernateSession implements ModelSession {
    static final Logger logger = LogManager.getLogger();

    private Session session;
    boolean inTransaction;

    public HibernateSession (Session session) {
        this.session = session;
        session.beginTransaction();
        inTransaction = true;
    }

    @Override
    public <T> List<T> getAll (Class<T> entityClass) {
        return getAllStream(entityClass).collect(Collectors.toList());
    }

    @Override
    public <T> Stream<T> getAllStream (Class<T> entityClass) {
        // Create query for entity class
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);

        // Set table to be from the entity class' table
        Root<T> criteriaRoot = criteriaQuery.from(entityClass);
        criteriaQuery.select(criteriaRoot);

        // Make query
        Query<T> query = session.createQuery(criteriaQuery);
        return query.getResultStream();
    }

    @Override
    public <T, I> List<T> getAllWith (Class<T> entityClass, String col, I data) {
        return getAllWithStream(entityClass, col, data).collect(Collectors.toList());
    }

    @Override
    public <T, I> Stream<T> getAllWithStream (Class<T> entityClass, String col, I data) {
        // Create query for entity class
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
        Root<T> criteriaRoot = criteriaQuery.from(entityClass);

        var cols = col.split("\\.");
        var path = criteriaRoot.get(cols[0]);
        for (int i = 1; i < cols.length; i++) {
           path = path.get(cols[i]);
        }

        criteriaQuery.select(criteriaRoot) // Set root to be of the entity's table
                .where(criteriaBuilder.equal(path, data)); // Select only those which are equal to data

        // Build query
        Query<T> query = session.createQuery(criteriaQuery);
        return query.getResultStream();
    }

    @Override
    public <T, I> Optional<T> getByUnique (Class<T> entityClass, String col, I data) {
        var l = getAllWith(entityClass, col, data);
        if (l.size() == 0) {
            // No results
            return Optional.empty();
        } else if (l.size() == 1) {
            // One result
            return Optional.of(l.get(0));
        } else {
            // Multiple results, invalid id
            throw new RuntimeException(String.format("Column not unique: %s in class %s (%d results)!", data, entityClass.getName(), l.size()));
        }
    }

    @Override
    public <T, I> Optional<T> getById (Class<T> entityClass,  I id) {
        return getByUnique(entityClass, "id", id);
    }

    @Override
    public <T> void save (T entity) {
        session.persist(entity);
    }

    @Override
    public <T> void remove (T entity) {
        if (session.isReadOnly(entity)) {
            logger.error("Attempted to remove readonly entity: {}!", entity);
            return;
        }
        session.remove(entity);
    }

    @Override
    public void newTransaction () {
        if (inTransaction) {
            logger.error("Attempted to create a new transaction in a HibernateSession when already in a transaction!");
            return;
        }
        session.beginTransaction();
        inTransaction = true;
    }


    @Override
    public void commit () throws RollbackException {
        //logger.debug("Committing session!");
        session.flush();
        session.clear();
        session.getTransaction().commit();
        inTransaction = false;
    }

    @Override
    public void rollback () {
        session.getTransaction().rollback();
        inTransaction = false;
    }

    @Override
    public void close () {
        //logger.debug("Closing session!");
        session.close();
    }


}
