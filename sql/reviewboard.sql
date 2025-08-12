CREATE DATABASE reviewboard;
\c reviewboard;


CREATE TABLE IF NOT EXISTS companies (
    id BIGSERIAL PRIMARY KEY,
    name TEXT UNIQUE NOT NULL,
    slug TEXT UNIQUE NOT NULL,
    url TEXT UNIQUE NOT NULL,
    location TEXT,
    country TEXT,
    industry TEXT,
    image TEXT,
    tags TEXT[]
);


CREATE TABLE IF NOT EXISTS reviews (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    management INT NOT NULL,
    culture INT NOT NULL,
    salary INT NOT NULL,
    benefits INT NOT NULL,
    would_recommend INT NOT NULL,
    review TEXT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT now(),
    updated TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS review_summaries (
    company_id BIGINT NOT NULL PRIMARY KEY,
    content TEXT,
    created TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    hashed_password TEXT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT now(),
    updated TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS otps (
    id BIGINT NOT NULL,
    email TEXT PRIMARY KEY,
    token TEXT NOT NULL,
    expires BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS invites (
    id BIGSERIAL PRIMARY KEY,
    user_name TEXT NOT NULL,
    company_id BIGINT NOT NULL,
    n_invites INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT false
);

-- dummy data
insert into companies (
    id,
    name,
    slug,
    url,
    location,
    country,
    industry
    ) values (
    1,
    'rockthejvm',
    'rockthejvm',
    'https://rockthejvm.com',
    'Bucharest',
    'Romania',
    'Teaching'
    );

insert into users (id, email, hashed_password) values (1, 'marc@flatmappers.com', '123');

insert into invites (id, user_name, company_id, n_invites, active) values (1, 'daniel@rockthejvm.com', 1, 10, true);
