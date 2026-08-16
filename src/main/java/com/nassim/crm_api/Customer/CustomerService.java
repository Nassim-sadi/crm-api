package com.nassim.crm_api.Customer;

import com.nassim.crm_api.exception.CustomerNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CustomerResponse getCustomerById(Long id) {
        return toResponse(getEntityById(id));
    }

    public CustomerResponse createCustomer(CustomerRequest request) {
        Customer customer = new Customer(
                request.firstName(),
                request.lastName(),
                request.email()
        );
        return toResponse(customerRepository.save(customer));
    }

    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = getEntityById(id);
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        return toResponse(customerRepository.save(customer));
    }

    public void deleteCustomer(Long id) {
        getEntityById(id);
        customerRepository.deleteById(id);
    }

    private Customer getEntityById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail()
        );
    }
}
