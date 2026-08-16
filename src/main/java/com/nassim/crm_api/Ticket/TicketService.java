package com.nassim.crm_api.Ticket;

import com.nassim.crm_api.Customer.Customer;
import com.nassim.crm_api.Customer.CustomerRepository;
import com.nassim.crm_api.Customer.CustomerResponse;
import com.nassim.crm_api.Employee.Employee;
import com.nassim.crm_api.Employee.EmployeeRepository;
import com.nassim.crm_api.Employee.EmployeeResponse;
import com.nassim.crm_api.exception.CustomerNotFoundException;
import com.nassim.crm_api.exception.EmployeeNotFoundException;
import com.nassim.crm_api.exception.InvalidTicketStatusException;
import com.nassim.crm_api.exception.TicketNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Service
public class TicketService {

    private static final Map<Status, Set<Status>> ALLOWED_TRANSITIONS = new EnumMap<>(Map.of(
            Status.OPEN, Set.of(Status.IN_PROGRESS),
            Status.IN_PROGRESS, Set.of(Status.RESOLVED, Status.CLOSED),
            Status.RESOLVED, Set.of(Status.CLOSED, Status.OPEN),
            Status.CLOSED, Set.of(Status.OPEN)
    ));

    private final TicketRepository ticketRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public TicketService(TicketRepository ticketRepository,
                         CustomerRepository customerRepository,
                         EmployeeRepository employeeRepository) {
        this.ticketRepository = ticketRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    public Page<TicketResponse> search(Status status, Priority priority, Long customerId, Long employeeId, Pageable pageable) {
        return ticketRepository.search(status, priority, customerId, employeeId, pageable)
                .map(this::toResponse);
    }

    public TicketResponse getTicketById(Long id) {
        return toResponse(getEntityById(id));
    }

    public TicketResponse createTicket(TicketRequest request) {
        Customer customer = getCustomer(request.customerId());
        Employee employee = getEmployee(request.assignedEmployeeId());

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setPriority(request.priority());
        ticket.setStatus(Status.OPEN);
        ticket.setCustomer(customer);
        ticket.setAssignedEmployee(employee);
        return toResponse(ticketRepository.save(ticket));
    }

    public TicketResponse updateTicket(Long id, TicketRequest request) {
        Ticket ticket = getEntityById(id);
        Customer customer = getCustomer(request.customerId());
        Employee employee = getEmployee(request.assignedEmployeeId());

        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setPriority(request.priority());
        ticket.setCustomer(customer);
        ticket.setAssignedEmployee(employee);
        return toResponse(ticketRepository.save(ticket));
    }

    public TicketResponse assignEmployee(Long id, Long employeeId) {
        Ticket ticket = getEntityById(id);
        ticket.setAssignedEmployee(getEmployee(employeeId));
        return toResponse(ticketRepository.save(ticket));
    }

    public TicketResponse changeStatus(Long id, Status target) {
        Ticket ticket = getEntityById(id);
        Set<Status> allowed = ALLOWED_TRANSITIONS.get(ticket.getStatus());
        if (allowed == null || !allowed.contains(target)) {
            throw new InvalidTicketStatusException(id, ticket.getStatus(), target);
        }
        ticket.setStatus(target);
        return toResponse(ticketRepository.save(ticket));
    }

    public void deleteTicket(Long id) {
        getEntityById(id);
        ticketRepository.deleteById(id);
    }

    private Ticket getEntityById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    private Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    private Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private TicketResponse toResponse(Ticket ticket) {
        CustomerResponse customer = new CustomerResponse(
                ticket.getCustomer().getId(),
                ticket.getCustomer().getFirstName(),
                ticket.getCustomer().getLastName(),
                ticket.getCustomer().getEmail()
        );
        EmployeeResponse employee = new EmployeeResponse(
                ticket.getAssignedEmployee().getId(),
                ticket.getAssignedEmployee().getName(),
                ticket.getAssignedEmployee().getEmail(),
                ticket.getAssignedEmployee().getRole()
        );
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                customer,
                employee,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
