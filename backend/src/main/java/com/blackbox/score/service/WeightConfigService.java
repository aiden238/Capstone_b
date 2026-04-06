package com.blackbox.score.service;

import com.blackbox.auth.entity.User;
import com.blackbox.auth.repository.UserRepository;
import com.blackbox.common.exception.BusinessException;
import com.blackbox.common.exception.ErrorCode;
import com.blackbox.project.entity.Project;
import com.blackbox.project.repository.ProjectRepository;
import com.blackbox.score.dto.WeightConfigResponse;
import com.blackbox.score.dto.WeightUpdateRequest;
import com.blackbox.score.entity.WeightChangeLog;
import com.blackbox.score.entity.WeightConfig;
import com.blackbox.score.repository.WeightChangeLogRepository;
import com.blackbox.score.repository.WeightConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class WeightConfigService {

    private static final BigDecimal ONE = new BigDecimal("1.00");

    private final WeightConfigRepository weightConfigRepository;
    private final WeightChangeLogRepository changeLogRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public WeightConfigService(WeightConfigRepository weightConfigRepository,
                               WeightChangeLogRepository changeLogRepository,
                               ProjectRepository projectRepository,
                               UserRepository userRepository) {
        this.weightConfigRepository = weightConfigRepository;
        this.changeLogRepository = changeLogRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public WeightConfigResponse getWeights(UUID projectId) {
        WeightConfig config = weightConfigRepository.findByProjectId(projectId)
                .orElse(null);

        if (config == null) {
            // 기본 가중치 반환
            return new WeightConfigResponse(
                    null, projectId,
                    new BigDecimal("0.30"), new BigDecimal("0.25"),
                    new BigDecimal("0.20"), new BigDecimal("0.25"),
                    null
            );
        }

        return toResponse(config);
    }

    @Transactional
    public WeightConfigResponse updateWeights(UUID projectId, UUID professorId, WeightUpdateRequest request) {
        // 가중치 합이 1.00인지 검증
        BigDecimal sum = request.weightGit()
                .add(request.weightDoc())
                .add(request.weightMeeting())
                .add(request.weightTask());

        if (sum.compareTo(ONE) != 0) {
            throw new BusinessException(ErrorCode.INVALID_WEIGHT_SUM);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        WeightConfig config = weightConfigRepository.findByProjectId(projectId)
                .orElse(new WeightConfig(project, professor));

        // 변경 로그 기록
        String oldWeights = String.format(
                "{\"git\":%.2f,\"doc\":%.2f,\"meeting\":%.2f,\"task\":%.2f}",
                config.getWeightGit(), config.getWeightDoc(),
                config.getWeightMeeting(), config.getWeightTask());
        String newWeights = String.format(
                "{\"git\":%.2f,\"doc\":%.2f,\"meeting\":%.2f,\"task\":%.2f}",
                request.weightGit(), request.weightDoc(),
                request.weightMeeting(), request.weightTask());

        changeLogRepository.save(new WeightChangeLog(projectId, professorId, oldWeights, newWeights));

        // 가중치 업데이트
        config.setWeightGit(request.weightGit());
        config.setWeightDoc(request.weightDoc());
        config.setWeightMeeting(request.weightMeeting());
        config.setWeightTask(request.weightTask());
        config.setUpdatedAt(OffsetDateTime.now());

        weightConfigRepository.save(config);

        return toResponse(config);
    }

    private WeightConfigResponse toResponse(WeightConfig config) {
        return new WeightConfigResponse(
                config.getId(), config.getProject().getId(),
                config.getWeightGit(), config.getWeightDoc(),
                config.getWeightMeeting(), config.getWeightTask(),
                config.getUpdatedAt()
        );
    }
}
