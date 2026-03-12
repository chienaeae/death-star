-- Add Avatar Asset Soft Link to User Profile
ALTER TABLE user_profiles
    ADD COLUMN avatar_asset_id UUID,
    ADD CONSTRAINT fk_user_profile_avatar
        FOREIGN KEY (avatar_asset_id)
        REFERENCES assets(id)
        ON DELETE SET NULL;
