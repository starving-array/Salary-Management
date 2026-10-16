package com.salarymanagement.salarymanagement.seed;

import com.salarymanagement.salarymanagement.SalaryManagementApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Standalone, non-web application process for deterministic database seeding.
 * Architecturally separated from the backend web execution.
 */
public class DemoDataSeedApplication {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeedApplication.class);

    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        SeedResult result = run(args);
        if (!result.executed() && !result.skipped()) {
            System.exit(1);
        }
    }

    public static SeedResult run(String... args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        log.info("Starting standalone non-web DemoDataSeedApplication...");
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SalaryManagementApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.devtools.restart.enabled=false",
                        "seed.demo-data.enabled=false"
                )
                .run(args)) {
            DemoDataSeeder seeder = context.getBean(DemoDataSeeder.class);
            SeedResult result = seeder.seed();
            log.info("Demo data seed result: {}", result.message());
            return result;
        } catch (Throwable ex) {
            if ("org.springframework.boot.devtools.restart.SilentExitExceptionHandler$SilentExitException".equals(ex.getClass().getName())) {
                throw (RuntimeException) ex;
            }
            log.error("Demo data seeding failed: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}
