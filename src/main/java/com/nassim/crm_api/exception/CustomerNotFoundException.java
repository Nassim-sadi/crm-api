package com.nassim.crm_api.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(Long id) {
        super("Customer not found with id: " + id);
    }
}
