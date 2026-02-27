-- ---------------------------------------------------------------------------
-- V1__init.sql
-- Initial database schema for Death Star
-- ---------------------------------------------------------------------------

CREATE TABLE todos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for optimizing queries ordered by creation time
CREATE INDEX idx_todos_created_at ON todos(created_at DESC);