CREATE TABLE revoked_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(1000) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL
);

CREATE INDEX idx_revoked_tokens_expiry_date ON revoked_tokens(expiry_date);
