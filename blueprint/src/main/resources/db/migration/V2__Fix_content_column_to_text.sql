-- 修复content和description字段类型为TEXT
-- 确保它们能存储大文本内容

-- 对于H2数据库，修改列类型为TEXT
ALTER TABLE blueprints ALTER COLUMN content TEXT;
ALTER TABLE blueprints ALTER COLUMN description TEXT;
