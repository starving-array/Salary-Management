package com.salarymanagement.salarymanagement.repository;

import com.salarymanagement.salarymanagement.entity.CompensationRecord;
import com.salarymanagement.salarymanagement.repository.projection.CountrySalarySummaryProjection;
import com.salarymanagement.salarymanagement.repository.projection.JobTitleSalarySummaryProjection;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompensationRecordRepository extends JpaRepository<CompensationRecord, UUID> {

    List<CompensationRecord> findByEmployee_IdOrderByEffectiveFromAscCreatedAtAsc(UUID employeeId);

    boolean existsByEmployee_Id(UUID employeeId);

    @Query("SELECT COUNT(c.id) AS employeeCount, " +
           "MIN(c.annualSalary) AS minSalary, " +
           "MAX(c.annualSalary) AS maxSalary, " +
           "AVG(c.annualSalary) AS averageSalary " +
           "FROM CompensationRecord c " +
           "JOIN c.employee e " +
           "WHERE e.country.code = :countryCode " +
           "AND e.joinedAt <= :asOfDate " +
           "AND e.deletedAt IS NULL " +
           "AND (e.terminatedAt IS NULL OR e.terminatedAt > :asOfDate) " +
           "AND c.effectiveFrom <= :asOfDate " +
           "AND (c.effectiveTo IS NULL OR c.effectiveTo > :asOfDate)")
    CountrySalarySummaryProjection getCountrySalarySummary(
            @Param("countryCode") String countryCode,
            @Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT e.jobTitle AS jobTitle, " +
           "COUNT(c.id) AS employeeCount, " +
           "AVG(c.annualSalary) AS averageSalary " +
           "FROM CompensationRecord c " +
           "JOIN c.employee e " +
           "WHERE e.country.code = :countryCode " +
           "AND e.joinedAt <= :asOfDate " +
           "AND e.deletedAt IS NULL " +
           "AND (e.terminatedAt IS NULL OR e.terminatedAt > :asOfDate) " +
           "AND c.effectiveFrom <= :asOfDate " +
           "AND (c.effectiveTo IS NULL OR c.effectiveTo > :asOfDate) " +
           "GROUP BY e.jobTitle " +
           "ORDER BY e.jobTitle ASC")
    List<JobTitleSalarySummaryProjection> getSalaryByJobTitle(
            @Param("countryCode") String countryCode,
            @Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT e.jobTitle AS jobTitle, " +
           "COUNT(c.id) AS employeeCount, " +
           "AVG(c.annualSalary) AS averageSalary " +
           "FROM CompensationRecord c " +
           "JOIN c.employee e " +
           "WHERE e.country.code = :countryCode " +
           "AND LOWER(e.jobTitle) = LOWER(:jobTitle) " +
           "AND e.joinedAt <= :asOfDate " +
           "AND e.deletedAt IS NULL " +
           "AND (e.terminatedAt IS NULL OR e.terminatedAt > :asOfDate) " +
           "AND c.effectiveFrom <= :asOfDate " +
           "AND (c.effectiveTo IS NULL OR c.effectiveTo > :asOfDate) " +
           "GROUP BY e.jobTitle " +
           "ORDER BY e.jobTitle ASC")
    List<JobTitleSalarySummaryProjection> getSalaryByJobTitleAndCountry(
            @Param("countryCode") String countryCode,
            @Param("asOfDate") LocalDate asOfDate,
            @Param("jobTitle") String jobTitle);
}

