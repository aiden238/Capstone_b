-- V8: External Integrations (GitHub App, Google Drive)
-- 확장 1: 외부 서비스 연동 테이블

-- 외부 서비스 연동 정보
CREATE TABLE external_integrations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    provider        VARCHAR(20) NOT NULL,  -- GITHUB_APP | GOOGLE_DRIVE
    external_id     VARCHAR(255) NOT NULL, -- repo full name (owner/repo) | Drive folder ID
    external_name   VARCHAR(255),          -- display name
    webhook_id      VARCHAR(255),          -- GitHub webhook ID | Drive push channel ID
    webhook_expiry  TIMESTAMPTZ,           -- Drive channel expiration
    installation_id BIGINT,                -- GitHub App installation ID
    last_synced     TIMESTAMPTZ,
    sync_status     VARCHAR(10) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | PAUSED | ERROR
    error_message   TEXT,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE(project_id, provider, external_id)
);

-- 외부 서비스 인증 정보 (사용자별)
CREATE TABLE external_auth (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider        VARCHAR(20) NOT NULL,  -- GITHUB_APP | GOOGLE
    access_token    TEXT,                  -- encrypted
    refresh_token   TEXT,                  -- encrypted
    installation_id BIGINT,               -- GitHub App installation ID
    scope           VARCHAR(500),
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE(user_id, provider)
);

-- GitHub 사용자 매핑 (GitHub username <-> 플랫폼 user_id)
CREATE TABLE github_user_mappings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    github_username VARCHAR(100) NOT NULL,
    github_id       BIGINT,
    mapped_at       TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE(project_id, user_id),
    UNIQUE(project_id, github_username)
);

CREATE INDEX idx_ext_integration_project ON external_integrations(project_id);
CREATE INDEX idx_ext_integration_provider ON external_integrations(project_id, provider);
CREATE INDEX idx_ext_auth_user ON external_auth(user_id);
CREATE INDEX idx_github_mapping_project ON github_user_mappings(project_id);
CREATE INDEX idx_activity_source ON activity_logs(project_id, source);
CREATE INDEX idx_activity_occurred ON activity_logs(project_id, occurred_at DESC);
