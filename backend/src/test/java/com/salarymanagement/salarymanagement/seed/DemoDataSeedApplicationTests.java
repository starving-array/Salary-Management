package com.salarymanagement.salarymanagement.seed;

import com.salarymanagement.salarymanagement.SalaryManagementApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class DemoDataSeedApplicationTests {

    @Test
    @DisplayName("Seeding application context must not start an embedded web server")
    void shouldNotStartEmbeddedWebServer() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SalaryManagementApplication.class)
                .web(WebApplicationType.NONE)
                .properties("seed.demo-data.enabled=false")
                .run()) {

            // Must NOT be a web server application context
            assertFalse(context instanceof WebServerApplicationContext,
                    "Application context must not implement WebServerApplicationContext");

            // Must NOT contain web server beans
            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> context.getBean(WebServerApplicationContext.class),
                    "No WebServerApplicationContext bean should be present");

            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> context.getBean(ServletWebServerFactory.class),
                    "No ServletWebServerFactory bean should be present");

            // Must contain seeder beans
            assertNotNull(context.getBean(DemoDataSeeder.class),
                    "DemoDataSeeder bean must be available in context");
            assertNotNull(context.getBean(DemoDataGenerator.class),
                    "DemoDataGenerator bean must be available in context");
            assertNotNull(context.getBean(DemoDataProperties.class),
                    "DemoDataProperties bean must be available in context");
        }
    }

    @Test
    @DisplayName("DemoDataSeedApplication.run() should execute successfully without starting a web server")
    void shouldExecuteSeedSuccessfullyWithoutWebEnvironment() {
        SeedResult result = DemoDataSeedApplication.run();
        assertNotNull(result);
        assertTrue(result.executed() || result.skipped(),
                "Seeding must either execute successfully or skip idempotently");
    }
}
