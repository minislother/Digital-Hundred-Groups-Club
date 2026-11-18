package com.chinahitech.shop.config;

import javax.sql.DataSource;
import java.sql.SQLException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 初始化持久化补偿表并启用定期重试；无 Kafka 环境也能恢复缓存一致性。 */
@Configuration
@EnableScheduling
public class CacheConsistencyConfig {
    @Bean
    @ConditionalOnProperty(name = "app.cache-retry.initialize-schema", havingValue = "true", matchIfMissing = true)
    public DataSourceInitializer cacheConsistencyInitializer(DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        var schema = new ResourceDatabasePopulator(new ClassPathResource("cache-consistency-schema.sql"));
        initializer.setDatabasePopulator(connection -> {
            schema.populate(connection);
            // CREATE TABLE IF NOT EXISTS does not upgrade databases already running without Flyway.
            try (var statement = connection.createStatement();
                 var columns = statement.executeQuery("SHOW COLUMNS FROM activity_admission_outbox LIKE 'last_error'")) {
                if (columns.next()) return;
            }
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE activity_admission_outbox ADD COLUMN last_error VARCHAR(32) NULL");
            } catch (SQLException failure) {
                // Another replica may have added the column after our check.
                if (failure.getErrorCode() != 1060) throw failure;
            }
        });
        return initializer;
    }
}
