-- \c balancer_db;

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
    id     varchar(36) PRIMARY KEY,
    alias  varchar(50) NOT NULL,
    host   varchar(50) NOT NULL,
    port   integer     NOT NULL,
    in_use boolean     NOT NULL
);

CREATE TABLE IF NOT EXISTS workflows
(
    id              varchar(36) PRIMARY KEY,
    image           varchar(100) NOT NULL,
    memory_limit    numeric,
    algorithm       varchar(20)  NOT NULL,
    min_deployments numeric,
    max_deployments numeric
);

CREATE TABLE IF NOT EXISTS workflow_mappings
(
    path        varchar(50) PRIMARY KEY,
    workflow_id varchar(36),
    port        integer,

    FOREIGN KEY (workflow_id)
        REFERENCES workflows (id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS deployments
(
    id           varchar(36) PRIMARY KEY,
    worker_id    varchar(36) NOT NULL,
    workflow_id  varchar(36) NOT NULL,
    container_id varchar(64) NOT NULL,
    timestamp    numeric     NOT NULL,

    FOREIGN KEY (worker_id)
        REFERENCES workers (id)
        ON DELETE NO ACTION,
    FOREIGN KEY (workflow_id)
        REFERENCES workflows (id)
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS deployment_mappings
(
    deployment_id   varchar(36) NOT NULL,
    deployment_port integer     NOT NULL,
    worker_port     integer     NOT NULL,

    CONSTRAINT PK_DM PRIMARY KEY (deployment_id, deployment_port),
    FOREIGN KEY (deployment_id)
        REFERENCES deployments (id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS config
(
    key   varchar(50) PRIMARY KEY,
    value varchar(50) NOT NULL
);

INSERT INTO config(key, value)
VALUES ('PROCESSING_SOCKET_BUFFER_LENGTH', 2048),
       ('COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL',
        10),                                      -- recompute weight every 10 requests, used by weighted response time algorithm
       ('HEALTH_CHECK_TIMEOUT', 10000),
       ('HEALTH_CHECK_INTERVAL', 10000),
       ('HEALTH_CHECK_MAX_FAILURES', 3),
       ('CPU_WEIGHT', 0.3),                       -- no action on change, only variable change
       ('MEMORY_WEIGHT', 0.7),                    -- no action on change, only variable change
       ('DEPLOYMENTS_CHECK_INTERVAL', 60000),     -- no action on change, only variable change
       ('MASTER_CHANGES_CHECK_INTERVAL', 30000),  -- no action on change, only variable change
       ('NUMBER_RELEVANT_PERFORMANCE_METRICS', 3) -- might need health checks update
ON CONFLICT DO NOTHING;

-- ADD TEST DATA

INSERT INTO workers(id, alias, host, port, in_use)
VALUES ('7a0acdc5-9dd0-467a-9c92-9569d47eac1b', 'worker 1', '192.168.100.92', 8081, true),
       ('d0cb107a-78a5-4e1b-bde2-0318701ac329', 'worker 2', '192.168.100.93', 8081, true)
ON CONFLICT DO NOTHING;

INSERT INTO workflows(id, image, memory_limit, algorithm, min_deployments)
VALUES ('2307b2af-dc14-4737-b26b-3f68a9c5667g', 'nginx:latest', 1073741824, 'RANDOM', 3),
       ('e615b36a-cb04-11ec-9d64-0242ac120002', 'r0bb3rt17/test-image:latest', 1073741824, 'RANDOM', 3)
ON CONFLICT DO NOTHING;

INSERT INTO workflow_mappings(path, workflow_id, port)
VALUES ('/test-flow', '2307b2af-dc14-4737-b26b-3f68a9c5667g', 80),
       ('/test-flow2', '2307b2af-dc14-4737-b26b-3f68a9c5667g', 8080),
       ('/testx', 'e615b36a-cb04-11ec-9d64-0242ac120002', 8080)
ON CONFLICT DO NOTHING;

-- INSERT INTO deployments(id, worker_id, workflow_id, container_id, timestamp)
-- VALUES ('aaa143cc-b6f4-4dbc-b402-0fdc2415634c', 'a1511050-b7b3-4d9c-b634-7988ad79ab4b',
--         '2307b2af-dc14-4737-b26b-3f68a9c5667g', '33793ad97478', 1650612801919)
-- ON CONFLICT DO NOTHING;
--
-- INSERT INTO deployment_mappings(deployment_id, worker_port, deployment_port)
-- VALUES ('aaa143cc-b6f4-4dbc-b402-0fdc2415634c', 8444, 80),
--        ('aaa143cc-b6f4-4dbc-b402-0fdc2415634c', 8445, 8080)
-- ON CONFLICT DO NOTHING;
