package com.septeo.ulyses.technical.test.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Logs every HTTP request and its response to a dedicated access log file.
 *
 * <p>The fully qualified class name is referenced in {@code logback-spring.xml}
 * to route this logger's output to its own file appender with
 * {@code additivity=false}, so access lines never leak into the console/root log.
 *
 * <p>The filter is auto-registered by Spring Boot as a {@code @Component} and pinned
 * to {@link Ordered#HIGHEST_PRECEDENCE} so it wraps every other servlet filter,
 * including the Spring Security filter chain. This guarantees that:
 * <ul>
 *   <li>Requests rejected by Spring Security (e.g. {@code 401}/{@code 403}) are still logged.</li>
 *   <li>The status read from the response in the {@code finally} block is the final
 *       status returned to the client.</li>
 *   <li>The measured elapsed time covers the full request lifecycle.</li>
 * </ul>
 * No {@code FilterRegistrationBean} is needed because we do not require custom URL
 * patterns, dispatcher types or init params: the {@link Order} annotation is enough
 * to control the position of the filter in the chain.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger(AccessLogFilter.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            ACCESS_LOG.info("{} | {} | {} | {} | {}ms",
                    OffsetDateTime.now(ZoneOffset.UTC).format(TIMESTAMP_FORMAT),
                    request.getMethod(),
                    buildFullUrl(request),
                    response.getStatus(),
                    elapsedMillis(startNanos));
        }
    }

    private static String buildFullUrl(HttpServletRequest request) {
        final String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
