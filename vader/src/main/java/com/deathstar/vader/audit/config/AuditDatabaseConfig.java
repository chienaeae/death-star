package com.deathstar.vader.audit.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class AuditDatabaseConfig {

    // Primary database configuration removed and split into PrimaryDatabaseConfig.java

    @Bean(name = "auditDataSource")
    @ConfigurationProperties(prefix = "audit.clickhouse")
    public DataSource auditDataSource() {
        return org.springframework.boot.jdbc.DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "auditJdbcTemplate")
    public NamedParameterJdbcTemplate auditJdbcTemplate(
            @Qualifier("auditDataSource") DataSource auditDataSource) {
        return new NamedParameterJdbcTemplate(auditDataSource);
    }
}
