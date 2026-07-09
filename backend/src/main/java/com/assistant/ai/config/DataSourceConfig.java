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
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Custom DataSource configuration that handles Render's PostgreSQL URL format.
 *
 * Render provides database URLs in the format: postgresql://user:pass@host/db
 * But JDBC requires:                            jdbc:postgresql://host:port/db
 * And username/password must be configured separately.
 *
 * This class automatically parses any database URL format and configures Hikari.
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
        config.setDriverClassName("org.postgresql.Driver");

        // Parse any provided database URL
        DbSettings settings = resolveDbSettings();

        log.info("Connecting to database host: {}:{} (DB: {})", settings.host, settings.port, settings.database);

        config.setJdbcUrl(settings.jdbcUrl);
        config.setUsername(settings.username);
        config.setPassword(settings.password);

        config.setConnectionTimeout(20000);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.addDataSourceProperty("sslmode", "require");

        return new HikariDataSource(config);
    }

    private DbSettings resolveDbSettings() {
        // Try DATABASE_URL (Render auto-injected)
        if (!databaseUrl.isEmpty()) {
            log.info("Using DATABASE_URL environment variable");
            DbSettings parsed = parseUrl(databaseUrl);
            if (parsed != null) return parsed;
        }

        // Try DB_URL
        if (!dbUrl.isEmpty()) {
            log.info("Using DB_URL environment variable");
            DbSettings parsed = parseUrl(dbUrl);
            if (parsed != null) return parsed;
        }

        // Fallback: Construct from individual parts
        log.info("Constructing database configuration from individual parts (DB_HOST, etc.)");
        DbSettings settings = new DbSettings();
        settings.host = dbHost;
        settings.port = dbPort;
        settings.database = dbName;
        settings.username = dbUsername;
        settings.password = dbPassword;
        settings.jdbcUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
        return settings;
    }

    private DbSettings parseUrl(String rawUrl) {
        try {
            // Clean up scheme for java.net.URI parsing if needed
            String cleanUrl = rawUrl;
            if (cleanUrl.startsWith("jdbc:")) {
                // If it's already jdbc:postgresql://host/db, let's extract the part after jdbc:
                cleanUrl = cleanUrl.substring("jdbc:".length());
            }

            // Standardize scheme to postgresql (java.net.URI expects a scheme)
            if (cleanUrl.startsWith("postgres://")) {
                cleanUrl = "postgresql://" + cleanUrl.substring("postgres://".length());
            }

            URI uri = new URI(cleanUrl);
            DbSettings settings = new DbSettings();

            // Extract host, port, database
            settings.host = uri.getHost();
            settings.port = String.valueOf(uri.getPort() == -1 ? 5432 : uri.getPort());
            
            String path = uri.getPath();
            if (path != null && path.startsWith("/")) {
                settings.database = path.substring(1);
            } else {
                settings.database = path;
            }

            // Extract userInfo (user:password)
            String userInfo = uri.getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                settings.username = parts[0];
                settings.password = parts[1];
            } else {
                settings.username = dbUsername;
                settings.password = dbPassword;
            }

            // Construct clean JDBC URL WITHOUT credentials embedded (which PostgreSQL JDBC driver rejects)
            settings.jdbcUrl = "jdbc:postgresql://" + settings.host + ":" + settings.port + "/" + settings.database;
            return settings;
        } catch (URISyntaxException e) {
            log.error("Failed to parse database URL: {}. Falling back to default properties.", rawUrl, e);
            return null;
        }
    }

    private static class DbSettings {
        String jdbcUrl;
        String username;
        String password;
        String host;
        String port;
        String database;
    }
}
