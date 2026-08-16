package com.nassim.crm_api.Customer;

import com.nassim.crm_api.config.TestSecurityConfig;
import com.nassim.crm_api.exception.CustomerNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResultAssert;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@WebMvcTest(CustomerController.class)
@Import(TestSecurityConfig.class)
class CustomerControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private CustomerService customerService;

    private static final String VALID_BODY = """
            {"firstName": "Ada", "lastName": "Lovelace", "email": "ada@example.com"}
            """;

    private CustomerResponse response() {
        return new CustomerResponse(1L, "Ada", "Lovelace", "ada@example.com");
    }

    @Test
    void findAll_returnsCustomers() {
        when(customerService.getAllCustomers()).thenReturn(List.of(response()));

        MvcTestResultAssert result = mockMvc.get().uri("/api/customers").exchange().assertThat();

        result.hasStatusOk();
        result.bodyJson().extractingPath("$[0].firstName").asString().isEqualTo("Ada");
        result.bodyJson().extractingPath("$[0].email").asString().isEqualTo("ada@example.com");
    }

    @Test
    void findById_returnsCustomer() {
        when(customerService.getCustomerById(1L)).thenReturn(response());

        MvcTestResultAssert result = mockMvc.get().uri("/api/customers/1").exchange().assertThat();

        result.hasStatusOk();
        result.bodyJson().extractingPath("$.email").asString().isEqualTo("ada@example.com");
    }

    @Test
    void findById_returns404WhenNotFound() {
        when(customerService.getCustomerById(99L)).thenThrow(new CustomerNotFoundException(99L));

        MvcTestResultAssert result = mockMvc.get().uri("/api/customers/99").exchange().assertThat();

        result.hasStatus(404);
        result.bodyJson().extractingPath("$.status").asNumber().isEqualTo(404);
        result.bodyJson().extractingPath("$.message").asString().isEqualTo("Customer not found with id: 99");
    }

    @Test
    void create_returns201WithLocation() {
        when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(new CustomerResponse(5L, "Ada", "Lovelace", "ada@example.com"));

        MvcTestResultAssert result = mockMvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY)
                .exchange().assertThat();

        result.hasStatus(201);
        result.headers().hasHeaderSatisfying("Location",
                values -> assertThat(values).anyMatch(v -> v.endsWith("/api/customers/5")));
        result.bodyJson().extractingPath("$.id").asNumber().isEqualTo(5);
    }

    @Test
    void create_returns400WithErrorsWhenInvalid() {
        String invalid = """
                {"firstName": "", "lastName": "", "email": "not-an-email"}
                """;

        MvcTestResultAssert result = mockMvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid)
                .exchange().assertThat();

        result.hasStatus(400);
        result.bodyJson().extractingPath("$.status").asNumber().isEqualTo(400);
        result.bodyJson().extractingPath("$.message").asString().isEqualTo("Invalid request");
        result.bodyJson().extractingPath("$.errors.firstName").asString().isEqualTo("firstName is required");
        result.bodyJson().extractingPath("$.errors.email").asString().isEqualTo("email must be a valid email address");
    }

    @Test
    void update_returnsUpdatedCustomer() {
        when(customerService.updateCustomer(eq(1L), any(CustomerRequest.class))).thenReturn(new CustomerResponse(1L, "Grace", "Hopper", "grace@example.com"));

        MvcTestResultAssert result = mockMvc.put().uri("/api/customers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY)
                .exchange().assertThat();

        result.hasStatusOk();
        result.bodyJson().extractingPath("$.firstName").asString().isEqualTo("Grace");
    }

    @Test
    void delete_returns204() {
        doNothing().when(customerService).deleteCustomer(1L);

        MvcTestResultAssert result = mockMvc.delete().uri("/api/customers/1").exchange().assertThat();

        result.hasStatus(204);
    }
}
