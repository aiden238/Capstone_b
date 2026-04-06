'use client';

import { useEffect, useMemo } from 'react';
import Link from 'next/link';
import { useDashboardStore, ProjectSummary } from '@/store/dashboard';

const HEALTH_CONFIG: Record<string, { label: string; color: string; bg: string; icon: string }> = {
  HEALTHY: { label: '양호', color: 'text-green-700', bg: 'bg-green-50 border-green-200', icon: '🟢' },
  WARNING: { label: '주의', color: 'text-yellow-700', bg: 'bg-yellow-50 border-yellow-200', icon: '🟡' },
  DANGER:  { label: '위험', color: 'text-red-700', bg: 'bg-red-50 border-red-200', icon: '🔴' },
};

function ProgressBar({ done, total, label }: { done: number; total: number; label: string }) {
  const pct = total > 0 ? Math.round((done / total) * 100) : 0;
  return (
    <div>
      <div className="flex justify-between text-xs text-gray-500 mb-1">
        <span>{label}</span>
        <span>{done}/{total} ({pct}%)</span>
      </div>
      <div className="h-2 w-full rounded-full bg-gray-100">
        <div
          className="h-2 rounded-full bg-blue-500 transition-all"
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}

function ScoreBar({ label, value }: { label: string; value: number }) {
  const capped = Math.min(value, 150);
  const pct = (capped / 150) * 100;
  const color = value >= 120 ? 'bg-green-500' : value >= 80 ? 'bg-blue-500' : value >= 50 ? 'bg-yellow-500' : 'bg-red-500';
  return (
    <div className="flex items-center gap-2 text-xs">
      <span className="w-8 text-gray-500 text-right">{label}</span>
      <div className="flex-1 h-1.5 rounded-full bg-gray-100">
        <div className={`h-1.5 rounded-full ${color} transition-all`} style={{ width: `${pct}%` }} />
      </div>
      <span className="w-8 text-gray-600">{value.toFixed(0)}</span>
    </div>
  );
}

function timeAgo(dateStr: string | null): string {
  if (!dateStr) return '활동 없음';
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins}분 전`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  return `${days}일 전`;
}

function ProjectCard({ s }: { s: ProjectSummary }) {
  const health = HEALTH_CONFIG[s.healthStatus] || HEALTH_CONFIG.HEALTHY;

  return (
    <Link
      href={`/projects/${s.projectId}/board`}
      className={`block rounded-xl border p-5 transition-all hover:shadow-md ${health.bg}`}
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <div className="min-w-0 flex-1">
          <h3 className="font-semibold text-gray-900 truncate">{s.projectName}</h3>
          <p className="text-xs text-gray-500 mt-0.5">
            {s.courseName && <span>{s.courseName}</span>}
            {s.semester && <span> · {s.semester}</span>}
          </p>
        </div>
        <span className={`ml-2 flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${health.color}`}>
          {health.icon} {health.label}
        </span>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-3 gap-3 mb-3 text-center">
        <div className="rounded-lg bg-white/60 p-2">
          <div className="text-lg font-bold text-gray-900">{s.memberCount}</div>
          <div className="text-[10px] text-gray-500">팀원</div>
        </div>
        <div className="rounded-lg bg-white/60 p-2">
          <div className="text-lg font-bold text-gray-900">{s.taskTotal}</div>
          <div className="text-[10px] text-gray-500">태스크</div>
        </div>
        <div className="rounded-lg bg-white/60 p-2">
          <div className={`text-lg font-bold ${s.unreadAlertCount > 0 ? 'text-red-600' : 'text-gray-900'}`}>
            {s.unreadAlertCount}
          </div>
          <div className="text-[10px] text-gray-500">미확인 경보</div>
        </div>
      </div>

      {/* Progress */}
      <ProgressBar done={s.taskDone} total={s.taskTotal} label="진행률" />

      {/* Score Summary */}
      <div className="mt-3 space-y-1">
        <ScoreBar label="평균" value={s.scoreAvg} />
        <ScoreBar label="최고" value={s.scoreMax} />
        <ScoreBar label="최저" value={s.scoreMin} />
      </div>

      {/* Footer */}
      <div className="mt-3 flex items-center justify-between text-[10px] text-gray-400">
        <span>최근 활동: {timeAgo(s.lastActivityAt)}</span>
        <span>경보 총 {s.totalAlertCount}건</span>
      </div>
    </Link>
  );
}

export default function OverviewPage() {
  const { summaries, loading, fetchOverview } = useDashboardStore();

  useEffect(() => {
    fetchOverview();
  }, [fetchOverview]);

  // 통계
  const stats = useMemo(() => {
    if (summaries.length === 0) return null;
    const totalProjects = summaries.length;
    const totalMembers = summaries.reduce((a, s) => a + s.memberCount, 0);
    const totalAlerts = summaries.reduce((a, s) => a + s.unreadAlertCount, 0);
    const dangerCount = summaries.filter((s) => s.healthStatus === 'DANGER').length;
    const warningCount = summaries.filter((s) => s.healthStatus === 'WARNING').length;
    const healthyCount = summaries.filter((s) => s.healthStatus === 'HEALTHY').length;
    return { totalProjects, totalMembers, totalAlerts, dangerCount, warningCount, healthyCount };
  }, [summaries]);

  // 정렬: DANGER → WARNING → HEALTHY
  const sorted = useMemo(() => {
    const order: Record<string, number> = { DANGER: 0, WARNING: 1, HEALTHY: 2 };
    return [...summaries].sort((a, b) => (order[a.healthStatus] ?? 2) - (order[b.healthStatus] ?? 2));
  }, [summaries]);

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="text-gray-500">프로젝트 요약을 불러오는 중...</div>
      </div>
    );
  }

  return (
    <div>
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">교수 대시보드</h1>
        <p className="mt-1 text-sm text-gray-500">전체 프로젝트 현황을 한눈에 확인합니다</p>
      </div>

      {/* Summary Stats */}
      {stats && (
        <div className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
          <div className="rounded-xl border border-gray-200 bg-white p-4 text-center">
            <div className="text-2xl font-bold text-gray-900">{stats.totalProjects}</div>
            <div className="text-xs text-gray-500">전체 프로젝트</div>
          </div>
          <div className="rounded-xl border border-gray-200 bg-white p-4 text-center">
            <div className="text-2xl font-bold text-gray-900">{stats.totalMembers}</div>
            <div className="text-xs text-gray-500">전체 학생</div>
          </div>
          <div className="rounded-xl border border-gray-200 bg-white p-4 text-center">
            <div className={`text-2xl font-bold ${stats.totalAlerts > 0 ? 'text-red-600' : 'text-gray-900'}`}>
              {stats.totalAlerts}
            </div>
            <div className="text-xs text-gray-500">미확인 경보</div>
          </div>
          <div className="rounded-xl border border-green-200 bg-green-50 p-4 text-center">
            <div className="text-2xl font-bold text-green-700">{stats.healthyCount}</div>
            <div className="text-xs text-green-600">양호</div>
          </div>
          <div className="rounded-xl border border-yellow-200 bg-yellow-50 p-4 text-center">
            <div className="text-2xl font-bold text-yellow-700">{stats.warningCount}</div>
            <div className="text-xs text-yellow-600">주의</div>
          </div>
          <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-center">
            <div className="text-2xl font-bold text-red-700">{stats.dangerCount}</div>
            <div className="text-xs text-red-600">위험</div>
          </div>
        </div>
      )}

      {/* Project Cards */}
      {sorted.length === 0 ? (
        <div className="rounded-xl border-2 border-dashed border-gray-300 p-12 text-center">
          <div className="text-4xl mb-3">📊</div>
          <h3 className="text-lg font-medium text-gray-900">참여 중인 프로젝트가 없습니다</h3>
          <p className="mt-1 text-sm text-gray-500">
            프로젝트에 초대코드로 참여하거나 새 프로젝트를 생성하세요
          </p>
          <Link
            href="/projects"
            className="mt-4 inline-block rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            프로젝트 목록으로
          </Link>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {sorted.map((s) => (
            <ProjectCard key={s.projectId} s={s} />
          ))}
        </div>
      )}
    </div>
  );
}
