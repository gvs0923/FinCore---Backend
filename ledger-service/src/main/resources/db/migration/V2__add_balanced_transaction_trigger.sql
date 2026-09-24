-- Defense-in-depth: enforce debits == credits at the database level.
-- The application already checks this in LedgerService.assertBalanced(),
-- but a DB constraint trigger catches anything that bypasses the service
-- layer (manual SQL, future services, migration scripts).
--
-- This is a CONSTRAINT trigger with DEFERRABLE INITIALLY DEFERRED, which
-- means it fires at COMMIT time — after all entries for a transaction have
-- been inserted. A regular AFTER INSERT trigger would fire after each row,
-- which would fail because the first debit hasn't been paired with its
-- credit yet.

CREATE OR REPLACE FUNCTION assert_transaction_balanced()
RETURNS TRIGGER AS $$
DECLARE
    v_total_debits  NUMERIC(19, 4);
    v_total_credits NUMERIC(19, 4);
BEGIN
    SELECT COALESCE(SUM(amount), 0) INTO v_total_debits
    FROM ledger_entries
    WHERE transaction_id = NEW.transaction_id
      AND entry_type = 'DEBIT';

    SELECT COALESCE(SUM(amount), 0) INTO v_total_credits
    FROM ledger_entries
    WHERE transaction_id = NEW.transaction_id
      AND entry_type = 'CREDIT';

    IF v_total_debits != v_total_credits THEN
        RAISE EXCEPTION 'Unbalanced transaction %: debits=% credits=%',
            NEW.transaction_id, v_total_debits, v_total_credits;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_assert_transaction_balanced
    AFTER INSERT ON ledger_entries
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION assert_transaction_balanced();
