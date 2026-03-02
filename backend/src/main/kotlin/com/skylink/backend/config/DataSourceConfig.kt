package com.skylink.backend.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Data source configuration class.
 *
 * This class retrieves database connection settings from [AppDataSourceProperties]
 * (enabled via @EnableConfigurationProperties) and creates a Spring-managed
 * bean of type [DataSource] using HikariCP.
 *
 * @constructor Injects an instance of [AppDataSourceProperties]
 * populated automatically by Spring.
 */
@Configuration
@EnableConfigurationProperties(AppDataSourceProperties::class)
class DataSourceConfig(private val dsProps: AppDataSourceProperties) {

    /**
     * Creates and registers a [DataSource] bean backed by HikariCP.
     *
     * @return a configured [DataSource] bean managed by the Spring container.
     * @throws IllegalStateException if `app.datasource.url` is not defined.
     */
    @Bean
    fun dataSource(): DataSource {
        val url = dsProps.url
            ?: throw IllegalStateException("Database URL is not set. Set APP_DATASOURCE_URL env var or external config.")
        val username = dsProps.username ?: ""
        val password = dsProps.password ?: ""

        val cfg = HikariConfig().apply {
            jdbcUrl = url
            this.username = username
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = dsProps.maxPoolSize
            minimumIdle = dsProps.minIdle
            connectionTimeout = dsProps.connectionTimeout
            // optional: add connection test query if needed
            // connectionTestQuery = "SELECT 1"
        }
        return HikariDataSource(cfg)
    }
}