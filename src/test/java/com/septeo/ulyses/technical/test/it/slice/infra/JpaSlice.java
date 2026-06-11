package com.septeo.ulyses.technical.test.it.slice.infra;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.h2.H2ConsoleAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;

/**
 * Brings up just the persistence layer (H2 + Hibernate + SQL init) required by
 * controller/service/repository slices that talk to the real database.
 *
 * <p>{@code @EntityScan} is needed because slice tests disable component scanning,
 * so JPA would otherwise not discover the entities.
 */
@EntityScan(basePackages = "com.septeo.ulyses.technical.test.entity")
@ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        SqlInitializationAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        H2ConsoleAutoConfiguration.class
})
public class JpaSlice {
}
