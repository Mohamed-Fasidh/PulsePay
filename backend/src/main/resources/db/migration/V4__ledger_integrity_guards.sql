CREATE OR REPLACE FUNCTION verify_ledger_journal_balance() RETURNS trigger AS $$
DECLARE
    target_journal UUID := COALESCE(NEW.journal_id, OLD.journal_id);
    debit_total BIGINT;
    credit_total BIGINT;
BEGIN
    SELECT COALESCE(SUM(debit_minor), 0), COALESCE(SUM(credit_minor), 0)
      INTO debit_total, credit_total
      FROM ledger_entries WHERE journal_id = target_journal;
    IF debit_total <> credit_total THEN
        RAISE EXCEPTION 'Ledger journal % is unbalanced: debit %, credit %', target_journal, debit_total, credit_total;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER ledger_entries_must_balance
AFTER INSERT OR UPDATE OR DELETE ON ledger_entries
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION verify_ledger_journal_balance();

CREATE OR REPLACE FUNCTION reject_ledger_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Posted ledger records are immutable; use a reversal journal instead';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ledger_entries_immutable
BEFORE UPDATE OR DELETE ON ledger_entries
FOR EACH ROW EXECUTE FUNCTION reject_ledger_mutation();

CREATE TRIGGER ledger_journals_immutable
BEFORE UPDATE OR DELETE ON ledger_journals
FOR EACH ROW EXECUTE FUNCTION reject_ledger_mutation();
