package com.salarymanagement.salarymanagement.repository;

import com.salarymanagement.salarymanagement.entity.Employee;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByEmail(String email);
}
