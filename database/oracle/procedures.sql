-- =====================================================================
-- CRM API - PL/SQL demonstration package (learning artifact)
-- ---------------------------------------------------------------------
-- Demonstrates common PL/SQL constructs:
--   * a package (spec + body)
--   * stored procedures and functions
--   * explicit and implicit (FOR) cursor loops
--   * a custom exception with PRAGMA EXCEPTION_INIT
--   * RAISE / RAISE_APPLICATION_ERROR
--   * exception handlers (WHEN NO_DATA_FOUND, WHEN OTHERS)
--   * DBMS_OUTPUT for client-side output
--
-- Run after schema.sql from SQLcl or SQL*Plus (SERVEROUTPUT enabled):
--   SET SERVEROUTPUT ON
--   @procedures.sql
-- =====================================================================

CREATE OR REPLACE PACKAGE ticket_ops AS

    -- Custom exception bound to ORA-20001, raised when a reassignment
    -- finds no active tickets to move.
    e_no_active_tickets EXCEPTION;
    PRAGMA EXCEPTION_INIT (e_no_active_tickets, -20001);

    -- Returns the number of OPEN tickets for a given priority.
    FUNCTION count_open_tickets (p_priority IN VARCHAR2) RETURN NUMBER;

    -- Resolves the title of a ticket; returns 'Unknown ticket' if the
    -- id does not exist (WHEN NO_DATA_FOUND).
    FUNCTION get_ticket_title (p_ticket_id IN NUMBER) RETURN VARCHAR2;

    -- Moves every OPEN / IN_PROGRESS ticket of one employee to another.
    -- Raises e_no_active_tickets when there is nothing to reassign.
    PROCEDURE reassign_tickets (p_old_employee_id IN NUMBER,
                                p_new_employee_id IN NUMBER);

    -- Prints a formatted report of all tickets with the given status.
    PROCEDURE list_tickets (p_status IN VARCHAR2);

END ticket_ops;
/

CREATE OR REPLACE PACKAGE BODY ticket_ops AS

    FUNCTION count_open_tickets (p_priority IN VARCHAR2) RETURN NUMBER IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*)
          INTO v_count
          FROM ticket
         WHERE status   = 'OPEN'
           AND priority = p_priority;
        RETURN v_count;
    END count_open_tickets;

    FUNCTION get_ticket_title (p_ticket_id IN NUMBER) RETURN VARCHAR2 IS
        v_title VARCHAR2(255);
    BEGIN
        SELECT title
          INTO v_title
          FROM ticket
         WHERE id = p_ticket_id;
        RETURN v_title;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RETURN 'Unknown ticket';
    END get_ticket_title;

    PROCEDURE reassign_tickets (p_old_employee_id IN NUMBER,
                                p_new_employee_id IN NUMBER) IS
        CURSOR c_tickets IS
            SELECT id
              FROM ticket
             WHERE assigned_employee_id = p_old_employee_id
               AND status IN ('OPEN', 'IN_PROGRESS')
               FOR UPDATE;
    BEGIN
        FOR rec IN c_tickets LOOP
            UPDATE ticket
               SET assigned_employee_id = p_new_employee_id,
                   updated_at           = SYSTIMESTAMP
             WHERE id = rec.id;
        END LOOP;

        IF SQL%ROWCOUNT = 0 THEN
            RAISE e_no_active_tickets;
        END IF;

        DBMS_OUTPUT.PUT_LINE('Reassigned ' || SQL%ROWCOUNT
                             || ' active ticket(s) to employee '
                             || p_new_employee_id);
    EXCEPTION
        WHEN e_no_active_tickets THEN
            RAISE_APPLICATION_ERROR(-20001,
                'No active tickets to reassign from employee ' || p_old_employee_id);
    END reassign_tickets;

    PROCEDURE list_tickets (p_status IN VARCHAR2) IS
        CURSOR c_tickets IS
            SELECT t.id,
                   t.title,
                   t.priority,
                   c.email AS customer_email,
                   e.email AS assignee_email
              FROM ticket t
              JOIN customer c ON c.id = t.customer_id
              JOIN employee e ON e.id = t.assigned_employee_id
             WHERE t.status = p_status
             ORDER BY t.priority, t.created_at;
    BEGIN
        FOR rec IN c_tickets LOOP
            DBMS_OUTPUT.PUT_LINE(
                '#' || rec.id ||
                ' [' || rec.priority || '] ' || rec.title ||
                ' (customer=' || rec.customer_email ||
                ', agent=' || rec.assignee_email || ')');
        END LOOP;

        IF NOT c_tickets%FOUND THEN
            DBMS_OUTPUT.PUT_LINE('No tickets found with status ' || p_status);
        END IF;
    END list_tickets;

END ticket_ops;
/

-- =====================================================================
-- Demo block: exercises every member of the package.
-- Adjust the ids / priorities to match the data you inserted.
-- =====================================================================
BEGIN
    DBMS_OUTPUT.PUT_LINE('Open HIGH tickets: ' || ticket_ops.count_open_tickets('HIGH'));
    DBMS_OUTPUT.PUT_LINE('Title of ticket 1: ' || ticket_ops.get_ticket_title(1));
    DBMS_OUTPUT.PUT_LINE('Title of ticket 999: ' || ticket_ops.get_ticket_title(999));

    ticket_ops.list_tickets('OPEN');

    -- Move all open work of employee 1 to employee 2 (adjust as needed).
    ticket_ops.reassign_tickets(p_old_employee_id => 1, p_new_employee_id => 2);
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Demo error: ' || SQLERRM);
END;
/
