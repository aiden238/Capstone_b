-- V2: Projects & Project Members 테이블
-- 프로젝트 관리 + 멤버 역할 + 데이터 수집 동의

CREATE TABLE projects (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    course_name VARCHAR(255),
    semester    VARCHAR(20),
    start_date  DATE,
    end_date    DATE,
    invite_code VARCHAR(8) UNIQUE,
    created_by  UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE project_members (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id          UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES users(id),
    role                VARCHAR(20) NOT NULL DEFAULT 'MEMBER',  -- LEADER | MEMBER | OBSERVER
    joined_at           TIMESTAMPTZ DEFAULT NOW(),

    -- 데이터 수집 동의 기록
    consent_platform    BOOLEAN DEFAULT FALSE,
    consent_github      BOOLEAN DEFAULT FALSE,
    consent_drive       BOOLEAN DEFAULT FALSE,
    consent_ai_analysis BOOLEAN DEFAULT FALSE,
    consented_at        TIMESTAMPTZ,

    UNIQUE(project_id, user_id)
);

CREATE INDEX idx_projects_invite_code ON projects(invite_code);
CREATE INDEX idx_project_members_project ON project_members(project_id);
CREATE INDEX idx_project_members_user ON project_members(user_id);
