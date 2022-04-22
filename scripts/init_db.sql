
CREATE TABLE IF NOT EXISTS users
(
    username varchar(30) PRIMARY KEY,
    password varchar(20) NOT NULL
);

INSERT INTO users(username, password)
VALUES ('root', 'admin'),
       ('test', 'test')
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS workers
(
    id varchar(36) PRIMARY KEY,
    alias varchar(50) NOT NULL,
    ip varchar(15) NOT NULL,
    in_use boolean NOT NULL
);

INSERT INTO workers(id, alias, ip, in_use)
VALUES ('a1511050-b7b3-4d9c-b634-7988ad79ab4b', 'localhost', '127.0.0.1', true)
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS workflows
(
    id varchar(36) PRIMARY KEY,
    path varchar(50) NOT NULL ,
    image varchar(100) NOT NULL,
    memory_limit numeric,
    ports integer[]
);


