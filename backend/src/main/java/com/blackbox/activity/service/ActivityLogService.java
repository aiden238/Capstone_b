package com.blackbox.activity.service;

import com.blackbox.activity.entity.ActionType;
import com.blackbox.activity.entity.ActivityLog;
import com.blackbox.activity.entity.ActivitySource;
import com.blackbox.activity.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID projectId, UUID userId, ActionType actionType, String metadata) {
        ActivityLog log = new ActivityLog(projectId, userId, ActivitySource.PLATFORM, actionType, metadata);
        activityLogRepository.save(log);
    }
}
