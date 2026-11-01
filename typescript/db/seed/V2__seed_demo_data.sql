-- Demo data so the two endpoints can be tried straight away. Lives in db/seed, which only the running app
-- applies (see SqlMigrator); tests start from an empty schema and insert their own data.
INSERT INTO time_deposits (id, plan_type, days, balance) VALUES
    (1, 'basic',   45,  1000.00),
    (2, 'student', 100, 2000.00),
    (3, 'premium', 100, 5000.00),
    (4, 'basic',   20,  1500.00),
    (5, 'student', 400, 3000.00),
    (6, 'premium', 40,  8000.00);

INSERT INTO withdrawals (id, time_deposit_id, amount, date) VALUES
    (1, 1, 100.00, '2024-01-15'),
    (2, 1,  50.00, '2024-02-20'),
    (3, 3, 250.00, '2024-03-05');
