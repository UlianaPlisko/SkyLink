package com.skylink.backend

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext

class SkylinkBackendApplicationTests {

    @Test
    fun `application class is available`() {
        assertNotNull(SkylinkBackendApplication::class)
        assertNotNull(SkylinkBackendApplication())
    }

    @Test
    fun `main delegates to spring application runner`() {
        val args = arrayOf("--spring.main.web-application-type=none")
        val context: ConfigurableApplicationContext = mock()

        Mockito.mockStatic(SpringApplication::class.java).use { springApplication ->
            springApplication.`when`<ConfigurableApplicationContext> {
                SpringApplication.run(SkylinkBackendApplication::class.java, *args)
            }.thenReturn(context)

            main(args)

            springApplication.verify {
                SpringApplication.run(SkylinkBackendApplication::class.java, *args)
            }
        }
    }
}
