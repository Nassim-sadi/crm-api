package com.nassim.crm_api.Ticket;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("""
            SELECT t FROM Ticket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:customerId IS NULL OR t.customer.id = :customerId)
              AND (:employeeId IS NULL OR t.assignedEmployee.id = :employeeId)
            """)
    Page<Ticket> search(@Param("status") Status status,
                        @Param("priority") Priority priority,
                        @Param("customerId") Long customerId,
                        @Param("employeeId") Long employeeId,
                        Pageable pageable);
}
