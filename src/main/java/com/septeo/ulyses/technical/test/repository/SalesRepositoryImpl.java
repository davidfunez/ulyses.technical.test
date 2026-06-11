package com.septeo.ulyses.technical.test.repository;

import com.septeo.ulyses.technical.test.entity.Sales;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the SalesRepository interface.
 * This class provides the implementation for all sales-related operations.
 */
@Repository
@Slf4j
public class SalesRepositoryImpl implements SalesRepository {

    private static final String LOG_PREFIX = "[SALES-REPO] ";

    // Single source of truth for the hydration contract: every query in this repository
    // returns Sales with brand and vehicle eagerly joined to avoid N+1 on the service layer.
    private static final String SELECT_WITH_GRAPH =
            "SELECT s FROM Sales s JOIN FETCH s.brand JOIN FETCH s.vehicle";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Sales> findAll() {
        log.debug("{}findAll", LOG_PREFIX);
        return entityManager
                .createQuery(SELECT_WITH_GRAPH, Sales.class)
                .getResultList();
    }

    @Override
    public Optional<Sales> findById(Long id) {
        log.debug("{}findById id={}", LOG_PREFIX, id);
        final TypedQuery<Sales> query = entityManager
                .createQuery(SELECT_WITH_GRAPH + " WHERE s.id = :id", Sales.class)
                .setParameter("id", id);

        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Sales> findPage(int page, int pageSize) {
        log.debug("{}findPage page={} pageSize={}", LOG_PREFIX, page, pageSize);
        return entityManager
                .createQuery(SELECT_WITH_GRAPH + " ORDER BY s.id ASC", Sales.class)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize + 1)
                .getResultList();
    }

    @Override
    public List<Sales> findByBrandId(Long brandId) {
        log.debug("{}findByBrandId brandId={}", LOG_PREFIX, brandId);
        return entityManager
                .createQuery(SELECT_WITH_GRAPH + " WHERE s.brand.id = :brandId", Sales.class)
                .setParameter("brandId", brandId)
                .getResultList();
    }

    @Override
    public List<Sales> findByVehicleId(Long vehicleId) {
        log.debug("{}findByVehicleId vehicleId={}", LOG_PREFIX, vehicleId);
        return entityManager
                .createQuery(SELECT_WITH_GRAPH + " WHERE s.vehicle.id = :vehicleId", Sales.class)
                .setParameter("vehicleId", vehicleId)
                .getResultList();
    }
}
