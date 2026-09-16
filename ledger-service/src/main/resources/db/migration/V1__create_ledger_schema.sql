-- Double-entry ledger schema.
-- Financial history is append-only: rows in ledger_transactions and
-- ledger_entries are never updated or deleted. Corrections are made with
-- new, reversing transactions.

CREATE TABLE accounts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_ref   VARCHAR(255) NOT NULL UNIQUE,       -- e.g. CUSTOMER_WALLET:C987
    account_type  VARCHAR(32)  NOT NULL,               -- ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    currency      VARCHAR(3)   NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE ledger_transactions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference     VARCHAR(255) NOT NULL UNIQUE,        -- e.g. payment id / idempotency key
    description   VARCHAR(1000),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE ledger_entries (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id   UUID NOT NULL REFERENCES ledger_transactions(id),
    account_id       UUID NOT NULL REFERENCES accounts(id),
    entry_type       VARCHAR(6) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount           NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency         VARCHAR(3) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ledger_entries_transaction_id ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_account_id ON ledger_entries(account_id);

-- Read-optimized balance projection, rebuilt from ledger_entries.
-- Never written to directly by application code except by the ledger
-- posting path, in the same DB transaction as the journal entries.
CREATE TABLE account_balances (
    account_id    UUID PRIMARY KEY REFERENCES accounts(id),
    balance       NUMERIC(19, 4) NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    version       BIGINT NOT NULL DEFAULT 0
);
