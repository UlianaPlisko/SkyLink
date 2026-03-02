package com.skylink.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties holder for the application data source.
 *
 * Spring automatically binds external configuration values to this class
 * using the prefix `app.datasource`. Values can come from:
 * - application.properties / application.yml
 * - environment variables
 * - external configuration files (via spring.config.location)
 *
 * Properties:
 * @property url JDBC URL of the database
 * @property username Database username
 * @property password Database password
 * @property maxPoolSize Maximum size of the Hikari connection pool (default: 10)
 * @property minIdle Minimum number of idle connections in the pool (default: 2)
 * @property connectionTimeout Maximum time in milliseconds to wait for a connection (default: 30000L)
 */
@ConfigurationProperties(prefix = "app.datasource")
data class AppDataSourceProperties(
    var url: String? = null,
    var username: String? = null,
    var password: String? = null,
    var maxPoolSize: Int = 10,
    var minIdle: Int = 2,
    var connectionTimeout: Long = 30000L
)