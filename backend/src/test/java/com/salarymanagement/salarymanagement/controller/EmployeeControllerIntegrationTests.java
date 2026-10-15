package com.salarymanagement.salarymanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salarymanagement.salarymanagement.dto.request.CreateEmployeeRequest;
import com.salarymanagement.salarymanagement.dto.request.UpdateEmployeeRequest;
import com.salarymanagement.salarymanagement.entity.CompensationRecord;
import com.salarymanagement.salarymanagement.entity.Country;
import com.salarymanagement.salarymanagement.entity.Employee;
import com.salarymanagement.salarymanagement.repository.CompensationRecordRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Autowired
    private CompensationRecordRepository compensationRecordRepository;

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

    @Test
    @DisplayName("should update employee successfully and return 200 OK")
    void shouldUpdateEmployeeSuccessfully() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP2001",
                "Grace",
                "Hopper",
                "grace.hopper.2001@example.com",
                countryUS,
                "Computer Scientist",
                "R&D",
                LocalDate.of(2024, 1, 1),
                null,
                null
        );
        employeeRepository.save(employee);

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
                "Amazing Grace",
                "Hopper-Brewer",
                "grace.brewer.2001@example.com",
                "US",
                "Chief Scientist",
                "Advanced Computing",
                LocalDate.of(2026, 12, 31)
        );

        mockMvc.perform(put("/api/v1/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employeeId.toString()))
                .andExpect(jsonPath("$.employeeNumber").value("EMP2001"))
                .andExpect(jsonPath("$.firstName").value("Amazing Grace"))
                .andExpect(jsonPath("$.lastName").value("Hopper-Brewer"))
                .andExpect(jsonPath("$.email").value("grace.brewer.2001@example.com"))
                .andExpect(jsonPath("$.countryCode").value("US"))
                .andExpect(jsonPath("$.jobTitle").value("Chief Scientist"))
                .andExpect(jsonPath("$.department").value("Advanced Computing"))
                .andExpect(jsonPath("$.joinedAt").value("2024-01-01"))
                .andExpect(jsonPath("$.terminatedAt").value("2026-12-31"));

        Employee updated = employeeRepository.findById(employeeId).orElseThrow();
        assertEquals("Amazing Grace", updated.getFirstName());
        assertEquals("grace.brewer.2001@example.com", updated.getEmail());
    }

    @Test
    @DisplayName("should allow update when keeping the same email")
    void shouldAllowUpdateWithSameEmail() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP2002",
                "Helen",
                "Keller",
                "helen.keller.2002@example.com",
                countryUS,
                "Author",
                "Communications",
                LocalDate.of(2024, 1, 1),
                null,
                null
        );
        employeeRepository.save(employee);

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
                "Helen",
                "Keller",
                "helen.keller.2002@example.com",
                null,
                "Senior Author",
                "Communications",
                null
        );

        mockMvc.perform(put("/api/v1/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("Senior Author"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when update fields are invalid")
    void shouldReturnBadRequestWhenUpdateFieldsAreInvalid() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP2003",
                "Isaac",
                "Newton",
                "isaac.newton.2003@example.com",
                countryUS,
                "Physicist",
                "Research",
                LocalDate.of(2024, 1, 1),
                null,
                null
        );
        employeeRepository.save(employee);

        UpdateEmployeeRequest invalidRequest = new UpdateEmployeeRequest(
                "",
                "",
                "invalid-email-format",
                null,
                "",
                null,
                null
        );

        mockMvc.perform(put("/api/v1/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("should return 409 Conflict when update email already belongs to another employee")
    void shouldReturnConflictWhenUpdateEmailBelongsToAnotherEmployee() throws Exception {
        Employee emp1 = new Employee(
                UUID.randomUUID(),
                "EMP2004",
                "John",
                "Locke",
                "john.locke.2004@example.com",
                countryUS,
                "Philosopher",
                "Humanities",
                LocalDate.of(2024, 1, 1),
                null,
                null
        );
        employeeRepository.save(emp1);

        UUID emp2Id = UUID.randomUUID();
        Employee emp2 = new Employee(
                emp2Id,
                "EMP2005",
                "Jean",
                "Rousseau",
                "jean.rousseau.2005@example.com",
                countryUS,
                "Writer",
                "Humanities",
                LocalDate.of(2024, 1, 1),
                null,
                null
        );
        employeeRepository.save(emp2);

        UpdateEmployeeRequest duplicateEmailRequest = new UpdateEmployeeRequest(
                "Jean-Jacques",
                "Rousseau",
                "john.locke.2004@example.com", // Belongs to emp1
                null,
                "Writer",
                "Humanities",
                null
        );

        mockMvc.perform(put("/api/v1/employees/{id}", emp2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmailRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already exists: john.locke.2004@example.com"));
    }

    @Test
    @DisplayName("should return 404 Not Found when updating a nonexistent employee")
    void shouldReturnNotFoundWhenUpdatingNonexistentEmployee() throws Exception {
        UUID randomId = UUID.randomUUID();
        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "Ghost",
                "User",
                "ghost@example.com",
                null,
                "Phantom",
                null,
                null
        );

        mockMvc.perform(put("/api/v1/employees/{id}", randomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Employee not found with id: " + randomId));
    }

    @Test
    @DisplayName("should return 400 Bad Request when update termination date is earlier than join date")
    void shouldReturnBadRequestWhenUpdateTerminationDateBeforeJoinDate() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP2006",
                "Katherine",
                "Johnson",
                "katherine.johnson.2006@example.com",
                countryUS,
                "Mathematician",
                "Aerospace",
                LocalDate.of(2025, 6, 1),
                null,
                null
        );
        employeeRepository.save(employee);

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "Katherine",
                "Johnson",
                "katherine.johnson.2006@example.com",
                null,
                "Mathematician",
                "Aerospace",
                LocalDate.of(2025, 5, 1) // Before joinedAt 2025-06-01
        );

        mockMvc.perform(put("/api/v1/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Termination date cannot be before joining date"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when attempting to change country code")
    void shouldReturnBadRequestWhenAttemptingToChangeCountryCode() throws Exception {
        Country countryGB = countryRepository.findById("GB")
                .orElseGet(() -> countryRepository.save(new Country("GB", "United Kingdom", "GBP")));

        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP2007",
                "Leonardo",
                "DaVinci",
                "leonardo.davinci.2007@example.com",
                countryUS,
                "Polymath",
                "Arts & Science",
                LocalDate.of(2024, 1, 1),
                null,
                null
        );
        employeeRepository.save(employee);

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "Leonardo",
                "DaVinci",
                "leonardo.davinci.2007@example.com",
                "GB", // Attempting country change
                "Polymath",
                "Arts & Science",
                null
        );

        mockMvc.perform(put("/api/v1/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Changing employee country is not allowed: country-transfer workflows are excluded"));
    }

    @Test
    @DisplayName("should soft-delete employee, return 204 No Content, and preserve physical employee and compensation history")
    void shouldSoftDeleteEmployeeAndPreserveCompensationHistory() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP2008",
                "Marie",
                "Curie",
                "marie.curie.2008@example.com",
                countryUS,
                "Chemist",
                "Research",
                LocalDate.of(2024, 1, 1),
                null,
                null
        );
        employeeRepository.save(employee);

        UUID compRecordId = UUID.randomUUID();
        CompensationRecord compensation = new CompensationRecord(
                compRecordId,
                employee,
                new BigDecimal("150000.00"),
                LocalDate.of(2024, 1, 1),
                null,
                Instant.now()
        );
        compensationRecordRepository.save(compensation);

        // Execute soft delete
        mockMvc.perform(delete("/api/v1/employees/{id}", employeeId))
                .andExpect(status().isNoContent());

        // Verify entity still physically exists in PostgreSQL
        Optional<Employee> persistedEmployee = employeeRepository.findById(employeeId);
        assertTrue(persistedEmployee.isPresent());
        assertNotNull(persistedEmployee.get().getDeletedAt(), "deletedAt must be populated");

        // Verify compensation record is physically preserved
        Optional<CompensationRecord> persistedComp = compensationRecordRepository.findById(compRecordId);
        assertTrue(persistedComp.isPresent(), "Compensation record must remain physically in database");
        assertEquals(0, new BigDecimal("150000.00").compareTo(persistedComp.get().getAnnualSalary()));
    }

    @Test
    @DisplayName("should return 404 Not Found when retrieving a soft-deleted employee")
    void shouldReturnNotFoundWhenRetrievingDeletedEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP2009",
                "Nikola",
                "Tesla",
                "nikola.tesla.2009@example.com",
                countryUS,
                "Inventor",
                "Electrical Engineering",
                LocalDate.of(2024, 1, 1),
                null,
                Instant.now() // Soft-deleted
        );
        employeeRepository.save(employee);

        mockMvc.perform(get("/api/v1/employees/{id}", employeeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Employee not found with id: " + employeeId));
    }

    @Test
    @DisplayName("should return 404 Not Found on repeated deletion of an already-deleted employee")
    void shouldReturnNotFoundOnRepeatedDeletion() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee(
                employeeId,
                "EMP2010",
                "Oliver",
                "Heaviside",
                "oliver.heaviside.2010@example.com",
                countryUS,
                "Engineer",
                "Math",
                LocalDate.of(2024, 1, 1),
                null,
                null
        );
        employeeRepository.save(employee);

        // First deletion -> 204 No Content
        mockMvc.perform(delete("/api/v1/employees/{id}", employeeId))
                .andExpect(status().isNoContent());

        // Repeated deletion -> 404 Not Found
        mockMvc.perform(delete("/api/v1/employees/{id}", employeeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Employee not found with id: " + employeeId));
    }

    @Test
    @DisplayName("should return 404 Not Found when deleting a nonexistent employee")
    void shouldReturnNotFoundWhenDeletingNonexistentEmployee() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/employees/{id}", randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Employee not found with id: " + randomId));
    }

    private Employee createDirectoryEmployee(String empNumber, String firstName, String lastName,
                                             String email, Country country, String jobTitle,
                                             Instant deletedAt) {
        Employee employee = new Employee(
                UUID.randomUUID(),
                empNumber,
                firstName,
                lastName,
                email,
                country,
                jobTitle,
                "Engineering",
                LocalDate.of(2025, 1, 1),
                null,
                deletedAt
        );
        return employeeRepository.save(employee);
    }

    @Test
    @DisplayName("should return default paginated employees sorted by employee number ascending")
    void shouldReturnDefaultPaginatedEmployeesSortedByEmployeeNumberAsc() throws Exception {
        createDirectoryEmployee("EMP_DIR_03", "Charlie", "Brown", "charlie.dir@example.com", countryUS, "Developer", null);
        createDirectoryEmployee("EMP_DIR_01", "Alice", "Wonderland", "alice.dir@example.com", countryUS, "Architect", null);
        createDirectoryEmployee("EMP_DIR_02", "Bob", "Builder", "bob.dir@example.com", countryUS, "Engineer", null);

        mockMvc.perform(get("/api/v1/employees")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].employeeNumber").value("EMP_DIR_01"))
                .andExpect(jsonPath("$.content[1].employeeNumber").value("EMP_DIR_02"))
                .andExpect(jsonPath("$.content[2].employeeNumber").value("EMP_DIR_03"));
    }

    @Test
    @DisplayName("should search partially and case-insensitively across employeeNumber, firstName, lastName, and email")
    void shouldSearchAcrossEmployeeNumberFirstLastAndEmailCaseInsensitively() throws Exception {
        createDirectoryEmployee("EMP_ALPHA_10", "Ada", "Lovelace", "ada.lovelace@math.org", countryUS, "Scientist", null);
        createDirectoryEmployee("EMP_BETA_20", "Charles", "Babbage", "charles@computing.org", countryUS, "Inventor", null);

        // Search by partial employee number (case-insensitive)
        mockMvc.perform(get("/api/v1/employees").param("search", "alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].employeeNumber").value("EMP_ALPHA_10"));

        // Search by partial first name
        mockMvc.perform(get("/api/v1/employees").param("search", "ADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Ada"));

        // Search by partial last name
        mockMvc.perform(get("/api/v1/employees").param("search", "babb"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].lastName").value("Babbage"));

        // Search by partial email
        mockMvc.perform(get("/api/v1/employees").param("search", "math.org"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("ada.lovelace@math.org"));
    }

    @Test
    @DisplayName("should filter by country and job title individually and combined")
    void shouldFilterByCountryCodeAndJobTitleIndividuallyAndCombined() throws Exception {
        Country countryGB = countryRepository.findById("GB")
                .orElseGet(() -> countryRepository.save(new Country("GB", "United Kingdom", "GBP")));

        createDirectoryEmployee("EMP_FLT_01", "Flt1", "One", "flt1@example.com", countryUS, "Backend Engineer", null);
        createDirectoryEmployee("EMP_FLT_02", "Flt2", "Two", "flt2@example.com", countryUS, "Frontend Engineer", null);
        createDirectoryEmployee("EMP_FLT_03", "Flt3", "Three", "flt3@example.com", countryGB, "Backend Engineer", null);

        // Filter countryCode=US
        mockMvc.perform(get("/api/v1/employees").param("countryCode", "US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].countryCode").value("US"))
                .andExpect(jsonPath("$.content[1].countryCode").value("US"));

        // Filter countryCode=us (case-insensitive)
        mockMvc.perform(get("/api/v1/employees").param("countryCode", "us"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // Filter jobTitle=Backend Engineer
        mockMvc.perform(get("/api/v1/employees").param("jobTitle", "Backend Engineer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].jobTitle").value("Backend Engineer"))
                .andExpect(jsonPath("$.content[1].jobTitle").value("Backend Engineer"));

        // Combined filter countryCode=US and jobTitle=Backend Engineer
        mockMvc.perform(get("/api/v1/employees")
                        .param("countryCode", "US")
                        .param("jobTitle", "Backend Engineer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].employeeNumber").value("EMP_FLT_01"));

        // Combined filter countryCode=GB and jobTitle=Frontend Engineer (no match)
        mockMvc.perform(get("/api/v1/employees")
                        .param("countryCode", "GB")
                        .param("jobTitle", "Frontend Engineer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("should exclude soft-deleted employees from directory results")
    void shouldExcludeSoftDeletedEmployeesFromDirectory() throws Exception {
        createDirectoryEmployee("EMP_ACT_01", "Active", "Employee", "active.dir@example.com", countryUS, "Dev", null);
        createDirectoryEmployee("EMP_DEL_02", "Deleted", "Employee", "deleted.dir@example.com", countryUS, "Dev", Instant.now());

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].employeeNumber").value("EMP_ACT_01"));
    }

    @Test
    @DisplayName("should handle pagination parameters and hasNext flag accurately")
    void shouldHandlePaginationParametersAndHasNextFlag() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createDirectoryEmployee("EMP_PAGE_" + i, "Name" + i, "Last" + i, "page" + i + "@example.com", countryUS, "Dev", null);
        }

        // Page 0 (size 2) -> elements 1, 2 (hasNext = true)
        mockMvc.perform(get("/api/v1/employees").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].employeeNumber").value("EMP_PAGE_1"))
                .andExpect(jsonPath("$.content[1].employeeNumber").value("EMP_PAGE_2"));

        // Page 1 (size 2) -> elements 3, 4 (hasNext = true)
        mockMvc.perform(get("/api/v1/employees").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].employeeNumber").value("EMP_PAGE_3"))
                .andExpect(jsonPath("$.content[1].employeeNumber").value("EMP_PAGE_4"));

        // Page 2 (size 2) -> element 5 (hasNext = false)
        mockMvc.perform(get("/api/v1/employees").param("page", "2").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].employeeNumber").value("EMP_PAGE_5"));
    }

    @Test
    @DisplayName("should return empty page when no employees match filters")
    void shouldReturnEmptyPageWhenNoEmployeesMatch() throws Exception {
        createDirectoryEmployee("EMP_EXIST_01", "Exist", "One", "exist@example.com", countryUS, "Dev", null);

        mockMvc.perform(get("/api/v1/employees").param("search", "NonExistentKeyword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("should return 400 Bad Request for invalid pagination parameters")
    void shouldReturnBadRequestForInvalidPaginationParameters() throws Exception {
        // Negative page index
        mockMvc.perform(get("/api/v1/employees").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Page index must not be less than zero")));

        // Page size less than 1
        mockMvc.perform(get("/api/v1/employees").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Page size must not be less than one")));

        // Page size greater than 100
        mockMvc.perform(get("/api/v1/employees").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Page size must not be greater than 100")));

        // Non-numeric parameter
        mockMvc.perform(get("/api/v1/employees").param("page", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Invalid parameter value for: page")));
    }
}

