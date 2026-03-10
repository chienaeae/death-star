-- ---------------------------------------------------------------------------
-- Death Star V2 Migration - Identity and Access Management (IAM)
-- ---------------------------------------------------------------------------

-- 1. Core Users Table (Identity decoupled from credentials)
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. User Identities Table (Supports Local, Google, Azure AD, etc.)
CREATE TABLE user_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL, -- e.g., 'LOCAL', 'GOOGLE'
    provider_id VARCHAR(255) NOT NULL, -- Hashed password for 'LOCAL', or OIDC 'sub' for others
    metadata JSONB, -- For storing extra OIDC claims or tokens if needed
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_provider_provider_id UNIQUE (provider, provider_id)
);

-- Index for fast authentication lookups
CREATE INDEX idx_user_identities_lookup ON user_identities(provider, provider_id);

-- 3. Refresh Tokens Table (For Refresh Token Rotation and Revocation)
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id UUID NOT NULL, -- Ties rotated tokens together to detect replay attacks
    token_hash VARCHAR(255) UNIQUE NOT NULL, -- Storing hash, not the plain token, to mitigate DB leaks
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast token validation and cleanup
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 4. User Profiles Table (1-to-1 with Users, separated for clean domain boundary)
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(100),
    bio VARCHAR(500),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);