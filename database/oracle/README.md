# Oracle / PL-SQL Demo

Learning artifacts that mirror the CRM domain in Oracle syntax. They are not used by the Spring Boot application (which runs on H2); they exist to demonstrate Oracle DDL and PL/SQL programming constructs.

## Prerequisites

- An Oracle database (12c or newer, for `IDENTITY` columns)
- A client: [SQLcl](https://www.oracle.com/database/sqldeveloper/technologies/sqlcl/) (recommended) or SQL\*Plus

## Files

| File | Purpose |
| --- | --- |
| `schema.sql` | Oracle DDL mirroring the JPA entities: `CUSTOMER`, `EMPLOYEE`, `TICKET`, `TICKET_COMMENT` |
| `procedures.sql` | PL/SQL package `ticket_ops` (spec + body) and a demo block |

## Run it

```sql
-- connect to your schema, e.g. with SQLcl:
--   sql crm/crm@//localhost:1521/FREEPDB1

SET SERVEROUTPUT ON

@schema.sql

INSERT INTO customer (first_name, last_name, email)
VALUES ('Ada', 'Lovelace', 'ada@example.com');

INSERT INTO employee (name, email, role, password_hash)
VALUES ('Grace Hopper', 'grace@crm.com', 'AGENT', NULL);

INSERT INTO ticket (title, description, status, priority, customer_id, assigned_employee_id, created_at, updated_at)
VALUES ('Printer broken', 'Paper jam in bay 3', 'OPEN', 'HIGH', 1, 1, SYSTIMESTAMP, SYSTIMESTAMP);

@procedures.sql
```

## What each part demonstrates

| Construct | Where |
| --- | --- |
| DDL: `IDENTITY` PK, `CHECK` constraints, unique constraints, FKs, indexes | `schema.sql` |
| Stored function (`RETURN NUMBER`) with `SELECT ... INTO` | `ticket_ops.count_open_tickets` |
| Stored function handling `WHEN NO_DATA_FOUND` | `ticket_ops.get_ticket_title` |
| Stored procedure with a `FOR UPDATE` cursor loop | `ticket_ops.reassign_tickets` |
| Custom exception + `PRAGMA EXCEPTION_INIT` + `RAISE` | `ticket_ops.e_no_active_tickets` |
| `RAISE_APPLICATION_ERROR` re-raise in the exception section | `ticket_ops.reassign_tickets` |
| Explicit cursor loop + `DBMS_OUTPUT` report | `ticket_ops.list_tickets` |
| Package spec vs body, named (`=>`) argument passing | `procedures.sql` |

## Expected output

```text
Open HIGH tickets: 1
Title of ticket 1: Printer broken
Title of ticket 999: Unknown ticket
#1 [HIGH] Printer broken (customer=ada@example.com, agent=grace@crm.com)
Reassigned 1 active ticket(s) to employee 2
```

> Note: PL/SQL development can also be done browser-free with `SQLcl`'s `/` terminator for anonymous blocks, as shown in `procedures.sql`.
