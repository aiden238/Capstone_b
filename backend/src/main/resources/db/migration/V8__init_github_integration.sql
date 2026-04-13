-- V8: GitHub Integration
-- GitHub App 연동을 위한 테이블 및 users 테이블 컬럼 추가

-- 사용자-GitHub 계정 연동 (커밋 author → userId 매핑)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS github_username VARCHAR(100) UNIQUE;

CREATE INDEX idx_users_github_username ON users(github_username);

-- 프로젝트-GitHub 저장소 연동
CREATE TABLE project_github_integrations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    installation_id BIGINT NOT NULL,
    repo_full_name  VARCHAR(255) NOT NULL,               -- "owner/repo"
    repo_id         BIGINT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE(project_id)                                   -- 프로젝트당 1개 저장소
);

CREATE INDEX idx_github_integrations_project ON project_github_integrations(project_id);
CREATE INDEX idx_github_integrations_installation ON project_github_integrations(installation_id);
