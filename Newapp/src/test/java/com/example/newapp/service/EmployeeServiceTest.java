package com.example.newapp.service;
import com.example.newapp.entity.Employee;
import com.example.newapp.exception.ResourceNotFoundException;
import com.example.newapp.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        employee = new Employee();
        employee.setId(1L);
        employee.setName("John Doe");
        employee.setDepartment("IT");
        employee.setEmail("john@example.com");
    }

    // Basic CRUD Tests

    @Test
    void testGetAllEmployees() {
        List<Employee> employees = Collections.singletonList(employee);
        when(employeeRepository.findAll()).thenReturn(employees);

        List<Employee> result = employeeService.getAllEmployees();

        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
        verify(employeeRepository, times(1)).findAll();
    }

    @Test
    void testSaveEmployee() {
        when(employeeRepository.save(employee)).thenReturn(employee);

        Employee saved = employeeService.saveEmployee(employee);

        assertNotNull(saved);
        assertEquals("john@example.com", saved.getEmail());
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    void testDeleteEmployee() {
        Long id = 1L;
        when(employeeRepository.existsById(id)).thenReturn(true);
        doNothing().when(employeeRepository).deleteById(id);

        employeeService.deleteEmployee(id);

        verify(employeeRepository, times(1)).existsById(id);
        verify(employeeRepository, times(1)).deleteById(id);
    }

    @Test
    void testDeleteEmployeeNotFoundThrowsException() {
        Long employeeId = 99L;
        when(employeeRepository.existsById(employeeId)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            employeeService.deleteEmployee(employeeId);
        });

        assertEquals("Employee with ID 99 not found", exception.getMessage());
        verify(employeeRepository, times(1)).existsById(employeeId);
        verify(employeeRepository, never()).deleteById(anyLong());
    }

    @Test
    void testGetEmployeeById() {
        Long id = 1L;
        when(employeeRepository.findById(id)).thenReturn(Optional.of(employee));

        Employee found = employeeService.getEmployeeById(id);

        assertNotNull(found);
        assertEquals("John Doe", found.getName());
        verify(employeeRepository, times(1)).findById(id);
    }

    @Test
    void testGetEmployeeByIdNotFound() {
        Long id = 99L;
        when(employeeRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            employeeService.getEmployeeById(id);
        });

        assertEquals("Employee with ID 99 not found", exception.getMessage());
        verify(employeeRepository, times(1)).findById(id);
    }

    @Test
    void testUpdateEmployee() {
        Long id = 1L;

        Employee updatedDetails = new Employee();
        updatedDetails.setName("Jane Doe");
        updatedDetails.setEmail("jane@example.com");
        updatedDetails.setDepartment("HR");

        when(employeeRepository.findById(id)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(updatedDetails);

        Employee updated = employeeService.updateEmployee(id, updatedDetails);

        assertEquals("Jane Doe", updated.getName());
        assertEquals("jane@example.com", updated.getEmail());
        assertEquals("HR", updated.getDepartment());
        verify(employeeRepository, times(1)).findById(id);
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    void testUpdateEmployeeNotFound() {
        Long id = 100L;

        Employee newDetails = new Employee();
        newDetails.setName("Someone");
        newDetails.setEmail("some@example.com");

        when(employeeRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            employeeService.updateEmployee(id, newDetails);
        });

        assertEquals("Employee with ID 100 not found", exception.getMessage());
        verify(employeeRepository, times(1)).findById(id);
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    // Edge Case & Null Handling Tests

    @Test
    void testSaveEmployeeWithNullName() {
        Employee invalidEmployee = new Employee();
        invalidEmployee.setEmail("nullname@example.com");
        invalidEmployee.setDepartment("IT");

        when(employeeRepository.save(any(Employee.class))).thenReturn(invalidEmployee);

        Employee saved = employeeService.saveEmployee(invalidEmployee);

        assertNotNull(saved);
        assertNull(saved.getName());
        verify(employeeRepository, times(1)).save(invalidEmployee);
    }

    @Test
    void testGetAllEmployeesEmptyList() {
        when(employeeRepository.findAll()).thenReturn(Collections.emptyList());

        List<Employee> employees = employeeService.getAllEmployees();

        assertTrue(employees.isEmpty());
        verify(employeeRepository, times(1)).findAll();
    }

    @Test
    void testDeleteEmployeeWithNullId() {
        assertThrows(IllegalArgumentException.class, () -> {
            employeeService.deleteEmployee(null);
        });
    }

    @Test
    void testUpdateEmployeeWithPartialData() {
        Long id = 1L;

        Employee update = new Employee();
        update.setName(null);
        update.setEmail("updated@example.com");
        update.setDepartment(null);

        when(employeeRepository.findById(id)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee updated = employeeService.updateEmployee(id, update);

        assertEquals("updated@example.com", updated.getEmail());
        assertNull(updated.getName());
        assertNull(updated.getDepartment());
    }

    @Test
    void testSaveEmployeeWithExistingId() {
        employee.setId(10L);

        when(employeeRepository.save(employee)).thenReturn(employee);

        Employee saved = employeeService.saveEmployee(employee);

        assertEquals(10L, saved.getId());
        verify(employeeRepository, times(1)).save(employee);
    }

    // Additional Defensive Test: Save with Null Employee
    @Test
    void testSaveNullEmployeeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            employeeService.saveEmployee(null);
        });
    }

    // Additional Defensive Test: Update with Null Details
    @Test
    void testUpdateWithNullDetailsThrowsException() {
        Long id = 1L;
        assertThrows(IllegalArgumentException.class, () -> {
            employeeService.updateEmployee(id, null);
        });
    }
    @Test
    void testSaveEmployeeWithEmptyEmailThrowsException() {
        Employee invalid = new Employee();
        invalid.setName("Valid Name");
        invalid.setEmail("");
        invalid.setDepartment("IT");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            employeeService.saveEmployee(invalid);
        });

        assertEquals("Employee email cannot be empty", ex.getMessage());
    }
    @Test
    void testSaveEmployeeWithEmptyNameThrowsException() {
        Employee invalid = new Employee();
        invalid.setName("");
        invalid.setEmail("valid@email.com");
        invalid.setDepartment("HR");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            employeeService.saveEmployee(invalid);
        });

        assertEquals("Employee name cannot be empty", ex.getMessage());
    }
    @Test
    void testSaveEmployeeWithDuplicateEmailThrowsCustomException() {
        when(employeeRepository.save(any(Employee.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate email"));

        EmployeeService.EmployeeAlreadyExistsException ex = assertThrows(EmployeeService.EmployeeAlreadyExistsException.class, () -> {
            employeeService.saveEmployee(employee);
        });

        assertEquals("Employee with this email already exists", ex.getMessage());
    }


    @Test
    void testUpdateEmployeeWithDuplicateEmailThrowsCustomException() {
        Long id = 1L;
        when(employeeRepository.findById(id)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate"));

        Employee newData = new Employee();
        newData.setName("Test");
        newData.setEmail("duplicate@email.com");
        newData.setDepartment("Finance");

        EmployeeService.EmployeeAlreadyExistsException ex = assertThrows(
                EmployeeService.EmployeeAlreadyExistsException.class, () -> {
                    employeeService.updateEmployee(id, newData);
                });

        assertTrue(ex.getMessage().contains("Employee with this email already exists"));
    }



}
