package com.septeo.ulyses.technical.test.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class AccessLogFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private final AccessLogFilter subject = new AccessLogFilter();

    private final ListAppender appender = new ListAppender();
    private Logger filterLogger;
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        this.filterLogger = (Logger) LoggerFactory.getLogger(AccessLogFilter.class);
        this.previousLevel = this.filterLogger.getLevel();
        this.filterLogger.setLevel(Level.INFO);
        this.filterLogger.addAppender(this.appender);
        this.appender.start();
    }

    @AfterEach
    void tearDown() {
        this.filterLogger.detachAppender(this.appender);
        this.filterLogger.setLevel(this.previousLevel);
        this.appender.stop();
    }

    @Test
    @DisplayName("doFilterInternal logs method, URL and final response status after the chain runs")
    void test_doFilterInternal_1() throws ServletException, IOException {
        // Given
        when(this.request.getMethod()).thenReturn("GET");
        when(this.request.getRequestURI()).thenReturn("/api/brands");
        when(this.request.getQueryString()).thenReturn(null);
        when(this.response.getStatus()).thenReturn(200);

        // When
        this.subject.doFilter(this.request, this.response, this.chain);

        // Then
        verify(this.chain).doFilter(this.request, this.response);
        assertThat(this.appender.events).hasSize(1);
        final String message = this.appender.events.get(0).getFormattedMessage();
        assertThat(message).contains("GET")
                .contains("/api/brands")
                .contains("200")
                .contains("ms");
    }

    @Test
    @DisplayName("doFilterInternal appends the query string to the logged URL when present")
    void test_doFilterInternal_2() throws ServletException, IOException {
        // Given
        when(this.request.getMethod()).thenReturn("GET");
        when(this.request.getRequestURI()).thenReturn("/api/sales");
        when(this.request.getQueryString()).thenReturn("page=2");
        when(this.response.getStatus()).thenReturn(200);

        // When
        this.subject.doFilter(this.request, this.response, this.chain);

        // Then
        final String message = this.appender.events.get(0).getFormattedMessage();
        assertThat(message).contains("/api/sales?page=2");
    }

    @Test
    @DisplayName("doFilterInternal still logs when the filter chain throws (status captured in finally)")
    void test_doFilterInternal_3() throws ServletException, IOException {
        // Given
        when(this.request.getMethod()).thenReturn("POST");
        when(this.request.getRequestURI()).thenReturn("/api/brands");
        when(this.request.getQueryString()).thenReturn(null);
        when(this.response.getStatus()).thenReturn(500);
        final ServletException failure = new ServletException("boom");
        doThrow(failure).when(this.chain).doFilter(this.request, this.response);

        // When & Then
        assertThatThrownBy(() -> this.subject.doFilter(this.request, this.response, this.chain)).isSameAs(failure);
        assertThat(this.appender.events).hasSize(1);
        final String message = this.appender.events.get(0).getFormattedMessage();
        assertThat(message).contains("POST")
                .contains("/api/brands")
                .contains("500");
    }

    @Test
    @DisplayName("doFilterInternal reports a non-negative elapsed time")
    void test_doFilterInternal_4() throws ServletException, IOException {
        // Given
        when(this.request.getMethod()).thenReturn("GET");
        when(this.request.getRequestURI()).thenReturn("/api/vehicles");
        when(this.request.getQueryString()).thenReturn(null);
        when(this.response.getStatus()).thenReturn(404);
        doAnswer(invocation -> {
            Thread.sleep(2);
            return null;
        }).when(this.chain).doFilter(this.request, this.response);

        // When
        this.subject.doFilter(this.request, this.response, this.chain);

        // Then
        final String message = this.appender.events.get(0).getFormattedMessage();
        // pattern ends with "<n>ms" — assert it contains a digit followed by "ms"
        assertThat(message).matches(".*\\| \\d+ms$");
    }

    private static final class ListAppender extends AppenderBase<ILoggingEvent> {
        private final List<ILoggingEvent> events = new ArrayList<>();

        @Override
        protected void append(final ILoggingEvent event) {
            this.events.add(event);
        }
    }
}
