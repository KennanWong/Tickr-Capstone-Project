package tickr.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class HibernateModel implements DataModel {
    static final Logger logger = LogManager.getLogger();

    private final SessionFactory sessionFactory;
    private final StandardServiceRegistry registry;

    private boolean isClosed = false;


    private HibernateModel (StandardServiceRegistry registry) {
        this.registry = registry;

        try {
            sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
        } catch (Exception e) {
            logger.error("Failed to initialise Hibernate session factory!");
            registry.close();
            throw new RuntimeException("Failed to initialise Hibernate model", e);
        }
        logger.info("Successfully initialised Hibernate database!");
    }
    public HibernateModel () {
        this(new StandardServiceRegistryBuilder().configure().build());
    }

    public HibernateModel (String configFile) {
        this(new StandardServiceRegistryBuilder()
                .configure(configFile)
                .build());
    }

    @Override
    public synchronized ModelSession makeSession() {
        //logger.debug("Making a Hibernate model session!");
        if (isClosed) {
            throw new RuntimeException("Attempted to make session of already closed model!");
        }

        return new HibernateSession(sessionFactory.openSession());
    }

    @Override
    public synchronized boolean isClosed () {
        return isClosed;
    }

    @Override
    public synchronized void cleanup () {
        logger.info("Cleaning up Hibernate model!");
        sessionFactory.close();
        registry.close();
        isClosed = true;
    }
}
