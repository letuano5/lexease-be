-- Usage:
--   psql "$DB_URL" -v email='admin@example.com' -f scripts/promote-admin.sql
--
-- The user must already exist via public registration.
update users
set role = 'ADMIN',
    updated_at = now()
where lower(email) = lower(:'email');
