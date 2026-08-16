package com.nassim.crm_api.Ticket;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public Page<TicketResponse> search(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long customer,
            @RequestParam(required = false) Long assignedEmployee,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ticketService.search(status, priority, customer, assignedEmployee, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public TicketResponse findById(@PathVariable Long id) {
        return ticketService.getTicketById(id);
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketRequest request) {
        TicketResponse created = ticketService.createTicket(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody TicketRequest request) {
        return ticketService.updateTicket(id, request);
    }

    @PutMapping("/{id}/assign")
    public TicketResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        return ticketService.assignEmployee(id, request.employeeId());
    }

    @PostMapping("/{id}/start")
    public TicketResponse start(@PathVariable Long id) {
        return ticketService.changeStatus(id, Status.IN_PROGRESS);
    }

    @PostMapping("/{id}/resolve")
    public TicketResponse resolve(@PathVariable Long id) {
        return ticketService.changeStatus(id, Status.RESOLVED);
    }

    @PostMapping("/{id}/close")
    public TicketResponse close(@PathVariable Long id) {
        return ticketService.changeStatus(id, Status.CLOSED);
    }

    @PostMapping("/{id}/reopen")
    public TicketResponse reopen(@PathVariable Long id) {
        return ticketService.changeStatus(id, Status.OPEN);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}
