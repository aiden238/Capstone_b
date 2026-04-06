package com.blackbox.activity.entity;

public enum ActionType {
    // Task
    TASK_CREATE,
    TASK_UPDATE,
    TASK_STATUS_CHANGE,
    TASK_COMPLETE,
    TASK_DELETE,
    TASK_ASSIGN,
    TASK_UNASSIGN,

    // Meeting
    MEETING_CREATE,
    MEETING_UPDATE,
    MEETING_DELETE,
    MEETING_CHECKIN,

    // File
    FILE_UPLOAD,

    // GitHub (확장 1)
    COMMIT,
    PR_OPEN,
    PR_MERGE,
    ISSUE_CREATE,
    ISSUE_CLOSE,
    CODE_REVIEW,

    // Google Drive (확장 1)
    DOC_EDIT,
    DOC_CREATE,
    DOC_COMMENT
}
