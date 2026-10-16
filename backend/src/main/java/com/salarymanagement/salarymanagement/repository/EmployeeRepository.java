package com.salarymanagement.salarymanagement.repository;

import com.salarymanagement.salarymanagement.entity.Employee;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Employee e WHERE e.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT DISTINCT e.jobTitle FROM Employee e WHERE e.deletedAt IS NULL AND LOWER(e.jobTitle) LIKE :pattern ORDER BY e.jobTitle ASC")
    List<String> findDistinctJobTitlesMatching(@Param("pattern") String pattern, Pageable pageable);

    @Query("SELECT DISTINCT e.jobTitle FROM Employee e WHERE e.deletedAt IS NULL ORDER BY e.jobTitle ASC")
    List<String> findDistinctJobTitles(Pageable pageable);
}
