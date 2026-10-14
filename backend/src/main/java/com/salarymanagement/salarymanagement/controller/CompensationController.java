package com.salarymanagement.salarymanagement.controller;

import com.salarymanagement.salarymanagement.dto.request.ChangeSalaryRequest;
import com.salarymanagement.salarymanagement.dto.request.CreateCompensationRequest;
import com.salarymanagement.salarymanagement.dto.response.CompensationResponse;
import com.salarymanagement.salarymanagement.service.CompensationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees/{employeeId}/compensations")
public class CompensationController {

    private final CompensationService compensationService;

    public CompensationController(CompensationService compensationService) {
        this.compensationService = compensationService;
    }

    @PostMapping
    public ResponseEntity<CompensationResponse> assignInitialSalary(
            @PathVariable UUID employeeId,
            @Valid @RequestBody CreateCompensationRequest request) {
        CompensationResponse response = compensationService.assignInitialSalary(employeeId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CompensationResponse>> getCompensationHistory(@PathVariable UUID employeeId) {
        List<CompensationResponse> response = compensationService.getCompensationHistory(employeeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/changes")
    public ResponseEntity<CompensationResponse> recordSalaryChange(
            @PathVariable UUID employeeId,
            @Valid @RequestBody ChangeSalaryRequest request) {
        CompensationResponse response = compensationService.recordSalaryChange(employeeId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
