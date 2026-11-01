-- Naming assumption: the README names the tables `timeDeposits` and `withdrawals`.
-- Postgres folds unquoted identifiers to lower case, so `timeDeposits` would silently become
-- `timedeposits`. Instead of quoting camelCase names everywhere, we use snake_case in the database
-- (time_deposits, plan_type, time_deposit_id) and keep the README's camelCase names in the API.
CREATE TABLE time_deposits (
    id        INTEGER PRIMARY KEY,
    plan_type VARCHAR(255)   NOT NULL,
    days      INTEGER        NOT NULL,
    balance   NUMERIC(19, 2) NOT NULL
);

CREATE TABLE withdrawals (
    id              INTEGER PRIMARY KEY,
    time_deposit_id INTEGER        NOT NULL REFERENCES time_deposits (id),
    amount          NUMERIC(19, 2) NOT NULL,
    date            DATE           NOT NULL
);

CREATE INDEX idx_withdrawals_time_deposit_id ON withdrawals (time_deposit_id);
