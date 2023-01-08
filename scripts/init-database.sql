
CREATE TABLE IF NOT EXISTS users
(
    username varchar(30) PRIMARY KEY,
    password varchar(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS config
(
    key   varchar(50) PRIMARY KEY,
    value varchar(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS workers
(
    id     varchar(36) PRIMARY KEY,
    alias  varchar(50) NOT NULL,
    host   varchar(50) NOT NULL UNIQUE,
    port   integer     NOT NULL,
    status varchar(8)  NOT NULL
);

CREATE TABLE IF NOT EXISTS workflows
(
    id              varchar(36) PRIMARY KEY,
    image           varchar(100) NOT NULL,
    memory_limit    numeric,
    algorithm       varchar(25)  NOT NULL,
    min_deployments numeric,
    max_deployments numeric,
    up_scaling      int,
    down_scaling    int
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

CREATE TABLE IF NOT EXISTS analytics
(
    worker_id     varchar(36) NOT NULL,
    workflow_id   varchar(36) NOT NULL,
    deployment_id varchar(36) NOT NULL,
    timestamp     numeric     NOT NULL
);

CREATE TABLE IF NOT EXISTS workflow_analytics
(
    worker_id     varchar(36) NOT NULL,
    workflow_id   varchar(36) NOT NULL,
    deployment_id varchar(36) NOT NULL,
    event         varchar(6)  NOT NULL,
    timestamp     numeric     NOT NULL
);

CREATE TABLE IF NOT EXISTS metadata
(
    key   varchar(36) PRIMARY KEY,
    value varchar(36) NOT NULL
);

-- DEFAULT CONFIGURATION
INSERT INTO config(key, value)
VALUES ('PROCESSING_SOCKET_BUFFER_LENGTH', 2048),
       ('COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL', 10),
       ('HEALTH_CHECK_TIMEOUT', 10000),
       ('HEALTH_CHECK_INTERVAL', 10000),
       ('HEALTH_CHECK_MAX_FAILURES', 3),
       ('CPU_WEIGHT', 0.3),
       ('MEMORY_WEIGHT', 0.7),
       ('DEPLOYMENTS_CHECK_INTERVAL', 60000),
       ('MASTER_CHANGES_CHECK_INTERVAL', 30000),
       ('NUMBER_RELEVANT_PERFORMANCE_METRICS', 3)
ON CONFLICT DO NOTHING;
