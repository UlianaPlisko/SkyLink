package com.skylink.backend

import com.skylink.backend.config.AppDataSourceProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppDataSourceProperties::class)
class SkylinkBackendApplication

fun main(args: Array<String>) {
    runApplication<SkylinkBackendApplication>(*args)
}