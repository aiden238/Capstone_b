'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { useTimelineStore, TimelineEntry } from '@/store/timeline';
import { useProjectStore } from '@/store/project';

const SOURCE_COLORS: Record<string, { bg: string; text: string; dot: string }> = {
  PLATFORM: { bg: 'bg-gray-100', text: 'text-gray-700', dot: 'bg-gray-400' },
  GITHUB: { bg: 'bg-green-100', text: 'text-green-700', dot: 'bg-green-500' },
  GOOGLE_DRIVE: { bg: 'bg-blue-100', text: 'text-blue-700', dot: 'bg-blue-500' },
};

const ACTION_LABELS: Record<string, string> = {
  TASK_CREATE: '태스크 생성',
  TASK_UPDATE: '태스크 수정',
  TASK_STATUS_CHANGE: '태스크 상태 변경',
  TASK_COMPLETE: '태스크 완료',
  TASK_DELETE: '태스크 삭제',
  TASK_ASSIGN: '태스크 배정',
  TASK_UNASSIGN: '태스크 배정 해제',
  MEETING_CREATE: '회의 생성',
  MEETING_UPDATE: '회의 수정',
  MEETING_DELETE: '회의 삭제',
  MEETING_CHECKIN: '회의 체크인',
  FILE_UPLOAD: '파일 업로드',
  COMMIT: '커밋',
  PR_OPEN: 'PR 생성',
  PR_MERGE: 'PR 머지',
  ISSUE_CREATE: '이슈 생성',
  ISSUE_CLOSE: '이슈 종료',
  CODE_REVIEW: '코드 리뷰',
  DOC_EDIT: '문서 수정',
  DOC_CREATE: '문서 생성',
  DOC_COMMENT: '문서 댓글',
};

const SOURCE_LABELS: Record<string, string> = {
  PLATFORM: '플랫폼',
  GITHUB: 'GitHub',
  GOOGLE_DRIVE: 'Google Drive',
};

function parseMetadata(metadata: string | null): Record<string, unknown> {
  if (!metadata) return {};
  try {
    return JSON.parse(metadata);
  } catch {
    return {};
  }
}

function MetadataSummary({ entry }: { entry: TimelineEntry }) {
  const meta = parseMetadata(entry.metadata);

  if (entry.source === 'GITHUB') {
    if (entry.actionType === 'COMMIT') {
      return (
        <span className="text-xs text-gray-500">
          {meta.commitId && <code className="rounded bg-gray-100 px-1">{String(meta.commitId)}</code>}{' '}
          {meta.message && String(meta.message)}
        </span>
      );
    }
    if (entry.actionType === 'PR_OPEN' || entry.actionType === 'PR_MERGE') {
      return (
        <span className="text-xs text-gray-500">
          #{String(meta.prNumber)} {String(meta.title || '')}
          {meta.additions != null && (
            <span className="ml-1 text-green-600">+{String(meta.additions)}</span>
          )}
          {meta.deletions != null && (
            <span className="ml-1 text-red-600">-{String(meta.deletions)}</span>
          )}
        </span>
      );
    }
    if (entry.actionType === 'ISSUE_CREATE' || entry.actionType === 'ISSUE_CLOSE') {
      return (
        <span className="text-xs text-gray-500">
          #{String(meta.issueNumber)} {String(meta.title || '')}
        </span>
      );
    }
    if (entry.actionType === 'CODE_REVIEW') {
      return (
        <span className="text-xs text-gray-500">
          PR #{String(meta.prNumber)} — {String(meta.state || '')}
        </span>
      );
    }
  }

  if (entry.source === 'GOOGLE_DRIVE') {
    return (
      <span className="text-xs text-gray-500">
        {meta.fileName && String(meta.fileName)}
      </span>
    );
  }

  return null;
}

