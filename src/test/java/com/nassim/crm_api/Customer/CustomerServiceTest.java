package com.nassim.crm_api.Customer;

import com.nassim.crm_api.exception.CustomerNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void getAllCustomers_returnsAll() {
        when(customerRepository.findAll()).thenReturn(List.of(new Customer("Ada", "Lovelace", "ada@example.com")));

        List<CustomerResponse> customers = customerService.getAllCustomers();

        assertThat(customers).hasSize(1);
        assertThat(customers.get(0).email()).isEqualTo("ada@example.com");
    }

    @Test
    void getCustomerById_returnsCustomerWhenFound() {
        Customer customer = new Customer("Ada", "Lovelace", "ada@example.com");
        customer.setId(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        CustomerResponse result = customerService.getCustomerById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.firstName()).isEqualTo("Ada");
    }

    @Test
    void getCustomerById_throwsWhenNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(99L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createCustomer_mapsRequestAndSaves() {
        CustomerRequest request = new CustomerRequest("Grace", "Hopper", "grace@example.com");
        Customer saved = new Customer("Grace", "Hopper", "grace@example.com");
        saved.setId(7L);
        when(customerRepository.save(any(Customer.class))).thenReturn(saved);

        CustomerResponse result = customerService.createCustomer(request);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getFirstName()).isEqualTo("Grace");
        assertThat(captor.getValue().getLastName()).isEqualTo("Hopper");
        assertThat(captor.getValue().getEmail()).isEqualTo("grace@example.com");
        assertThat(result.id()).isEqualTo(7L);
    }

    @Test
    void updateCustomer_updatesFieldsAndSaves() {
        Customer existing = new Customer("Old", "Name", "old@example.com");
        existing.setId(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenReturn(existing);

        CustomerRequest request = new CustomerRequest("New", "Name", "new@example.com");
        CustomerResponse result = customerService.updateCustomer(1L, request);

        assertThat(result.firstName()).isEqualTo("New");
        assertThat(result.lastName()).isEqualTo("Name");
        assertThat(result.email()).isEqualTo("new@example.com");
    }

    @Test
    void updateCustomer_throwsWhenNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateCustomer(99L, new CustomerRequest("A", "B", "a@b.com")))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void deleteCustomer_deletesWhenFound() {
        Customer existing = new Customer("Ada", "Lovelace", "ada@example.com");
        existing.setId(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));

        customerService.deleteCustomer(1L);

        verify(customerRepository).deleteById(1L);
    }

    @Test
    void deleteCustomer_throwsWhenNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.deleteCustomer(99L))
                .isInstanceOf(CustomerNotFoundException.class);
        verify(customerRepository, never()).deleteById(99L);
    }
}
