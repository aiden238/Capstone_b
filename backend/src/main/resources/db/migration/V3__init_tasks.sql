-- V3: Tasks & Task Assignees 테이블
-- 칸반 보드 태스크 관리

CREATE TABLE tasks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    status       VARCHAR(20) NOT NULL DEFAULT 'TODO',    -- TODO | IN_PROGRESS | DONE
    priority     VARCHAR(10) DEFAULT 'MEDIUM',           -- LOW | MEDIUM | HIGH | URGENT
    tag          VARCHAR(30),                             -- 기능 | 문서 | 디자인 | 버그 | 조사
    due_date     DATE,
    completed_at TIMESTAMPTZ,
    created_by   UUID NOT NULL REFERENCES users(id),
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    updated_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE task_assignees (
    task_id     UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id),
    assigned_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY(task_id, user_id)
);

CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_tasks_status ON tasks(project_id, status);
CREATE INDEX idx_task_assignees_user ON task_assignees(user_id);
