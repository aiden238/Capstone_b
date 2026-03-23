-- V7: Contribution Scores, Alerts, Weight Configs, Weight Change Log
-- Score Engine + 경보 시스템 + 교수 가중치 설정

CREATE TABLE contribution_scores (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id     UUID NOT NULL REFERENCES projects(id),
    user_id        UUID NOT NULL REFERENCES users(id),

    -- 항목별 점수 (0~150, 팀 평균=100 기준)
    git_score      DECIMAL(5,2) DEFAULT 0,
    doc_score      DECIMAL(5,2) DEFAULT 0,
    meeting_score  DECIMAL(5,2) DEFAULT 0,
    task_score     DECIMAL(5,2) DEFAULT 0,

    -- 종합 점수
    total_score    DECIMAL(5,2) DEFAULT 0,

    -- 적용된 가중치 스냅샷
    weight_git     DECIMAL(3,2) DEFAULT 0.30,
    weight_doc     DECIMAL(3,2) DEFAULT 0.25,
    weight_meeting DECIMAL(3,2) DEFAULT 0.20,
    weight_task    DECIMAL(3,2) DEFAULT 0.25,

    calculated_at  TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE(project_id, user_id)
);

CREATE TABLE alerts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES projects(id),
    user_id     UUID REFERENCES users(id),     -- NULL이면 팀 전체 경보
    alert_type  VARCHAR(30) NOT NULL,           -- CRUNCH_TIME | FREE_RIDE | DROPOUT | OVERLOAD | TAMPER | GAMING_SUSPECT
    severity    VARCHAR(10) NOT NULL,           -- LOW | MEDIUM | HIGH | CRITICAL
    message     TEXT NOT NULL,
    is_read     BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE weight_configs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id     UUID NOT NULL REFERENCES projects(id),
    professor_id   UUID NOT NULL REFERENCES users(id),
    weight_git     DECIMAL(3,2) DEFAULT 0.30,
    weight_doc     DECIMAL(3,2) DEFAULT 0.25,
    weight_meeting DECIMAL(3,2) DEFAULT 0.20,
    weight_task    DECIMAL(3,2) DEFAULT 0.25,
    updated_at     TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT weights_sum CHECK (
        weight_git + weight_doc + weight_meeting + weight_task = 1.00
    )
);

CREATE TABLE weight_change_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES projects(id),
    changed_by  UUID NOT NULL REFERENCES users(id),
    old_weights JSONB NOT NULL,
    new_weights JSONB NOT NULL,
    changed_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_scores_project ON contribution_scores(project_id);
CREATE INDEX idx_alerts_project ON alerts(project_id);
CREATE INDEX idx_alerts_unread ON alerts(project_id, is_read) WHERE is_read = FALSE;
