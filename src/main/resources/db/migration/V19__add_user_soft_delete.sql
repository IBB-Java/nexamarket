ALTER TABLE user_accounts
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_user_accounts_status_role
    ON user_accounts (status, role);
