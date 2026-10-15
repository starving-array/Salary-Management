package com.salarymanagement.salarymanagement.seed;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
public class DemoDataGenerator {

    public record JobTitleConfig(
            String department,
            String title,
            int minSalaryUSD,
            int maxSalaryUSD,
            int minSalaryGBP,
            int maxSalaryGBP
    ) {}

    public record GeneratedEmployeeBundle(
            EmployeeSeed employee,
            List<CompensationSeed> compensations
    ) {}

    private static final String[] FIRST_NAMES = {
            "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda", "William", "Elizabeth",
            "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica", "Thomas", "Sarah", "Charles", "Karen",
            "Christopher", "Nancy", "Daniel", "Lisa", "Matthew", "Betty", "Anthony", "Margaret", "Mark", "Sandra",
            "Donald", "Ashley", "Steven", "Kimberly", "Paul", "Emily", "Andrew", "Donna", "Joshua", "Michelle",
            "Kenneth", "Dorothy", "Kevin", "Carol", "Brian", "Amanda", "George", "Melissa", "Edward", "Deborah",
            "Ronald", "Stephanie", "Timothy", "Rebecca", "Jason", "Sharon", "Jeffrey", "Laura", "Ryan", "Cynthia"
    };

    private static final String[] LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
            "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin",
            "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson",
            "Walker", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
            "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell", "Carter", "Roberts",
            "Gomez", "Phillips", "Evans", "Turner", "Diaz", "Parker", "Cruz", "Edwards", "Collins", "Reyes"
    };

    private static final JobTitleConfig[] JOB_TITLES = {
            // Engineering
            new JobTitleConfig("Engineering", "Software Engineer", 80_000, 115_000, 50_000, 75_000),
            new JobTitleConfig("Engineering", "Senior Software Engineer", 125_000, 165_000, 80_000, 110_000),
            new JobTitleConfig("Engineering", "Staff Software Engineer", 170_000, 220_000, 115_000, 145_000),
            new JobTitleConfig("Engineering", "Engineering Manager", 160_000, 210_000, 105_000, 140_000),
            new JobTitleConfig("Engineering", "DevOps Engineer", 95_000, 135_000, 60_000, 90_000),
            new JobTitleConfig("Engineering", "QA Engineer", 75_000, 105_000, 45_000, 68_000),

            // Product & Design
            new JobTitleConfig("Product", "Product Manager", 95_000, 135_000, 62_000, 88_000),
            new JobTitleConfig("Product", "Senior Product Manager", 140_000, 185_000, 90_000, 120_000),
            new JobTitleConfig("Product", "Director of Product", 195_000, 260_000, 130_000, 175_000),
            new JobTitleConfig("Design", "Product Designer", 82_000, 118_000, 52_000, 76_000),
            new JobTitleConfig("Design", "Senior UI/UX Designer", 120_000, 160_000, 78_000, 105_000),

            // Data & Analytics
            new JobTitleConfig("Data & Analytics", "Data Analyst", 75_000, 105_000, 48_000, 70_000),
            new JobTitleConfig("Data & Analytics", "Data Engineer", 110_000, 155_000, 72_000, 102_000),
            new JobTitleConfig("Data & Analytics", "Data Scientist", 120_000, 165_000, 80_000, 110_000),

            // Human Resources
            new JobTitleConfig("Human Resources", "HR Generalist", 65_000, 90_000, 42_000, 60_000),
            new JobTitleConfig("Human Resources", "Talent Acquisition Specialist", 70_000, 100_000, 45_000, 65_000),
            new JobTitleConfig("Human Resources", "HR Director", 160_000, 220_000, 105_000, 145_000),

            // Finance
            new JobTitleConfig("Finance", "Financial Analyst", 75_000, 108_000, 48_000, 72_000),
            new JobTitleConfig("Finance", "Senior Accountant", 90_000, 130_000, 60_000, 85_000),
            new JobTitleConfig("Finance", "Finance Director", 175_000, 240_000, 115_000, 160_000),

            // Marketing & Sales
            new JobTitleConfig("Marketing", "Marketing Specialist", 68_000, 95_000, 44_000, 62_000),
            new JobTitleConfig("Marketing", "Content Strategist", 75_000, 105_000, 48_000, 70_000),
            new JobTitleConfig("Marketing", "Marketing Director", 165_000, 230_000, 110_000, 150_000),
            new JobTitleConfig("Sales", "Account Executive", 85_000, 130_000, 55_000, 85_000),
            new JobTitleConfig("Sales", "Sales Director", 180_000, 250_000, 120_000, 165_000),

            // Customer Support & Legal
            new JobTitleConfig("Customer Support", "Support Specialist", 55_000, 78_000, 36_000, 52_000),
            new JobTitleConfig("Customer Support", "Support Manager", 95_000, 130_000, 62_000, 85_000),
            new JobTitleConfig("Legal & Compliance", "Compliance Officer", 90_000, 130_000, 60_000, 85_000),
            new JobTitleConfig("Legal & Compliance", "Legal Counsel", 135_000, 185_000, 90_000, 125_000)
    };

    private static final LocalDate ANCHOR_DATE = LocalDate.of(2026, 10, 15);
    private static final LocalDate MIN_JOIN_DATE = LocalDate.of(2018, 1, 15);
    private static final int JOIN_WINDOW_DAYS = 3088; // 2018-01-15 to 2026-06-30

    public List<GeneratedEmployeeBundle> generate(int totalCount, long randomSeed) {
        Random random = new Random(randomSeed);
        List<GeneratedEmployeeBundle> results = new ArrayList<>(totalCount);

        for (int i = 1; i <= totalCount; i++) {
            results.add(generateSingle(i, random));
        }

        return results;
    }

    public GeneratedEmployeeBundle generateSingle(int index, Random random) {
        String empNum = String.format("EMP-%05d", index);
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String email = String.format("%s.%s.%05d@example.com", firstName.toLowerCase(), lastName.toLowerCase(), index);

        UUID empId = UUID.nameUUIDFromBytes(("salary-emp-" + index).getBytes(StandardCharsets.UTF_8));

        // Country: 60% US, 40% GB
        String countryCode = (random.nextInt(100) < 60) ? "US" : "GB";

        JobTitleConfig job = JOB_TITLES[random.nextInt(JOB_TITLES.length)];

        LocalDate joinedAt = MIN_JOIN_DATE.plusDays(random.nextInt(JOIN_WINDOW_DAYS));

        // Status: 90% Active, 8% Terminated, 2% Soft-deleted
        int statusRoll = random.nextInt(100);
        LocalDate terminatedAt = null;
        Instant deletedAt = null;

        if (statusRoll < 90) {
            // Active
            terminatedAt = null;
        } else if (statusRoll < 98) {
            // Terminated
            long maxService = ChronoUnit.DAYS.between(joinedAt, LocalDate.of(2026, 9, 30));
            if (maxService >= 60) {
                long serviceDays = 30 + random.nextInt((int) Math.min(maxService - 30, 1200));
                terminatedAt = joinedAt.plusDays(serviceDays);
            }
        } else {
            // Soft-deleted
            deletedAt = Instant.parse("2026-08-01T12:00:00Z").plusSeconds(random.nextInt(5_000_000));
        }

        EmployeeSeed employee = new EmployeeSeed(
                empId,
                empNum,
                firstName,
                lastName,
                email,
                countryCode,
                job.title(),
                job.department(),
                joinedAt,
                terminatedAt,
                deletedAt
        );

        // Compensation Records
        List<CompensationSeed> compRecords = generateCompensationRecords(index, empId, joinedAt, terminatedAt, countryCode, job, random);

        return new GeneratedEmployeeBundle(employee, compRecords);
    }

    private List<CompensationSeed> generateCompensationRecords(
            int empIndex,
            UUID empId,
            LocalDate joinedAt,
            LocalDate terminatedAt,
            String countryCode,
            JobTitleConfig job,
            Random random
    ) {
        List<CompensationSeed> records = new ArrayList<>(3);

        int minSal = countryCode.equals("US") ? job.minSalaryUSD() : job.minSalaryGBP();
        int maxSal = countryCode.equals("US") ? job.maxSalaryUSD() : job.maxSalaryGBP();
        int spread = maxSal - minSal;
        int step = Math.max(1, spread / 1000);
        BigDecimal initialSalary = BigDecimal.valueOf(minSal + (long) random.nextInt(step) * 1000L).setScale(2, RoundingMode.UNNECESSARY);

        LocalDate serviceEnd = (terminatedAt != null) ? terminatedAt : ANCHOR_DATE;
        long serviceDays = ChronoUnit.DAYS.between(joinedAt, serviceEnd);

        Instant creationTime = Instant.parse("2026-01-01T00:00:00Z");

        if (serviceDays < 450) {
            // 1 record
            records.add(new CompensationSeed(
                    UUID.nameUUIDFromBytes(("salary-comp-" + empIndex + "-1").getBytes(StandardCharsets.UTF_8)),
                    empId,
                    initialSalary,
                    joinedAt,
                    terminatedAt,
                    creationTime
            ));
        } else if (serviceDays < 900) {
            // 2 records (1 raise)
            LocalDate change1 = joinedAt.plusDays(365 + random.nextInt(60));
            if (change1.isBefore(serviceEnd.minusDays(30))) {
                BigDecimal raised = initialSalary.multiply(BigDecimal.valueOf(1.08)).setScale(2, RoundingMode.HALF_UP);
                records.add(new CompensationSeed(
                        UUID.nameUUIDFromBytes(("salary-comp-" + empIndex + "-1").getBytes(StandardCharsets.UTF_8)),
                        empId,
                        initialSalary,
                        joinedAt,
                        change1,
                        creationTime
                ));
                records.add(new CompensationSeed(
                        UUID.nameUUIDFromBytes(("salary-comp-" + empIndex + "-2").getBytes(StandardCharsets.UTF_8)),
                        empId,
                        raised,
                        change1,
                        terminatedAt,
                        creationTime.plus(365, ChronoUnit.DAYS)
                ));
            } else {
                records.add(new CompensationSeed(
                        UUID.nameUUIDFromBytes(("salary-comp-" + empIndex + "-1").getBytes(StandardCharsets.UTF_8)),
                        empId,
                        initialSalary,
                        joinedAt,
                        terminatedAt,
                        creationTime
                ));
            }
        } else {
            // 3 records (2 raises)
            LocalDate change1 = joinedAt.plusDays(365 + random.nextInt(50));
            LocalDate change2 = change1.plusDays(365 + random.nextInt(50));
            if (change2.isBefore(serviceEnd.minusDays(30))) {
                BigDecimal raised1 = initialSalary.multiply(BigDecimal.valueOf(1.08)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal raised2 = initialSalary.multiply(BigDecimal.valueOf(1.18)).setScale(2, RoundingMode.HALF_UP);

                records.add(new CompensationSeed(
                        UUID.nameUUIDFromBytes(("salary-comp-" + empIndex + "-1").getBytes(StandardCharsets.UTF_8)),
                        empId,
                        initialSalary,
                        joinedAt,
                        change1,
                        creationTime
                ));
                records.add(new CompensationSeed(
                        UUID.nameUUIDFromBytes(("salary-comp-" + empIndex + "-2").getBytes(StandardCharsets.UTF_8)),
                        empId,
                        raised1,
                        change1,
                        change2,
                        creationTime.plus(365, ChronoUnit.DAYS)
                ));
                records.add(new CompensationSeed(
                        UUID.nameUUIDFromBytes(("salary-comp-" + empIndex + "-3").getBytes(StandardCharsets.UTF_8)),
                        empId,
                        raised2,
                        change2,
                        terminatedAt,
                        creationTime.plus(730, ChronoUnit.DAYS)
                ));
            } else if (change1.isBefore(serviceEnd.minusDays(30))) {
                BigDecimal raised1 = initialSalary.multiply(BigDecimal.valueOf(1.08)).setScale(2, RoundingMode.HALF_UP);
                records.add(new CompensationSeed(
                        UUID.nameUUIDFromBytes(("salary-comp-" + empIndex + "-1").getBytes(StandardCharsets.UTF_8)),
                        empId,
                        initialSalary,
                        joinedAt,
                        change1,
                        creationTime
                ));
                records.add(new CompensationSeed(
                        UUID.nameUUIDFromBytes(("salary-comp-" + empIndex + "-2").getBytes(StandardCharsets.UTF_8)),
                        empId,
                        raised1,
                        change1,
                        terminatedAt,
                        creationTime.plus(365, ChronoUnit.DAYS)
                ));
            } else {
                records.add(new CompensationSeed(
                        UUID.nameUUIDFromBytes(("salary-comp-" + empIndex + "-1").getBytes(StandardCharsets.UTF_8)),
                        empId,
                        initialSalary,
                        joinedAt,
                        terminatedAt,
                        creationTime
                ));
            }
        }

        return records;
    }
}
