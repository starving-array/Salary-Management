package com.salarymanagement.salarymanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salarymanagement.salarymanagement.dto.request.CreateEmployeeRequest;
import com.salarymanagement.salarymanagement.entity.Country;
import com.salarymanagement.salarymanagement.entity.Employee;
import com.salarymanagement.salarymanagement.repository.CountryRepository;
import com.salarymanagement.salarymanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Country countryUS;

    @BeforeEach
    void setUp() {
        countryUS = countryRepository.findById("US")
                .orElseGet(() -> countryRepository.save(new Country("US", "United States", "USD")));
    }

    @Test
    @DisplayName("should create an employee and return 201 Created with Location header and response body")
    void shouldCreateEmployeeSuccessfully() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP1001",
                "Alice",
                "Smith",
                "alice.smith.1001@example.com",
                "US",
                "Software Engineer",
                "Engineering",
                LocalDate.of(2025, 1, 15),
                null
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/employees/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.employeeNumber").value("EMP1001"))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.email").value("alice.smith.1001@example.com"))
                .andExpect(jsonPath("$.countryCode").value("US"))
                .andExpect(jsonPath("$.jobTitle").value("Software Engineer"))
                .andExpect(jsonPath("$.department").value("Engineering"))
                .andExpect(jsonPath("$.joinedAt").value("2025-01-15"))
                .andExpect(jsonPath("$.terminatedAt").doesNotExist());

        assertTrue(employeeRepository.existsByEmployeeNumber("EMP1001"));
    }

    @Test
    @DisplayName("should retrieve an employee by id and return 200 OK")
    void shouldRetrieveEmployeeByIdSuccessfully() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP1002",
                "Bob",
                "Jones",
                "bob.jones.1002@example.com",
                countryUS,
                "Product Manager",
                "Product",
                LocalDate.of(2024, 6, 1),
                null,
                null
        );
        employeeRepository.save(employee);

        mockMvc.perform(get("/api/v1/employees/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employeeId.toString()))
                .andExpect(jsonPath("$.employeeNumber").value("EMP1002"))
                .andExpect(jsonPath("$.firstName").value("Bob"))
                .andExpect(jsonPath("$.lastName").value("Jones"))
                .andExpect(jsonPath("$.email").value("bob.jones.1002@example.com"))
                .andExpect(jsonPath("$.countryCode").value("US"))
                .andExpect(jsonPath("$.jobTitle").value("Product Manager"))
                .andExpect(jsonPath("$.department").value("Product"))
                .andExpect(jsonPath("$.joinedAt").value("2024-06-01"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when request fields are invalid")
    void shouldReturnBadRequestWhenRequestFieldsAreInvalid() throws Exception {
        CreateEmployeeRequest invalidRequest = new CreateEmployeeRequest(
                "",
                "",
                "",
                "invalid-email-format",
                "",
                "",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details", hasItem(containsString("employeeNumber"))))
                .andExpect(jsonPath("$.details", hasItem(containsString("firstName"))))
                .andExpect(jsonPath("$.details", hasItem(containsString("email"))))
                .andExpect(jsonPath("$.details", hasItem(containsString("countryCode"))))
                .andExpect(jsonPath("$.details", hasItem(containsString("jobTitle"))))
                .andExpect(jsonPath("$.details", hasItem(containsString("joinedAt"))));
    }

    @Test
    @DisplayName("should return 400 Bad Request when country code does not exist")
    void shouldReturnBadRequestWhenCountryCodeDoesNotExist() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP1003",
                "Charlie",
                "Brown",
                "charlie.brown.1003@example.com",
                "NONEXIST",
                "Analyst",
                "Finance",
                LocalDate.of(2025, 2, 1),
                null
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Country not found with code: NONEXIST"));
    }

    @Test
    @DisplayName("should return 409 Conflict when employee number already exists")
    void shouldReturnConflictWhenEmployeeNumberAlreadyExists() throws Exception {
        UUID existingId = UUID.randomUUID();
        Employee existing = new Employee(
                existingId,
                "EMP1004",
                "David",
                "Miller",
                "david.miller.1004@example.com",
                countryUS,
                "Designer",
                "Design",
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
        employeeRepository.save(existing);

        CreateEmployeeRequest duplicateNumberRequest = new CreateEmployeeRequest(
                "EMP1004",
                "Different",
                "Person",
                "different.person@example.com",
                "US",
                "Designer",
                "Design",
                LocalDate.of(2025, 1, 1),
                null
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateNumberRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Employee number already exists: EMP1004"));
    }

    @Test
    @DisplayName("should return 409 Conflict when email already exists")
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        UUID existingId = UUID.randomUUID();
        Employee existing = new Employee(
                existingId,
                "EMP1005",
                "Eva",
                "Green",
                "eva.green.1005@example.com",
                countryUS,
                "HR Specialist",
                "Human Resources",
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
        employeeRepository.save(existing);

        CreateEmployeeRequest duplicateEmailRequest = new CreateEmployeeRequest(
                "EMP1006",
                "Another",
                "Person",
                "eva.green.1005@example.com",
                "US",
                "Recruiter",
                "Human Resources",
                LocalDate.of(2025, 1, 1),
                null
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmailRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already exists: eva.green.1005@example.com"));
    }

    @Test
    @DisplayName("should return 404 Not Found when employee does not exist")
    void shouldReturnNotFoundWhenEmployeeDoesNotExist() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/employees/{id}", randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Employee not found with id: " + randomId));
    }

    @Test
    @DisplayName("should return 400 Bad Request when terminatedAt is before joinedAt")
    void shouldReturnBadRequestWhenTerminatedAtBeforeJoinedAt() throws Exception {
        CreateEmployeeRequest invalidDatesRequest = new CreateEmployeeRequest(
                "EMP1007",
                "Frank",
                "Wright",
                "frank.wright.1007@example.com",
                "US",
                "Architect",
                null,
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 5, 1)
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDatesRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Termination date cannot be before joining date"));
    }
}
