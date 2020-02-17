CREATE TABLE users
(
    id serial PRIMARY KEY,
    name VARCHAR(32),
    email VARCHAR(32) NOT NULL,
    password VARCHAR(128) NOT NULL
);