export default function TimelinePage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { entries, loading, hasMore, fetchTimeline, loadMore, reset } = useTimelineStore();
  const { members, fetchMembers } = useProjectStore();

  const [sourceFilter, setSourceFilter] = useState<string>('');
  const [userFilter, setUserFilter] = useState<string>('');

  useEffect(() => {
    if (projectId) {
      reset();
      fetchTimeline(projectId, {
        source: sourceFilter || undefined,
        userId: userFilter || undefined,
      });
      if (members.length === 0) fetchMembers(projectId);
    }
  }, [projectId, sourceFilter, userFilter]);

  const handleLoadMore = () => {
    if (projectId) {
      loadMore(projectId, {
        source: sourceFilter || undefined,
        userId: userFilter || undefined,
      });
    }
  };

  // 날짜별 그룹핑
  const groupedEntries: Record<string, TimelineEntry[]> = {};
  entries.forEach((entry) => {
    const date = new Date(entry.occurredAt).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      weekday: 'short',
    });
    if (!groupedEntries[date]) groupedEntries[date] = [];
    groupedEntries[date].push(entry);
  });

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">통합 타임라인</h2>
        <div className="flex gap-2">
          <select
            value={sourceFilter}
            onChange={(e) => setSourceFilter(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm"
          >
            <option value="">전체 소스</option>
            <option value="PLATFORM">플랫폼</option>
            <option value="GITHUB">GitHub</option>
            <option value="GOOGLE_DRIVE">Google Drive</option>
          </select>
          <select
            value={userFilter}
            onChange={(e) => setUserFilter(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm"
          >
            <option value="">전체 멤버</option>
            {members.map((m) => (
              <option key={m.userId} value={m.userId}>
                {m.userName}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* 소스별 범례 */}
      <div className="mb-4 flex gap-4">
        {Object.entries(SOURCE_COLORS).map(([key, colors]) => (
          <div key={key} className="flex items-center gap-1.5 text-xs text-gray-600">
            <span className={`inline-block h-2.5 w-2.5 rounded-full ${colors.dot}`} />
            {SOURCE_LABELS[key]}
          </div>
        ))}
      </div>

      {entries.length === 0 && !loading ? (
        <div className="rounded-lg border border-dashed border-gray-300 py-16 text-center text-gray-500">
          <p className="text-lg font-medium">활동 기록이 없습니다</p>
          <p className="mt-1 text-sm">태스크, 회의, 파일 업로드 등의 활동이 이곳에 표시됩니다.</p>
        </div>
      ) : (
        <div className="relative">
          {/* 세로 타임라인 선 */}
          <div className="absolute left-4 top-0 h-full w-0.5 bg-gray-200" />

          {Object.entries(groupedEntries).map(([date, dayEntries]) => (
            <div key={date} className="mb-6">
              <div className="relative mb-3 ml-10 text-sm font-semibold text-gray-700">{date}</div>
              {dayEntries.map((entry) => {
                const colors = SOURCE_COLORS[entry.source] || SOURCE_COLORS.PLATFORM;
                return (
                  <div key={entry.id} className="relative mb-3 flex items-start">
                    {/* 타임라인 도트 */}
                    <div
                      className={`relative z-10 mt-1.5 h-3 w-3 flex-shrink-0 rounded-full border-2 border-white ${colors.dot}`}
                      style={{ marginLeft: '10px' }}
                    />
                    {/* 카드 */}
                    <div className="ml-4 flex-1 rounded-lg border border-gray-200 bg-white p-3 shadow-sm">
                      <div className="flex items-center gap-2">
                        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${colors.bg} ${colors.text}`}>
                          {SOURCE_LABELS[entry.source]}
                        </span>
                        <span className="text-sm font-medium text-gray-900">
                          {ACTION_LABELS[entry.actionType] || entry.actionType}
                        </span>
                        <span className="ml-auto text-xs text-gray-400">
                          {new Date(entry.occurredAt).toLocaleTimeString('ko-KR', {
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                        </span>
                      </div>
                      <div className="mt-1 flex items-center gap-2">
                        <span className="text-xs font-medium text-gray-600">{entry.userName}</span>
                        <MetadataSummary entry={entry} />
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          ))}

          {hasMore && (
            <div className="mt-4 text-center">
              <button
                onClick={handleLoadMore}
                disabled={loading}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50"
              >
                {loading ? '로딩 중...' : '더 보기'}
              </button>
            </div>
          )}
        </div>
      )}

      {loading && entries.length === 0 && (
        <div className="py-16 text-center text-gray-500">로딩 중...</div>
      )}
    </div>
  );
}
