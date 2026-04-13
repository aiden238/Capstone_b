-- V9: Google Drive Integration
-- Google Drive 연동을 위한 테이블 및 users 테이블 컬럼 추가

-- 사용자-Google 계정 연동 (revision author → userId 매핑)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS google_email VARCHAR(255) UNIQUE;

CREATE INDEX idx_users_google_email ON users(google_email);

-- 프로젝트-Google Drive 폴더 연동
CREATE TABLE project_drive_integrations (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id        UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    drive_folder_id   VARCHAR(255) NOT NULL,
    -- Google Drive Push Notification (Changes: watch) 채널 정보
    watch_channel_id  VARCHAR(255),
    watch_resource_id VARCHAR(255),
    watch_expiry      TIMESTAMPTZ,
    -- 암호화 저장 (AES-256 권장, 현재는 Plain — 프로덕션 배포 전 반드시 암호화)
    access_token      TEXT,
    refresh_token     TEXT,
    token_expiry      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ DEFAULT NOW(),
    updated_at        TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE(project_id)                     -- 프로젝트당 1개 Drive 연동
);

CREATE INDEX idx_drive_integrations_project ON project_drive_integrations(project_id);
CREATE INDEX idx_drive_integrations_channel ON project_drive_integrations(watch_channel_id);
