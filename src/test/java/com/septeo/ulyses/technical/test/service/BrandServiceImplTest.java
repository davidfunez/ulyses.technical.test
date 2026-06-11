package com.septeo.ulyses.technical.test.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.septeo.ulyses.technical.test.entity.Brand;
import com.septeo.ulyses.technical.test.repository.BrandRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandServiceImpl subject;

    @Test
    @DisplayName("getAllBrands delegates to the repository and returns its list")
    void test_getAllBrands_1() {
        // Given
        final List<Brand> expected = List.of(brand(1L), brand(2L));
        when(this.brandRepository.findAll()).thenReturn(expected);

        // When
        final List<Brand> result = this.subject.getAllBrands();

        // Then
        assertThat(result).isSameAs(expected);
        verify(this.brandRepository).findAll();
        verifyNoMoreInteractions(this.brandRepository);
    }

    @Test
    @DisplayName("getAllBrands returns an empty list when the repository has no brands")
    void test_getAllBrands_2() {
        // Given
        when(this.brandRepository.findAll()).thenReturn(List.of());

        // When
        final List<Brand> result = this.subject.getAllBrands();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getBrandById returns the brand when found")
    void test_getBrandById_1() {
        // Given
        final Brand brand = brand(7L);
        when(this.brandRepository.findById(7L)).thenReturn(Optional.of(brand));

        // When
        final Optional<Brand> result = this.subject.getBrandById(7L);

        // Then
        assertThat(result).contains(brand);
    }

    @Test
    @DisplayName("getBrandById returns empty when the brand does not exist")
    void test_getBrandById_2() {
        // Given
        when(this.brandRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        final Optional<Brand> result = this.subject.getBrandById(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("saveBrand delegates to the repository and returns the saved brand")
    void test_saveBrand_1() {
        // Given
        final Brand input = brand(null);
        final Brand saved = brand(42L);
        when(this.brandRepository.save(input)).thenReturn(saved);

        // When
        final Brand result = this.subject.saveBrand(input);

        // Then
        assertThat(result).isSameAs(saved);
        verify(this.brandRepository).save(input);
        verifyNoMoreInteractions(this.brandRepository);
    }

    @Test
    @DisplayName("deleteBrand delegates to the repository")
    void test_deleteBrand_1() {
        // Given
        final Long id = 5L;

        // When
        this.subject.deleteBrand(id);

        // Then
        verify(this.brandRepository).deleteById(id);
        verifyNoMoreInteractions(this.brandRepository);
    }

    private static Brand brand(final Long id) {
        final Brand brand = new Brand();
        brand.setId(id);
        brand.setName("name-" + id);
        return brand;
    }
}
