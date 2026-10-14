package com.salarymanagement.salarymanagement.repository;

import com.salarymanagement.salarymanagement.entity.CompensationRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompensationRecordRepository extends JpaRepository<CompensationRecord, UUID> {
}
