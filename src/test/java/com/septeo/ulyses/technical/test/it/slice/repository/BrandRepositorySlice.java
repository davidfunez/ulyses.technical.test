package com.septeo.ulyses.technical.test.it.slice.repository;

import com.septeo.ulyses.technical.test.repository.BrandRepositoryImpl;

import org.springframework.context.annotation.Import;

@Import(BrandRepositoryImpl.class)
public class BrandRepositorySlice {
}
