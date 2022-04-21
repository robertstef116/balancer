
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

CREATE TABLE IF NOT EXISTS deployments
(
    id varchar(36) PRIMARY KEY,
    path varchar(50) NOT NULL ,
    image varchar(100) NOT NULL,
    memory_limit numeric,
    ports integer[]
)
