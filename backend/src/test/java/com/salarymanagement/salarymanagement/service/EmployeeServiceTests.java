package com.salarymanagement.salarymanagement.service;

import com.salarymanagement.salarymanagement.dto.request.CreateEmployeeRequest;
import com.salarymanagement.salarymanagement.dto.request.UpdateEmployeeRequest;
import com.salarymanagement.salarymanagement.dto.response.EmployeeResponse;
import com.salarymanagement.salarymanagement.entity.Country;
import com.salarymanagement.salarymanagement.entity.Employee;
import com.salarymanagement.salarymanagement.exception.CountryNotFoundException;
import com.salarymanagement.salarymanagement.exception.DuplicateResourceException;
import com.salarymanagement.salarymanagement.exception.ResourceNotFoundException;
import com.salarymanagement.salarymanagement.mapper.EmployeeMapper;
import com.salarymanagement.salarymanagement.repository.CountryRepository;
import com.salarymanagement.salarymanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTests {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CountryRepository countryRepository;

    @Spy
    private EmployeeMapper employeeMapper = new EmployeeMapper();

    @InjectMocks
    private EmployeeService employeeService;

    private Country country;
    private CreateEmployeeRequest validRequest;

    @BeforeEach
    void setUp() {
        country = new Country("US", "United States", "USD");
        validRequest = new CreateEmployeeRequest(
                "EMP001",
                "Alice",
                "Smith",
                "alice.smith@example.com",
                "US",
                "Software Engineer",
                "Engineering",
                LocalDate.of(2025, 1, 15),
                null
        );
    }

    @Test
    @DisplayName("should create employee successfully when request is valid")
    void shouldCreateEmployeeSuccessfully() {
        when(countryRepository.findById("US")).thenReturn(Optional.of(country));
        when(employeeRepository.existsByEmployeeNumber("EMP001")).thenReturn(false);
        when(employeeRepository.existsByEmail("alice.smith@example.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponse response = employeeService.createEmployee(validRequest);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("EMP001", response.getEmployeeNumber());
        assertEquals("Alice", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        assertEquals("alice.smith@example.com", response.getEmail());
        assertEquals("US", response.getCountryCode());
        assertEquals("Software Engineer", response.getJobTitle());
        assertEquals("Engineering", response.getDepartment());
        assertEquals(LocalDate.of(2025, 1, 15), response.getJoinedAt());

        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when terminatedAt is before joinedAt")
    void shouldThrowExceptionWhenTerminatedAtBeforeJoinedAt() {
        validRequest.setJoinedAt(LocalDate.of(2025, 6, 1));
        validRequest.setTerminatedAt(LocalDate.of(2025, 5, 1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                employeeService.createEmployee(validRequest)
        );

        assertEquals("Termination date cannot be before joining date", ex.getMessage());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("should throw CountryNotFoundException when country does not exist")
    void shouldThrowExceptionWhenCountryNotFound() {
        when(countryRepository.findById("US")).thenReturn(Optional.empty());

        CountryNotFoundException ex = assertThrows(CountryNotFoundException.class, () ->
                employeeService.createEmployee(validRequest)
        );

        assertEquals("Country not found with code: US", ex.getMessage());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when employee number already exists")
    void shouldThrowExceptionWhenEmployeeNumberExists() {
        when(countryRepository.findById("US")).thenReturn(Optional.of(country));
        when(employeeRepository.existsByEmployeeNumber("EMP001")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                employeeService.createEmployee(validRequest)
        );

        assertEquals("Employee number already exists: EMP001", ex.getMessage());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        when(countryRepository.findById("US")).thenReturn(Optional.of(country));
        when(employeeRepository.existsByEmployeeNumber("EMP001")).thenReturn(false);
        when(employeeRepository.existsByEmail("alice.smith@example.com")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                employeeService.createEmployee(validRequest)
        );

        assertEquals("Email already exists: alice.smith@example.com", ex.getMessage());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("should retrieve employee by id successfully")
    void shouldRetrieveEmployeeByIdSuccessfully() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP001",
                "Alice",
                "Smith",
                "alice.smith@example.com",
                country,
                "Software Engineer",
                "Engineering",
                LocalDate.of(2025, 1, 15),
                null,
                null
        );

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        EmployeeResponse response = employeeService.getEmployeeById(employeeId);

        assertNotNull(response);
        assertEquals(employeeId, response.getId());
        assertEquals("EMP001", response.getEmployeeNumber());
        assertEquals("Alice", response.getFirstName());
        assertEquals("US", response.getCountryCode());
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when employee does not exist")
    void shouldThrowExceptionWhenEmployeeNotFound() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                employeeService.getEmployeeById(employeeId)
        );

        assertEquals("Employee not found with id: " + employeeId, ex.getMessage());
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when employee is soft deleted")
    void shouldThrowExceptionWhenEmployeeIsSoftDeleted() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP001",
                "Alice",
                "Smith",
                "alice.smith@example.com",
                country,
                "Software Engineer",
                "Engineering",
                LocalDate.of(2025, 1, 15),
                null,
                Instant.now()
        );

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                employeeService.getEmployeeById(employeeId)
        );

        assertEquals("Employee not found with id: " + employeeId, ex.getMessage());
    }

    @Test
    @DisplayName("should update employee successfully")
    void shouldUpdateEmployeeSuccessfully() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP001",
                "Alice",
                "Smith",
                "alice.smith@example.com",
                country,
                "Software Engineer",
                "Engineering",
                LocalDate.of(2025, 1, 15),
                null,
                null
        );

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
                "Alicia",
                "Smith-Johnson",
                "alicia.johnson@example.com",
                "US",
                "Lead Engineer",
                "Architecture",
                LocalDate.of(2025, 12, 31)
        );

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(countryRepository.existsById("US")).thenReturn(true);
        when(employeeRepository.existsByEmailAndIdNot("alicia.johnson@example.com", employeeId)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse response = employeeService.updateEmployee(employeeId, updateRequest);

        assertNotNull(response);
        assertEquals(employeeId, response.getId());
        assertEquals("EMP001", response.getEmployeeNumber()); // Unchanged
        assertEquals("Alicia", response.getFirstName());
        assertEquals("Smith-Johnson", response.getLastName());
        assertEquals("alicia.johnson@example.com", response.getEmail());
        assertEquals("Lead Engineer", response.getJobTitle());
        assertEquals("Architecture", response.getDepartment());
        assertEquals(LocalDate.of(2025, 12, 31), response.getTerminatedAt());
        verify(employeeRepository).save(employee);
    }

    @Test
    @DisplayName("should allow updating employee with the same email")
    void shouldAllowUpdatingWithSameEmail() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP001",
                "Alice",
                "Smith",
                "alice.smith@example.com",
                country,
                "Software Engineer",
                "Engineering",
                LocalDate.of(2025, 1, 15),
                null,
                null
        );

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
                "Alice",
                "Smith",
                "alice.smith@example.com",
                null,
                "Senior Engineer",
                "Engineering",
                null
        );

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse response = employeeService.updateEmployee(employeeId, updateRequest);

        assertNotNull(response);
        assertEquals("Senior Engineer", response.getJobTitle());
        verify(employeeRepository, never()).existsByEmailAndIdNot(any(), any());
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when updating email to one used by another employee")
    void shouldThrowExceptionWhenUpdateEmailTaken() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP001",
                "Alice",
                "Smith",
                "alice.smith@example.com",
                country,
                "Software Engineer",
                "Engineering",
                LocalDate.of(2025, 1, 15),
                null,
                null
        );

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
                "Alice",
                "Smith",
                "taken@example.com",
                null,
                "Software Engineer",
                "Engineering",
                null
        );

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot("taken@example.com", employeeId)).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                employeeService.updateEmployee(employeeId, updateRequest)
        );

        assertEquals("Email already exists: taken@example.com", ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when update termination date is before joining date")
    void shouldThrowExceptionWhenUpdateTerminationBeforeJoining() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP001",
                "Alice",
                "Smith",
                "alice.smith@example.com",
                country,
                "Software Engineer",
                "Engineering",
                LocalDate.of(2025, 1, 15),
                null,
                null
        );

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
                "Alice",
                "Smith",
                "alice.smith@example.com",
                null,
                "Software Engineer",
                "Engineering",
                LocalDate.of(2025, 1, 1) // Before joinedAt 2025-01-15
        );

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                employeeService.updateEmployee(employeeId, updateRequest)
        );

        assertEquals("Termination date cannot be before joining date", ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw CountryNotFoundException when updating with nonexistent country code")
    void shouldThrowExceptionWhenUpdateCountryNotFound() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP001",
                "Alice",
                "Smith",
                "alice.smith@example.com",
                country,
                "Software Engineer",
                "Engineering",
                LocalDate.of(2025, 1, 15),
                null,
                null
        );

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
                "Alice",
                "Smith",
                "alice.smith@example.com",
                "NONEXIST",
                "Software Engineer",
                "Engineering",
                null
        );

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(countryRepository.existsById("NONEXIST")).thenReturn(false);

        CountryNotFoundException ex = assertThrows(CountryNotFoundException.class, () ->
                employeeService.updateEmployee(employeeId, updateRequest)
        );

        assertEquals("Country not found with code: NONEXIST", ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when attempting to change country code")
    void shouldThrowExceptionWhenAttemptingToChangeCountry() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP001",
                "Alice",
                "Smith",
                "alice.smith@example.com",
                country,
                "Software Engineer",
                "Engineering",
                LocalDate.of(2025, 1, 15),
                null,
                null
        );

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
                "Alice",
                "Smith",
                "alice.smith@example.com",
                "CA",
                "Software Engineer",
                "Engineering",
                null
        );

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(countryRepository.existsById("CA")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                employeeService.updateEmployee(employeeId, updateRequest)
        );

        assertEquals("Changing employee country is not allowed: country-transfer workflows are excluded", ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("should soft-delete employee by setting deletedAt timestamp")
    void shouldSoftDeleteEmployeeSuccessfully() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP001",
                "Alice",
                "Smith",
                "alice.smith@example.com",
                country,
                "Software Engineer",
                "Engineering",
                LocalDate.of(2025, 1, 15),
                null,
                null
        );

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        employeeService.deleteEmployee(employeeId);

        assertNotNull(employee.getDeletedAt());
        verify(employeeRepository).save(employee);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when deleting an already soft-deleted employee")
    void shouldThrowExceptionWhenDeletingAlreadyDeletedEmployee() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP001",
                "Alice",
                "Smith",
                "alice.smith@example.com",
                country,
                "Software Engineer",
                "Engineering",
                LocalDate.of(2025, 1, 15),
                null,
                Instant.now() // Already deleted
        );

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                employeeService.deleteEmployee(employeeId)
        );

        assertEquals("Employee not found with id: " + employeeId, ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }
}
