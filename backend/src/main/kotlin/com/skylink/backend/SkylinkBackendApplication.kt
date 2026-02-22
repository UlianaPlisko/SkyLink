package com.skylink.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration

@SpringBootApplication(
    exclude = [
        DataSourceAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class
    ]
)
class SkylinkBackendApplication

fun main(args: Array<String>) {
    runApplication<SkylinkBackendApplication>(*args)
}
