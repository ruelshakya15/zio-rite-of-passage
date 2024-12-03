CREATE TABLE IF NOT EXISTS companies(
    id BIGSERIAL PRIMARY KEY,
    slug TEXT UNIQUE NOT NULL,
    name TEXT UNIQUE NOT NULL,
    url TEXT UNIQUE NOT NULL,
    location TEXT,
    country TEXT,
    industry TEXT,
    image TEXT,
    tags TEXT []
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
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    hashed_password TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS recovery_tokens (
    email TEXT PRIMARY KEY,
    token text NOT NULL,
    expiration BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS invites (
    id BIGSERIAL PRIMARY KEY,
    user_name TEXT NOT NULL,
    company_id BIGINT NOT NULL,
    n_invites INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS review_summaries (
    company_id BIGINT NOT NULL,
    contents TEXT,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- dummy data
insert into companies(
    id,
    slug,
    name,
    url,
    location,
    country,
    industry
) values (
    1,
    'rock-the-jvm',
    'Rock the JVM',
    'https://rockthejvm.com',
    'Bucharest',
    'Romania',
    'Teaching'
);

insert into invites(id, user_name, company_id, n_invites, active) values (1, 'daniel@rockthejvm.com', 1, 10, true);