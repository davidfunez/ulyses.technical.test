package com.septeo.ulyses.technical.test.it.slice.repository;

import com.septeo.ulyses.technical.test.repository.SalesRepositoryImpl;

import org.springframework.context.annotation.Import;

@Import(SalesRepositoryImpl.class)
public class SalesRepositorySlice {
}
