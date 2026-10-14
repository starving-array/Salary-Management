package com.salarymanagement.salarymanagement.mapper;

import com.salarymanagement.salarymanagement.dto.response.CompensationResponse;
import com.salarymanagement.salarymanagement.entity.CompensationRecord;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CompensationMapper {

    public CompensationResponse toResponse(CompensationRecord record, String currencyCode) {
        if (record == null) {
            return null;
        }
        return new CompensationResponse(
                record.getId(),
                record.getEmployee() != null ? record.getEmployee().getId() : null,
                record.getAnnualSalary(),
                currencyCode,
                record.getEffectiveFrom(),
                record.getEffectiveTo(),
                record.getCreatedAt()
        );
    }

    public List<CompensationResponse> toResponseList(List<CompensationRecord> records, String currencyCode) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream()
                .map(r -> toResponse(r, currencyCode))
                .toList();
    }
}
