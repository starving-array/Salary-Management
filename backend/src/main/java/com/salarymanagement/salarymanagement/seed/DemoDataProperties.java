package com.salarymanagement.salarymanagement.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "seed.demo-data")
public class DemoDataProperties {

    /**
     * Whether the demo data seeder is active. Disabled by default.
     */
    private boolean enabled = false;

    /**
     * Number of fictional employees to generate. Exactly 10,000 by default.
     */
    private int count = 10_000;

    /**
     * Fixed pseudo-random seed to guarantee 100% deterministic generation across runs.
     */
    private long randomSeed = 20261015L;

    /**
     * If true, resets previously seeded demo employees (matching 'EMP-%').
     * Requires {@code confirm=true} as a safety guard.
     */
    private boolean reset = false;

    /**
     * Explicit safety guard confirmation required when {@code reset=true}.
     */
    private boolean confirm = false;

    /**
     * Whether the application should exit immediately after completing seed execution.
     * Useful for standalone CLI invocation or Gradle tasks.
     */
    private boolean exitAfterSeed = false;

    /**
     * JDBC batch size for batched SQL inserts.
     */
    private int batchSize = 1_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public boolean isReset() {
        return reset;
    }

    public void setReset(boolean reset) {
        this.reset = reset;
    }

    public boolean isConfirm() {
        return confirm;
    }

    public void setConfirm(boolean confirm) {
        this.confirm = confirm;
    }

    public boolean isExitAfterSeed() {
        return exitAfterSeed;
    }

    public void setExitAfterSeed(boolean exitAfterSeed) {
        this.exitAfterSeed = exitAfterSeed;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
