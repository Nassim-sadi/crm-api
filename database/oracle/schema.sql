-- =====================================================================
-- CRM API - Oracle schema (demonstration / learning artifact)
-- ---------------------------------------------------------------------
-- Mirrors the JPA domain model used by the Spring Boot application:
--   Customer, Employee (roles + password hash), Ticket, TicketComment.
-- Requires Oracle 12c+ (IDENTITY columns). Run from SQLcl or SQL*Plus:
--   @schema.sql
--
-- Column names are unquoted and <= 30 characters (Oracle limits).
-- =====================================================================

-- ---------------------------------------------------------------------
-- CUSTOMER
-- ---------------------------------------------------------------------
CREATE TABLE customer (
    id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR2(100) NOT NULL,
    last_name  VARCHAR2(100) NOT NULL,
    email      VARCHAR2(150) NOT NULL CONSTRAINT uq_customer_email UNIQUE
);

-- ---------------------------------------------------------------------
-- EMPLOYEE
-- ---------------------------------------------------------------------
CREATE TABLE employee (
    id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR2(100) NOT NULL,
    email         VARCHAR2(150) NOT NULL CONSTRAINT uq_employee_email UNIQUE,
    role          VARCHAR2(20)  NOT NULL
                  CONSTRAINT ck_employee_role CHECK (role IN ('ADMIN','MANAGER','AGENT','SUPPORT')),
    password_hash VARCHAR2(100)
);

-- ---------------------------------------------------------------------
-- TICKET
-- ---------------------------------------------------------------------
CREATE TABLE ticket (
    id                   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title                VARCHAR2(255) NOT NULL,
    description          VARCHAR2(2000),
    status               VARCHAR2(20)  NOT NULL
                         CONSTRAINT ck_ticket_status CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED')),
    priority             VARCHAR2(20)  NOT NULL
                         CONSTRAINT ck_ticket_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    customer_id          NUMBER NOT NULL,
    assigned_employee_id NUMBER NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ticket_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT fk_ticket_employee FOREIGN KEY (assigned_employee_id) REFERENCES employee (id)
);

-- ---------------------------------------------------------------------
-- TICKET_COMMENT
-- ---------------------------------------------------------------------
CREATE TABLE ticket_comment (
    id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id  NUMBER NOT NULL,
    text       VARCHAR2(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_comment_ticket FOREIGN KEY (ticket_id)
        REFERENCES ticket (id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Indexes on frequently filtered / joined columns
-- ---------------------------------------------------------------------
CREATE INDEX idx_ticket_status   ON ticket (status);
CREATE INDEX idx_ticket_priority ON ticket (priority);
CREATE INDEX idx_ticket_customer ON ticket (customer_id);
CREATE INDEX idx_ticket_employee ON ticket (assigned_employee_id);
CREATE INDEX idx_comment_ticket  ON ticket_comment (ticket_id);
