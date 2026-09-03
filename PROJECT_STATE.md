# SettleFlow — Project State

## Phase

Phase 1 — Base Setup

## Application

- Java: 21
- Spring Boot: 4.1.1
- Build tool: Maven
- Application port: 8080
- Application name: settleflow

## Database

- Database: PostgreSQL
- PostgreSQL version: 17
- Database name: settleflow
- Database user: settleflow
- JPA Hibernate DDL mode: `validate`

Hibernate does not create or modify the schema. Liquibase owns schema migrations.

## Database Migration

Liquibase is enabled.

Master changelog:

`src/main/resources/db/changelog/db.changelog-master.yaml`

The schema is split into one changeset per logical change:

1. `001-create-settlements-table.yaml`
2. `002-create-transactions-table.yaml`
3. `003-create-ledger-entries-table.yaml`
4. `004-add-settlement-transaction-fk.yaml`
5. `005-add-transaction-ledger-fk.yaml`
6. `006-add-transaction-idempotency-unique.yaml`

All six changesets were successfully executed against a fresh PostgreSQL database.

## Schema

### settlements

- `id` — UUID primary key
- `merchant_id` — VARCHAR(100), NOT NULL
- `amount` — DECIMAL(19,4), NOT NULL
- `status` — VARCHAR(30), NOT NULL
- `created_at` — TIMESTAMP, NOT NULL

### transactions

- `id` — UUID primary key
- `settlement_id` — UUID, NOT NULL
- `merchant_id` — VARCHAR(100), NOT NULL
- `amount` — DECIMAL(19,4), NOT NULL
- `status` — VARCHAR(30), NOT NULL
- `idempotency_key` — VARCHAR(255), NOT NULL, UNIQUE
- `created_at` — TIMESTAMP, NOT NULL

Relationship:

`transactions.settlement_id → settlements.id`

### ledger_entries

- `id` — UUID primary key
- `transaction_id` — UUID, NOT NULL
- `amount` — DECIMAL(19,4), NOT NULL
- `entry_type` — VARCHAR(30), NOT NULL
- `created_at` — TIMESTAMP, NOT NULL

Relationship:

`ledger_entries.transaction_id → transactions.id`

## Java Domain Classes

Currently present:

- `Settlement`
- `Transaction`
- `LedgerEntry`

These classes currently exist as Java domain classes. JPA entity annotations/mappings are not part of this Phase 1 baseline.

## Health Check

Spring Boot Actuator is enabled.

Exposed endpoint:

`GET /actuator/health`

Verified response:

```json
{
  "groups": [
    "liveness",
    "readiness"
  ],
  "status": "UP"
}
