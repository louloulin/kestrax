-- 蓝图表结构
CREATE TABLE blueprints (
    id VARCHAR(36) PRIMARY KEY,
    namespace_id VARCHAR(36) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description CLOB,
    content CLOB NOT NULL,
    kind VARCHAR(50),
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    is_template BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- 蓝图标签表
CREATE TABLE blueprint_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blueprint_id VARCHAR(36) NOT NULL,
    tag VARCHAR(255) NOT NULL,
    FOREIGN KEY (blueprint_id) REFERENCES blueprints(id) ON DELETE CASCADE
);

-- 蓝图任务表
CREATE TABLE blueprint_included_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blueprint_id VARCHAR(36) NOT NULL,
    task VARCHAR(255) NOT NULL,
    FOREIGN KEY (blueprint_id) REFERENCES blueprints(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_blueprint_namespace ON blueprints(namespace_id);
CREATE INDEX idx_blueprint_created_by ON blueprints(created_by);
CREATE INDEX idx_blueprint_kind ON blueprints(kind);
CREATE INDEX idx_blueprint_tags_blueprint_id ON blueprint_tags(blueprint_id);
CREATE INDEX idx_blueprint_tasks_blueprint_id ON blueprint_included_tasks(blueprint_id);
