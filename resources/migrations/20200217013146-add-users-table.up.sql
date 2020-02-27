CREATE TABLE users
(
    id serial PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    email VARCHAR(32) UNIQUE NOT NULL,
    username VARCHAR(32) UNIQUE NOT NULL,
    password VARCHAR(128) NOT NULL
);