package com.assistant.ai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Custom DataSource configuration that handles Render's PostgreSQL URL format.
 *
 * Render provides database URLs in the format: postgresql://user:pass@host/db
 * But JDBC requires:                            jdbc:postgresql://user:pass@host/db
 *
 * This class automatically converts either format to a valid JDBC URL.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    // Render injects DATABASE_URL automatically for linked databases
    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    // Our custom DB_URL (may also be postgresql:// format from Render)
    @Value("${DB_URL:}")
    private String dbUrl;

    // Individual connection parts (from render.yaml fromDatabase properties)
    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private String dbPort;

    @Value("${DB_NAME:ai_assistant}")
    private String dbName;

    @Value("${DB_USERNAME:postgres}")
    private String dbUsername;

    @Value("${DB_PASSWORD:muthuvel2004}")
    private String dbPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        String jdbcUrl = resolveJdbcUrl();
        log.info("Connecting to database: {}", jdbcUrl.replaceAll(":[^:@]+@", ":***@")); // mask password in logs

        config.setJdbcUrl(jdbcUrl);

        // If URL contains credentials, don't set them separately
        if (!jdbcUrl.contains("@")) {
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);
        } else {
            // Render's connectionString contains user:pass in the URL itself
            // Extract username and password from URL or leave for JDBC driver
        }

        config.setDriverClassName("org.postgresql.Driver");
        config.setConnectionTimeout(20000);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.addDataSourceProperty("sslmode", "require");

        return new HikariDataSource(config);
    }

    private String resolveJdbcUrl() {
        // Priority 1: DATABASE_URL (Render auto-injects this)
        if (!databaseUrl.isEmpty()) {
            log.info("Using DATABASE_URL environment variable");
            return toJdbcUrl(databaseUrl);
        }

        // Priority 2: DB_URL (our custom variable, might be postgresql:// from Render)
        if (!dbUrl.isEmpty()) {
            log.info("Using DB_URL environment variable");
            return toJdbcUrl(dbUrl);
        }

        // Priority 3: Individual DB_HOST / DB_PORT / DB_NAME components
        String constructed = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
        log.info("Constructing JDBC URL from DB_HOST/DB_PORT/DB_NAME");
        return constructed;
    }

    /**
     * Converts any PostgreSQL URL format to a valid JDBC URL.
     * Handles:
     *   postgresql://user:pass@host/db  -> jdbc:postgresql://user:pass@host/db
     *   postgres://user:pass@host/db    -> jdbc:postgresql://user:pass@host/db
     *   jdbc:postgresql://...           -> unchanged (already valid)
     */
    private String toJdbcUrl(String url) {
        if (url.startsWith("jdbc:")) {
            return url; // Already valid JDBC URL
        }
        if (url.startsWith("postgresql://")) {
            return "jdbc:" + url;
        }
        if (url.startsWith("postgres://")) {
            return "jdbc:postgresql://" + url.substring("postgres://".length());
        }
        // Unknown format — return as-is and let JDBC handle it
        return url;
    }
}
