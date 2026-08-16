package com.nassim.crm_api.Ticket;

import com.nassim.crm_api.Customer.Customer;
import com.nassim.crm_api.Customer.CustomerRepository;
import com.nassim.crm_api.Employee.Employee;
import com.nassim.crm_api.Employee.EmployeeRepository;
import com.nassim.crm_api.Employee.EmployeeRole;
import com.nassim.crm_api.exception.CustomerNotFoundException;
import com.nassim.crm_api.exception.EmployeeNotFoundException;
import com.nassim.crm_api.exception.InvalidTicketStatusException;
import com.nassim.crm_api.exception.TicketNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private TicketService ticketService;

    private Customer customer;
    private Employee employee;

    @BeforeEach
    void setUp() {
        customer = new Customer("Ada", "Lovelace", "ada@example.com");
        customer.setId(1L);
        employee = new Employee("Grace Hopper", "grace@crm.com", EmployeeRole.AGENT);
        employee.setId(1L);
    }

    private TicketRequest request() {
        return new TicketRequest("Printer broken", "Paper jam in bay 3", Priority.HIGH, 1L, 1L);
    }

    private Ticket ticket() {
        Ticket ticket = new Ticket();
        ticket.setId(10L);
        ticket.setTitle("Printer broken");
        ticket.setPriority(Priority.HIGH);
        ticket.setStatus(Status.OPEN);
        ticket.setCustomer(customer);
        ticket.setAssignedEmployee(employee);
        return ticket;
    }

    @Test
    void createTicket_setsStatusOpenAndSaves() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse result = ticketService.createTicket(request());

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        Ticket saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(Status.OPEN);
        assertThat(saved.getTitle()).isEqualTo("Printer broken");
        assertThat(saved.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(saved.getCustomer().getId()).isEqualTo(1L);
        assertThat(saved.getAssignedEmployee().getId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(Status.OPEN);
        assertThat(result.title()).isEqualTo("Printer broken");
    }

    @Test
    void createTicket_throwsWhenCustomerNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.createTicket(request()))
                .isInstanceOf(CustomerNotFoundException.class);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void createTicket_throwsWhenEmployeeNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.createTicket(request()))
                .isInstanceOf(EmployeeNotFoundException.class);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void getTicketById_returnsResponseWhenFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket()));

        TicketResponse result = ticketService.getTicketById(10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.customer().firstName()).isEqualTo("Ada");
        assertThat(result.assignedEmployee().name()).isEqualTo("Grace Hopper");
    }

    @Test
    void getTicketById_throwsWhenNotFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(99L))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void search_delegatesWithFilters() {
        when(ticketRepository.search(Status.OPEN, Priority.HIGH, 1L, 2L, Pageable.unpaged()))
                .thenReturn(Page.empty());

        Page<TicketResponse> result = ticketService.search(Status.OPEN, Priority.HIGH, 1L, 2L, Pageable.unpaged());

        verify(ticketRepository).search(Status.OPEN, Priority.HIGH, 1L, 2L, Pageable.unpaged());
        assertThat(result).isEmpty();
    }

    @Test
    void search_mapsEntitiesToResponses() {
        when(ticketRepository.search(Status.OPEN, Priority.HIGH, 1L, 2L, Pageable.unpaged()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(ticket())));

        Page<TicketResponse> result = ticketService.search(Status.OPEN, Priority.HIGH, 1L, 2L, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(Status.OPEN);
        assertThat(result.getContent().get(0).customer().email()).isEqualTo("ada@example.com");
    }

    @Test
    void changeStatus_allowsValidTransition() {
        Ticket ticket = ticket();
        ticket.setStatus(Status.IN_PROGRESS);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        TicketResponse result = ticketService.changeStatus(10L, Status.RESOLVED);

        assertThat(result.status()).isEqualTo(Status.RESOLVED);
    }

    @Test
    void changeStatus_rejectsInvalidTransition() {
        Ticket ticket = ticket();
        ticket.setStatus(Status.OPEN);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.changeStatus(10L, Status.CLOSED))
                .isInstanceOf(InvalidTicketStatusException.class)
                .hasMessageContaining("OPEN").hasMessageContaining("CLOSED");
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void changeStatus_throwsWhenTicketNotFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.changeStatus(99L, Status.IN_PROGRESS))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void assignEmployee_reassignsAndSaves() {
        Ticket ticket = ticket();
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        Employee newEmployee = new Employee("Linus", "linus@crm.com", EmployeeRole.SUPPORT);
        newEmployee.setId(2L);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(newEmployee));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        TicketResponse result = ticketService.assignEmployee(10L, 2L);

        assertThat(result.assignedEmployee().id()).isEqualTo(2L);
        assertThat(result.assignedEmployee().name()).isEqualTo("Linus");
    }

    @Test
    void assignEmployee_throwsWhenEmployeeNotFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket()));
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.assignEmployee(10L, 99L))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void updateTicket_updatesAllFields() {
        Ticket ticket = ticket();
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        TicketResponse result = ticketService.updateTicket(10L,
                new TicketRequest("New title", "New desc", Priority.CRITICAL, 1L, 1L));

        assertThat(result.title()).isEqualTo("New title");
        assertThat(result.description()).isEqualTo("New desc");
        assertThat(result.priority()).isEqualTo(Priority.CRITICAL);
    }

    @Test
    void deleteTicket_throwsWhenNotFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.deleteTicket(99L))
                .isInstanceOf(TicketNotFoundException.class);
        verify(ticketRepository, never()).deleteById(99L);
    }
}
