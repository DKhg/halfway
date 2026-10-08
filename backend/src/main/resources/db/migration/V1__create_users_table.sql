CREATE TABLE users (
    id              UUID PRIMARY KEY,
    token_hash      VARCHAR(64) NOT NULL,
    oauth_provider  VARCHAR(20),
    oauth_id        VARCHAR(255),
    last_nickname   VARCHAR(20),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX ux_users_token_hash ON users (token_hash);
CREATE UNIQUE INDEX ux_users_oauth_provider_id ON users (oauth_provider, oauth_id);
