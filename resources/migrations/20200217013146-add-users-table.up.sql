CREATE TABLE users
(
    id serial PRIMARY KEY,
    name VARCHAR(32),
    email VARCHAR(32) NOT NULL,
    username VARCHAR(32),
    password VARCHAR(128) NOT NULL
);

--;;

CREATE UNIQUE INDEX username_idx ON users (username);