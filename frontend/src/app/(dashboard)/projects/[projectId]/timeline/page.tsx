'use client';

import { useEffect } from 'react';
import { useParams } from 'next/navigation';
import { useActivityStore, ActivitySource, ActionType } from '@/store/activity';

const SOURCE_COLORS: Record<ActivitySource, string> = {
  PLATFORM: 'bg-blue-100 text-blue-700',
  GITHUB: 'bg-green-100 text-green-700',
  GOOGLE_DRIVE: 'bg-yellow-100 text-yellow-800',
  MANUAL: 'bg-gray-100 text-gray-600',
};

const SOURCE_LABELS: Record<ActivitySource, string> = {
  PLATFORM: '플랫폼',
  GITHUB: 'GitHub',
  GOOGLE_DRIVE: 'Drive',
  MANUAL: '수동',
};

const ACTION_LABELS: Record<ActionType, string> = {
  TASK_CREATE: '태스크 생성',
  TASK_UPDATE: '태스크 수정',
  TASK_STATUS_CHANGE: '상태 변경',
  TASK_COMPLETE: '태스크 완료',
  TASK_DELETE: '태스크 삭제',
  TASK_ASSIGN: '담당자 배정',
  TASK_UNASSIGN: '담당자 해제',
  MEETING_CREATE: '회의 생성',
  MEETING_UPDATE: '회의 수정',
  MEETING_DELETE: '회의 삭제',
  MEETING_CHECKIN: '회의 체크인',
  FILE_UPLOAD: '파일 업로드',
  COMMIT: '커밋',
  PR_OPEN: 'PR 오픈',
  PR_MERGE: 'PR 병합',
  ISSUE_CREATE: '이슈 생성',
  ISSUE_CLOSE: '이슈 종료',
  CODE_REVIEW: '코드 리뷰',
  DOC_EDIT: '문서 수정',
  DOC_CREATE: '문서 생성',
  DOC_COMMENT: '문서 댓글',
};

function formatDateTime(iso: string) {
  const d = new Date(iso);
  return d.toLocaleDateString('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function TimelinePage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { activities, totalPages, currentPage, loading, fetchActivities } = useActivityStore();

  useEffect(() => {
    if (projectId) fetchActivities(projectId, 0);
  }, [projectId, fetchActivities]);

  const handlePrev = () => {
    if (currentPage > 0) fetchActivities(projectId!, currentPage - 1);
  };
  const handleNext = () => {
    if (currentPage < totalPages - 1) fetchActivities(projectId!, currentPage + 1);
  };

  return (
    <div className="max-w-3xl">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-900">활동 타임라인</h2>
        <div className="flex items-center gap-2 text-xs text-gray-500">
          {Object.entries(SOURCE_LABELS).map(([src, label]) => (
            <span
              key={src}
              className={`inline-flex items-center rounded-full px-2 py-0.5 ${SOURCE_COLORS[src as ActivitySource]}`}
            >
              {label}
            </span>
          ))}
        </div>
      </div>

      {loading ? (
        <div className="py-10 text-center text-sm text-gray-400">불러오는 중...</div>
      ) : activities.length === 0 ? (
        <div className="py-10 text-center text-sm text-gray-400">아직 활동 기록이 없습니다.</div>
      ) : (
        <div className="relative border-l-2 border-gray-200 pl-6 space-y-4">
          {activities.map((log) => (
            <div key={log.id} className="relative">
              <span className="absolute -left-[29px] top-1 h-3 w-3 rounded-full border-2 border-white bg-gray-400 ring-1 ring-gray-200" />
              <div className="rounded-lg border border-gray-100 bg-white p-3 shadow-sm">
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span
                      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${SOURCE_COLORS[log.source]}`}
                    >
                      {SOURCE_LABELS[log.source]}
                    </span>
                    <span className="text-sm font-medium text-gray-800">
                      {ACTION_LABELS[log.actionType] ?? log.actionType}
                    </span>
                    {log.qualityScore != null && log.actionType === 'COMMIT' && (
                      <span
                        className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                          log.qualityScore >= 1.2
                            ? 'bg-purple-100 text-purple-700'
                            : log.qualityScore >= 0.8
                            ? 'bg-blue-100 text-blue-700'
                            : 'bg-gray-100 text-gray-500'
                        }`}
                        title={log.qualityReason ?? ''}
                      >
                        AI 품질 {log.qualityScore.toFixed(1)}
                      </span>
                    )}
                  </div>
                  <span className="shrink-0 text-xs text-gray-400">
                    {formatDateTime(log.occurredAt)}
                  </span>
                </div>
                {log.metadata && (
                  <div className="mt-1 text-xs text-gray-500 truncate">
                    {Object.entries(log.metadata)
                      .map(([k, v]) => `${k}: ${v}`)
                      .join(' · ')}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-3">
          <button
            onClick={handlePrev}
            disabled={currentPage === 0}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-40"
          >
            ← 이전
          </button>
          <span className="text-sm text-gray-500">
            {currentPage + 1} / {totalPages}
          </span>
          <button
            onClick={handleNext}
            disabled={currentPage >= totalPages - 1}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-40"
          >
            다음 →
          </button>
        </div>
      )}
    </div>
  );
}
