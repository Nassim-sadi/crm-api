package com.nassim.crm_api.exception;

import com.nassim.crm_api.Ticket.Status;

public class InvalidTicketStatusException extends RuntimeException {

    public InvalidTicketStatusException(Long ticketId, Status current, Status target) {
        super("Cannot change ticket " + ticketId + " from " + current + " to " + target);
    }
}
