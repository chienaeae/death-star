CREATE TABLE IF NOT EXISTS task_field_definitions (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    bucket_type VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, name)
);

-- Seed default system fields for test tenant
INSERT INTO task_field_definitions (id, tenant_id, name, field_type, bucket_type)
VALUES
    ('00000000-0000-0000-0000-000000000000', 'system', 'Title', 'STRING', 'STATIC'),
    ('00000000-0000-0000-0000-000000000001', 'system', 'Status', 'STATUS', 'DYNAMIC'),
    ('00000000-0000-0000-0000-000000000002', 'system', 'LexRank', 'LEXRANK', 'DYNAMIC'),
    ('00000000-0000-0000-0000-000000000003', 'system', 'Description', 'STRING', 'STATIC'),
    ('00000000-0000-0000-0000-000000000004', 'system', 'Priority', 'STRING', 'STATIC'),
    ('00000000-0000-0000-0000-000000000005', 'system', 'DueDate', 'STRING', 'STATIC')
ON CONFLICT DO NOTHING;
