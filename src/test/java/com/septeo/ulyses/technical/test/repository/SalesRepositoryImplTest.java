package com.septeo.ulyses.technical.test.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import com.septeo.ulyses.technical.test.entity.Sales;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalesRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Sales> query;

    private SalesRepositoryImpl subject;

    @BeforeEach
    void setUp() throws Exception {
        this.subject = new SalesRepositoryImpl();
        // EntityManager is @PersistenceContext-injected; set it via reflection for unit tests.
        final Field field = SalesRepositoryImpl.class.getDeclaredField("entityManager");
        field.setAccessible(true);
        field.set(this.subject, this.entityManager);
    }

    @Test
    @DisplayName("findAll runs the SELECT-all query and returns its result")
    void test_findAll_1() {
        // Given
        final List<Sales> expected = List.of(new Sales(), new Sales());
        when(this.entityManager.createQuery(
                "SELECT s FROM Sales s "
                        + "JOIN FETCH s.brand "
                        + "JOIN FETCH s.vehicle",
                Sales.class)).thenReturn(this.query);
        when(this.query.getResultList()).thenReturn(expected);

        // When
        final List<Sales> result = this.subject.findAll();

        // Then
        assertThat(result).isSameAs(expected);
        verify(this.query).getResultList();
        verifyNoMoreInteractions(this.query);
    }

    @Test
    @DisplayName("findById returns the entity wrapped in an Optional when found")
    void test_findById_1() {
        // Given
        final Sales sale = new Sales();
        when(this.entityManager.createQuery(
                "SELECT s FROM Sales s "
                        + "JOIN FETCH s.brand "
                        + "JOIN FETCH s.vehicle "
                        + "WHERE s.id = :id",
                Sales.class)).thenReturn(this.query);
        when(this.query.setParameter("id", 1L)).thenReturn(this.query);
        when(this.query.getSingleResult()).thenReturn(sale);

        // When
        final Optional<Sales> result = this.subject.findById(1L);

        // Then
        assertThat(result).contains(sale);
    }

    @Test
    @DisplayName("findById returns empty when the JPA query throws NoResultException")
    void test_findById_2() {
        // Given
        when(this.entityManager.createQuery(
                "SELECT s FROM Sales s "
                        + "JOIN FETCH s.brand "
                        + "JOIN FETCH s.vehicle "
                        + "WHERE s.id = :id",
                Sales.class)).thenReturn(this.query);
        when(this.query.setParameter("id", 99L)).thenReturn(this.query);
        when(this.query.getSingleResult()).thenThrow(new NoResultException());

        // When
        final Optional<Sales> result = this.subject.findById(99L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findPage applies pagination using firstResult and maxResults=pageSize+1")
    void test_findPage_1() {
        // Given
        final List<Sales> rows = List.of(new Sales(), new Sales(), new Sales());
        when(this.entityManager.createQuery(
                "SELECT s FROM Sales s "
                        + "JOIN FETCH s.brand "
                        + "JOIN FETCH s.vehicle "
                        + "ORDER BY s.id ASC",
                Sales.class)).thenReturn(this.query);
        when(this.query.setFirstResult(20)).thenReturn(this.query);
        when(this.query.setMaxResults(11)).thenReturn(this.query);
        when(this.query.getResultList()).thenReturn(rows);

        // When
        final List<Sales> result = this.subject.findPage(3, 10);

        // Then
        assertThat(result).isSameAs(rows);
        verify(this.query).setFirstResult(20);
        verify(this.query).setMaxResults(11);
    }

    @Test
    @DisplayName("findPage uses offset 0 for the first page")
    void test_findPage_2() {
        // Given
        when(this.entityManager.createQuery(
                "SELECT s FROM Sales s "
                        + "JOIN FETCH s.brand "
                        + "JOIN FETCH s.vehicle "
                        + "ORDER BY s.id ASC",
                Sales.class)).thenReturn(this.query);
        when(this.query.setFirstResult(0)).thenReturn(this.query);
        when(this.query.setMaxResults(11)).thenReturn(this.query);
        when(this.query.getResultList()).thenReturn(List.of());

        // When
        this.subject.findPage(1, 10);

        // Then
        verify(this.query).setFirstResult(0);
        verify(this.query).setMaxResults(11);
    }

    @Test
    @DisplayName("findByBrandId binds the brandId parameter and returns the query result")
    void test_findByBrandId_1() {
        // Given
        final List<Sales> expected = List.of(new Sales());
        when(this.entityManager.createQuery(
                "SELECT s FROM Sales s "
                        + "JOIN FETCH s.brand "
                        + "JOIN FETCH s.vehicle "
                        + "WHERE s.brand.id = :brandId",
                Sales.class)).thenReturn(this.query);
        when(this.query.setParameter("brandId", 7L)).thenReturn(this.query);
        when(this.query.getResultList()).thenReturn(expected);

        // When
        final List<Sales> result = this.subject.findByBrandId(7L);

        // Then
        assertThat(result).isSameAs(expected);
        verify(this.query).setParameter("brandId", 7L);
    }

    @Test
    @DisplayName("findByVehicleId binds the vehicleId parameter and returns the query result")
    void test_findByVehicleId_1() {
        // Given
        final List<Sales> expected = List.of(new Sales(), new Sales());
        when(this.entityManager.createQuery(
                "SELECT s FROM Sales s "
                        + "JOIN FETCH s.brand "
                        + "JOIN FETCH s.vehicle "
                        + "WHERE s.vehicle.id = :vehicleId",
                Sales.class)).thenReturn(this.query);
        when(this.query.setParameter("vehicleId", 5L)).thenReturn(this.query);
        when(this.query.getResultList()).thenReturn(expected);

        // When
        final List<Sales> result = this.subject.findByVehicleId(5L);

        // Then
        assertThat(result).isSameAs(expected);
        verify(this.query).setParameter("vehicleId", 5L);
    }
}
