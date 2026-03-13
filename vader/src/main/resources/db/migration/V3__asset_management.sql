CREATE TABLE assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id), -- Soft delete approach: no ON DELETE CASCADE
    status VARCHAR(50) NOT NULL DEFAULT 'INIT',
    s3_key VARCHAR(512) NOT NULL UNIQUE,
    mime_type VARCHAR(100) NOT NULL,
    ref_count INTEGER NOT NULL DEFAULT 0,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE -- Soft delete marker
);

CREATE TABLE asset_references (
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL, -- e.g., 'DOCUMENT', 'PROFILE'
    entity_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (asset_id, entity_type, entity_id)
);

CREATE INDEX idx_assets_owner_id ON assets(owner_id);
CREATE INDEX idx_assets_status ON assets(status);
CREATE INDEX idx_asset_refs_entity ON asset_references(entity_type, entity_id);

