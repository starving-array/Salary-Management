package com.salarymanagement.salarymanagement.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "seed.demo-data.enabled", havingValue = "true")
public class DemoDataRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataRunner.class);

    private final DemoDataSeeder seeder;
    private final DemoDataProperties properties;

    public DemoDataRunner(DemoDataSeeder seeder, DemoDataProperties properties) {
        this.seeder = seeder;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        log.info("DemoDataRunner triggered: starting deterministic seeding...");
        try {
            SeedResult result = seeder.seed();
            log.info("Demo data seed result: {}", result.message());

            if (properties.isExitAfterSeed()) {
                log.info("Exit after seed requested (--seed.demo-data.exit-after-seed=true). Terminating process with code 0.");
                System.exit(0);
            }
        } catch (Exception ex) {
            log.error("Demo data seeding failed: {}", ex.getMessage(), ex);
            if (properties.isExitAfterSeed()) {
                System.exit(1);
            }
            throw ex;
        }
    }
}
