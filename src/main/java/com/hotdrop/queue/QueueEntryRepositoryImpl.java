package com.hotdrop.queue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

@Repository
public class QueueEntryRepositoryImpl implements QueueEntryRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private Boolean isH2 = null;

    private synchronized boolean checkIsH2() {
        if (isH2 != null) {
            return isH2;
        }
        Session session = entityManager.unwrap(Session.class);
        isH2 = session.doReturningWork(connection -> {
            String dbProductName = connection.getMetaData().getDatabaseProductName();
            return dbProductName != null && dbProductName.equalsIgnoreCase("H2");
        });
        return isH2;
    }

    @Override
    public int randomizeWaitingQueue(Long eventId) {
        if (checkIsH2()) {
            return entityManager.createNativeQuery("""
                MERGE INTO queue_entries q
                USING (
                    SELECT id, ROW_NUMBER() OVER (ORDER BY RAND()) as pos
                    FROM queue_entries
                    WHERE event_id = :eventId AND status = 'WAITING'
                ) s
                ON (q.id = s.id)
                WHEN MATCHED THEN
                UPDATE SET queue_position = s.pos, status = 'QUEUED'
            """).setParameter("eventId", eventId).executeUpdate();
        }

        return entityManager.createNativeQuery("""
            WITH shuffled AS (
                SELECT id, ROW_NUMBER() OVER (ORDER BY random()) as pos
                FROM queue_entries
                WHERE event_id = :eventId AND status = 'WAITING'
            )
            UPDATE queue_entries q
            SET queue_position = s.pos, status = 'QUEUED'
            FROM shuffled s
            WHERE q.id = s.id
        """).setParameter("eventId", eventId).executeUpdate();
    }
}
