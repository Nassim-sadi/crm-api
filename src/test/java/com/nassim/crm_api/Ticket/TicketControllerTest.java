package com.nassim.crm_api.Ticket;

import com.nassim.crm_api.config.TestSecurityConfig;
import com.nassim.crm_api.Customer.CustomerResponse;
import com.nassim.crm_api.Employee.EmployeeResponse;
import com.nassim.crm_api.Employee.EmployeeRole;
import com.nassim.crm_api.exception.InvalidTicketStatusException;
import com.nassim.crm_api.exception.TicketNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResultAssert;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@WebMvcTest(TicketController.class)
@Import(TestSecurityConfig.class)
class TicketControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private TicketService ticketService;

    private static final String VALID_BODY = """
            {"title": "Printer broken", "description": "Paper jam", "priority": "HIGH", "customerId": 1, "assignedEmployeeId": 1}
            """;

    private TicketResponse ticketResponse() {
        CustomerResponse customer = new CustomerResponse(1L, "Ada", "Lovelace", "ada@example.com");
        EmployeeResponse employee = new EmployeeResponse(1L, "Grace Hopper", "grace@crm.com", EmployeeRole.AGENT);
        return new TicketResponse(
                10L,
                "Printer broken",
                "Paper jam",
                Status.OPEN,
                Priority.HIGH,
                customer,
                employee,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z")
        );
    }

    @Test
    void search_returnsPagedTicketsWithRelations() {
        when(ticketService.search(isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(ticketResponse()), PageRequest.of(0, 20), 1));

        MvcTestResultAssert result = mockMvc.get().uri("/api/tickets").exchange().assertThat();

        result.hasStatusOk();
        result.bodyJson().extractingPath("$.content[0].title").asString().isEqualTo("Printer broken");
        result.bodyJson().extractingPath("$.content[0].status").asString().isEqualTo("OPEN");
        result.bodyJson().extractingPath("$.content[0].customer.firstName").asString().isEqualTo("Ada");
        result.bodyJson().extractingPath("$.content[0].assignedEmployee.name").asString().isEqualTo("Grace Hopper");
    }

    @Test
    void search_filtersByStatus() {
        when(ticketService.search(eq(Status.OPEN), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(Page.empty());

        MvcTestResultAssert result = mockMvc.get().uri("/api/tickets?status=OPEN").exchange().assertThat();

        result.hasStatusOk();
    }

    @Test
    void search_rejectsInvalidStatusValue() {
        MvcTestResultAssert result = mockMvc.get().uri("/api/tickets?status=NONSENSE").exchange().assertThat();

        result.hasStatus(400);
        result.bodyJson().extractingPath("$.message").asString().contains("status");
    }

    @Test
    void findById_returnsTicket() {
        when(ticketService.getTicketById(10L)).thenReturn(ticketResponse());

        MvcTestResultAssert result = mockMvc.get().uri("/api/tickets/10").exchange().assertThat();

        result.hasStatusOk();
        result.bodyJson().extractingPath("$.title").asString().isEqualTo("Printer broken");
    }

    @Test
    void findById_returns404WhenNotFound() {
        when(ticketService.getTicketById(99L)).thenThrow(new TicketNotFoundException(99L));

        MvcTestResultAssert result = mockMvc.get().uri("/api/tickets/99").exchange().assertThat();

        result.hasStatus(404);
        result.bodyJson().extractingPath("$.message").asString().isEqualTo("Ticket not found with id: 99");
    }

    @Test
    void create_returns201WithLocation() {
        when(ticketService.createTicket(any(TicketRequest.class))).thenReturn(ticketResponse());

        MvcTestResultAssert result = mockMvc.post().uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY)
                .exchange().assertThat();

        result.hasStatus(201);
        result.headers().hasHeaderSatisfying("Location",
                values -> assertThat(values).anyMatch(v -> v.endsWith("/api/tickets/10")));
        result.bodyJson().extractingPath("$.id").asNumber().isEqualTo(10);
    }

    @Test
    void create_returns400WhenInvalid() {
        String invalid = """
                {"title": "", "priority": "HIGH", "customerId": 1, "assignedEmployeeId": 1}
                """;

        MvcTestResultAssert result = mockMvc.post().uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid)
                .exchange().assertThat();

        result.hasStatus(400);
        result.bodyJson().extractingPath("$.errors.title").asString().isEqualTo("title is required");
    }

    @Test
    void assign_returnsUpdatedTicket() {
        when(ticketService.assignEmployee(eq(10L), eq(2L))).thenReturn(ticketResponse());

        MvcTestResultAssert result = mockMvc.put().uri("/api/tickets/10/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\": 2}")
                .exchange().assertThat();

        result.hasStatusOk();
    }

    @Test
    void start_transitionsTicket() {
        when(ticketService.changeStatus(10L, Status.IN_PROGRESS)).thenReturn(ticketResponse());

        MvcTestResultAssert result = mockMvc.post().uri("/api/tickets/10/start").exchange().assertThat();

        result.hasStatusOk();
    }

    @Test
    void close_returns400OnInvalidTransition() {
        when(ticketService.changeStatus(10L, Status.CLOSED))
                .thenThrow(new InvalidTicketStatusException(10L, Status.OPEN, Status.CLOSED));

        MvcTestResultAssert result = mockMvc.post().uri("/api/tickets/10/close").exchange().assertThat();

        result.hasStatus(400);
        result.bodyJson().extractingPath("$.message").asString()
                .isEqualTo("Cannot change ticket 10 from OPEN to CLOSED");
    }

    @Test
    void update_returnsUpdatedTicket() {
        when(ticketService.updateTicket(eq(10L), any(TicketRequest.class))).thenReturn(ticketResponse());

        MvcTestResultAssert result = mockMvc.put().uri("/api/tickets/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY)
                .exchange().assertThat();

        result.hasStatusOk();
    }

    @Test
    void delete_returns204() {
        doNothing().when(ticketService).deleteTicket(10L);

        MvcTestResultAssert result = mockMvc.delete().uri("/api/tickets/10").exchange().assertThat();

        result.hasStatus(204);
    }
}
