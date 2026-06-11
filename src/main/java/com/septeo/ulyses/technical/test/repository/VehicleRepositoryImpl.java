package com.septeo.ulyses.technical.test.repository;

import com.septeo.ulyses.technical.test.entity.Vehicle;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the VehicleRepository interface.
 * This class provides the implementation for all vehicle-related operations.
 */
@Repository
public class VehicleRepositoryImpl implements VehicleRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Vehicle> findAll() {
        String stringQuery = "SELECT v FROM Vehicle v";
        Query query = entityManager.createQuery(stringQuery);
        return query.getResultList();
    }

    @Override
    public Optional<Vehicle> findById(Long id) {
        String stringQuery = "SELECT v FROM Vehicle v WHERE v.id = :id";
        Query query = entityManager.createQuery(stringQuery);
        query.setParameter("id", id);

        try {
            return Optional.of((Vehicle) query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        if (vehicle.getId() == null) {
            entityManager.persist(vehicle);
            return vehicle;
        } else {
            return entityManager.merge(vehicle);
        }
    }

    @Override
    public void deleteById(Long id) {
        String stringQuery = "DELETE FROM Vehicle v WHERE v.id = :id";
        Query query = entityManager.createQuery(stringQuery);
        query.setParameter("id", id);
        query.executeUpdate();
    }

}
