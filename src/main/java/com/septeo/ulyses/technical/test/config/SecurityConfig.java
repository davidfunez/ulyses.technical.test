package com.septeo.ulyses.technical.test.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * HTTP security:
 * <ul>
 *   <li>GET endpoints are public.</li>
 *   <li>POST/PUT/DELETE require role {@code ADMIN}.</li>
 *   <li>H2 console is left open (development convenience).</li>
 * </ul>
 * Stateless API: CSRF disabled and no HTTP session.
 */
@Configuration
public class SecurityConfig {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_ROLE = "USER";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/**").hasRole(ADMIN_ROLE)
                        .requestMatchers(HttpMethod.PUT, "/api/**").hasRole(ADMIN_ROLE)
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole(ADMIN_ROLE)
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(
            PasswordEncoder encoder,
            @Value("${app.security.user.username}") String userName,
            @Value("${app.security.user.password}") String userPassword,
            @Value("${app.security.admin.username}") String adminName,
            @Value("${app.security.admin.password}") String adminPassword) {

        final UserDetails user = User.withUsername(userName)
                .password(encoder.encode(userPassword))
                .roles(USER_ROLE)
                .build();

        final UserDetails admin = User.withUsername(adminName)
                .password(encoder.encode(adminPassword))
                .roles(ADMIN_ROLE, USER_ROLE)
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }
}
