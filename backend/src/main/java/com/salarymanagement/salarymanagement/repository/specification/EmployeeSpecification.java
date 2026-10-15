package com.salarymanagement.salarymanagement.repository.specification;

import com.salarymanagement.salarymanagement.entity.Employee;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class EmployeeSpecification {

    private EmployeeSpecification() {
    }

    public static Specification<Employee> filter(String search, String countryCode, String jobTitle) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude soft-deleted employees
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Exact country filter
            if (countryCode != null && !countryCode.isBlank()) {
                predicates.add(cb.equal(
                        cb.upper(root.get("country").get("code")),
                        countryCode.trim().toUpperCase()
                ));
            }

            // Exact job-title filter
            if (jobTitle != null && !jobTitle.isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("jobTitle")),
                        jobTitle.trim().toLowerCase()
                ));
            }

            // Partial, case-insensitive search across employee number, names, and email
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate numMatch = cb.like(cb.lower(root.get("employeeNumber")), pattern);
                Predicate firstMatch = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate lastMatch = cb.like(cb.lower(root.get("lastName")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                predicates.add(cb.or(numMatch, firstMatch, lastMatch, emailMatch));
            }

            // Avoid N+1: fetch join country only for entity query, not for count query
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("country", JoinType.INNER);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
