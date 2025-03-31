
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email TEXT NOT NULL,
    hashed_password TEXT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT now(),
    updated TIMESTAMP NOT NULL DEFAULT now()
);
