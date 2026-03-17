CREATE TABLE IF NOT EXISTS boards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS board_columns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    order_index INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_boards_tenant ON boards(tenant_id);
CREATE INDEX idx_board_columns_board ON board_columns(board_id);

CREATE TABLE IF NOT EXISTS items (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    attr_static JSONB DEFAULT '{}'::jsonb,
    attr_dynamic JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_items_tenant ON items(tenant_id);
