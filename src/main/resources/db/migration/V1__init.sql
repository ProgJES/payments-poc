-- V1__init.sql
-- Payment Service - initial schema

-- 1) idempotency: prevent duplicate processing for POST /payments
CREATE TABLE idempotency_keys (
                                  id BIGSERIAL PRIMARY KEY,
                                  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
                                  request_hash VARCHAR(64) NOT NULL,
                                  status VARCHAR(16) NOT NULL, -- IN_PROGRESS, SUCCEEDED, FAILED
                                  response_code INTEGER,
                                  response_body JSONB,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_idempotency_keys_created_at ON idempotency_keys (created_at);

-- 2) payments: current state of a payment (aggregate root)
CREATE TABLE payments (
                          id BIGSERIAL PRIMARY KEY,
                          payment_id VARCHAR(64) NOT NULL UNIQUE,          -- public id (UUID/ULID)
                          idempotency_key VARCHAR(128) NOT NULL,           -- ties to idempotency key
                          payment_method VARCHAR(32) NOT NULL,             -- CARD (future: BANK_TRANSFER)
                          amount BIGINT NOT NULL CHECK (amount > 0),
                          currency CHAR(3) NOT NULL,
                          status VARCHAR(32) NOT NULL,                     -- INIT, AUTHORIZED, HELD, SETTLED, FAILED, REVERSED
                          description VARCHAR(255),
                          created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                          CONSTRAINT fk_payments_idempotency_key
                              FOREIGN KEY (idempotency_key)
                                  REFERENCES idempotency_keys (idempotency_key)
);

CREATE INDEX ix_payments_status_created_at ON payments (status, created_at);

-- 3) payment_events: append-only audit trail for state transitions
CREATE TABLE payment_events (
                                id BIGSERIAL PRIMARY KEY,
                                payment_id VARCHAR(64) NOT NULL,
                                event_type VARCHAR(64) NOT NULL,                 -- e.g., PAYMENT_CREATED, AUTHORIZED, CAPTURED, FAILED
                                from_status VARCHAR(32),
                                to_status VARCHAR(32),
                                payload JSONB,                                   -- raw gateway response, debug info, etc.
                                created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                CONSTRAINT fk_payment_events_payment_id
                                    FOREIGN KEY (payment_id)
                                        REFERENCES payments (payment_id)
);

CREATE INDEX ix_payment_events_payment_id_created_at ON payment_events (payment_id, created_at);

-- 4) ledger_entries: minimal internal ledger (Phase 1, same DB transaction)
CREATE TABLE ledger_entries (
                                id BIGSERIAL PRIMARY KEY,
                                payment_id VARCHAR(64) NOT NULL,
                                entry_type VARCHAR(16) NOT NULL,                 -- DEBIT, CREDIT
                                account_key VARCHAR(128) NOT NULL,               -- e.g., merchant:<id> / customer:<id>
                                amount BIGINT NOT NULL CHECK (amount > 0),
                                currency CHAR(3) NOT NULL,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                CONSTRAINT fk_ledger_entries_payment_id
                                    FOREIGN KEY (payment_id)
                                        REFERENCES payments (payment_id)
);

CREATE INDEX ix_ledger_entries_account_key_created_at ON ledger_entries (account_key, created_at);
CREATE INDEX ix_ledger_entries_payment_id ON ledger_entries (payment_id);
