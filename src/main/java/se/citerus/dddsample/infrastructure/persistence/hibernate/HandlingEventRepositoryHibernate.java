package se.citerus.dddsample.infrastructure.persistence.hibernate;

import org.springframework.stereotype.Repository;
import se.citerus.dddsample.domain.model.cargo.TrackingId;
import se.citerus.dddsample.domain.model.handling.HandlingEvent;
import se.citerus.dddsample.domain.model.handling.HandlingEventRepository;
import se.citerus.dddsample.domain.model.handling.HandlingHistory;

/**
 * Hibernate implementation of HandlingEventRepository.
 *
 */

//这里是sql语句
@Repository
public class HandlingEventRepositoryHibernate extends HibernateRepository implements HandlingEventRepository {

  @Override
  public void store(final HandlingEvent event) {
    getSession().save(event);
  }
  //Hibernate save

  @Override
  public HandlingHistory lookupHandlingHistoryOfCargo(final TrackingId trackingId) {
    return new HandlingHistory(getSession().createQuery(
            "from HandlingEvent where cargo.trackingId = :tid").
            setParameter("tid", trackingId).
            list()
    );
  }

}
