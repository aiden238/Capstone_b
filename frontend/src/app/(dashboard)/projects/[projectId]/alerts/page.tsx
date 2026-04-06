'use client';

import { useEffect } from 'react';
import { useParams } from 'next/navigation';
import { useAlertStore, Alert, AlertType, AlertSeverity } from '@/store/alert';
import { useScoreStore } from '@/store/score';

const ALERT_TYPE_CONFIG: Record<AlertType, { label: string; icon: string; color: string }> = {
  CRUNCH_TIME: { label: '벼락치기', icon: '⚡', color: 'bg-orange-100 text-orange-800' },
  FREE_RIDE: { label: '무임승차', icon: '🚫', color: 'bg-red-100 text-red-800' },
  DROPOUT: { label: '이탈 의심', icon: '👻', color: 'bg-purple-100 text-purple-800' },
  OVERLOAD: { label: '과부하', icon: '🔥', color: 'bg-yellow-100 text-yellow-800' },
  TAMPER: { label: '변조 감지', icon: '🛡️', color: 'bg-red-100 text-red-800' },
  GAMING_SUSPECT: { label: '조작 의심', icon: '🎮', color: 'bg-red-100 text-red-800' },
};

const SEVERITY_CONFIG: Record<AlertSeverity, { label: string; color: string; dot: string }> = {
  LOW: { label: '낮음', color: 'text-gray-500', dot: 'bg-gray-400' },
  MEDIUM: { label: '보통', color: 'text-yellow-600', dot: 'bg-yellow-400' },
  HIGH: { label: '높음', color: 'text-orange-600', dot: 'bg-orange-500' },
  CRITICAL: { label: '심각', color: 'text-red-600', dot: 'bg-red-500' },
};

export default function AlertsPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { alerts, unreadCount, loading, fetchAlerts, markAsRead, markAllAsRead, runDetection } = useAlertStore();
  const { recalculate } = useScoreStore();

  useEffect(() => {
    if (projectId) fetchAlerts(projectId);
  }, [projectId, fetchAlerts]);

  const handleDetect = async () => {
    if (!projectId) return;
    // Recalculate scores first, then run detection
    await recalculate(projectId);
    await runDetection(projectId);
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h2 className="text-lg font-bold text-gray-900">경보</h2>
          {unreadCount > 0 && (
            <span className="inline-flex items-center rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-medium text-red-800">
              {unreadCount}개 미확인
            </span>
          )}
        </div>
        <div className="flex gap-2">
          {unreadCount > 0 && (
            <button
              onClick={() => projectId && markAllAsRead(projectId)}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
            >
              ✓ 모두 읽음
            </button>
          )}
          <button
            onClick={handleDetect}
            disabled={loading}
            className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? '분석 중...' : '🔍 경보 감지 실행'}
          </button>
        </div>
      </div>

      {/* Alert List */}
      {alerts.length > 0 ? (
        <div className="space-y-2">
          {alerts.map((alert) => (
            <AlertCard
              key={alert.id}
              alert={alert}
              onMarkRead={() => projectId && markAsRead(projectId, alert.id)}
            />
          ))}
        </div>
      ) : (
        <div className="rounded-lg border-2 border-dashed border-gray-300 p-12 text-center">
          <p className="text-4xl">🎉</p>
          <p className="mt-2 text-gray-500">경보가 없습니다</p>
          <p className="mt-1 text-sm text-gray-400">&quot;경보 감지 실행&quot;을 클릭하여 팀 상태를 점검하세요</p>
        </div>
      )}
    </div>
  );
}

function AlertCard({ alert, onMarkRead }: { alert: Alert; onMarkRead: () => void }) {
  const typeConfig = ALERT_TYPE_CONFIG[alert.alertType] || { label: alert.alertType, icon: '⚠️', color: 'bg-gray-100 text-gray-800' };
  const sevConfig = SEVERITY_CONFIG[alert.severity] || SEVERITY_CONFIG.MEDIUM;

  return (
    <div
      className={`rounded-lg border bg-white p-4 transition ${
        alert.isRead ? 'border-gray-100 opacity-60' : 'border-gray-200 shadow-sm'
      }`}
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1">
          <div className="flex items-center gap-2">
            {!alert.isRead && (
              <span className={`inline-block h-2 w-2 rounded-full ${sevConfig.dot}`} />
            )}
            <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${typeConfig.color}`}>
              {typeConfig.icon} {typeConfig.label}
            </span>
            <span className={`text-xs font-medium ${sevConfig.color}`}>
              {sevConfig.label}
            </span>
          </div>
          <p className="mt-1.5 text-sm text-gray-800">{alert.message}</p>
          <p className="mt-1 text-xs text-gray-400">
            {new Date(alert.createdAt).toLocaleString('ko-KR')}
          </p>
        </div>
        {!alert.isRead && (
          <button
            onClick={onMarkRead}
            className="shrink-0 rounded-md border border-gray-200 px-2 py-1 text-xs text-gray-500 hover:bg-gray-50"
          >
            확인
          </button>
        )}
      </div>
    </div>
  );
}
