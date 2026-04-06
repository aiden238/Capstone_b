'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { useScoreStore, ContributionScore } from '@/store/score';
import { useAuthStore } from '@/store/auth';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Radar,
} from 'recharts';

const CATEGORY_LABELS: Record<string, string> = {
  taskScore: '태스크',
  meetingScore: '회의',
  docScore: '문서',
  gitScore: 'Git',
};

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

export default function ScoresPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { scores, weights, loading, fetchScores, recalculate, fetchWeights, updateWeights } = useScoreStore();
  const { user } = useAuthStore();
  const [selectedUser, setSelectedUser] = useState<ContributionScore | null>(null);
  const [showWeightModal, setShowWeightModal] = useState(false);

  useEffect(() => {
    if (projectId) {
      fetchScores(projectId);
      fetchWeights(projectId);
    }
  }, [projectId, fetchScores, fetchWeights]);

  const isProfessorOrTA = user?.role === 'PROFESSOR' || user?.role === 'TA';

  // Bar chart data
  const barData = scores.map((s) => ({
    name: s.userName,
    태스크: Number(s.taskScore),
    회의: Number(s.meetingScore),
    문서: Number(s.docScore),
    Git: Number(s.gitScore),
    종합: Number(s.totalScore),
  }));

  // Radar data for selected user
  const radarData = selectedUser
    ? [
        { category: '태스크', score: Number(selectedUser.taskScore) },
        { category: '회의', score: Number(selectedUser.meetingScore) },
        { category: '문서', score: Number(selectedUser.docScore) },
        { category: 'Git', score: Number(selectedUser.gitScore) },
      ]
    : [];

  // Health indicator
  const getHealthColor = (score: number) => {
    if (score >= 80) return 'text-green-500';
    if (score >= 50) return 'text-yellow-500';
    if (score >= 30) return 'text-orange-500';
    return 'text-red-500';
  };

  const getHealthEmoji = (score: number) => {
    if (score >= 80) return '🟢';
    if (score >= 50) return '🟡';
    if (score >= 30) return '🟠';
    return '🔴';
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">팀 기여도</h2>
        <div className="flex gap-2">
          {isProfessorOrTA && (
            <button
              onClick={() => setShowWeightModal(true)}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              ⚙️ 가중치 설정
            </button>
          )}
          <button
            onClick={() => projectId && recalculate(projectId)}
            disabled={loading}
            className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? '계산 중...' : '🔄 재계산'}
          </button>
        </div>
      </div>

      {/* Score Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {scores.map((s, i) => (
          <button
            key={s.userId}
            onClick={() => setSelectedUser(s)}
            className={`rounded-lg border bg-white p-4 text-left shadow-sm transition hover:shadow-md ${
              selectedUser?.userId === s.userId ? 'border-blue-500 ring-2 ring-blue-200' : 'border-gray-200'
            }`}
          >
            <div className="flex items-center justify-between">
              <div>
                <div className="text-sm font-medium text-gray-900">{s.userName}</div>
                <div className="text-xs text-gray-500">{s.email}</div>
              </div>
              <div className="text-right">
                <div className={`text-2xl font-bold ${getHealthColor(Number(s.totalScore))}`}>
                  {Number(s.totalScore).toFixed(1)}
                </div>
                <div className="text-xs text-gray-400">
                  {getHealthEmoji(Number(s.totalScore))} 종합
                </div>
              </div>
            </div>
            <div className="mt-3 flex gap-2">
              {[
                { label: '태스크', value: s.taskScore, color: 'bg-blue-100 text-blue-700' },
                { label: '회의', value: s.meetingScore, color: 'bg-green-100 text-green-700' },
                { label: '문서', value: s.docScore, color: 'bg-yellow-100 text-yellow-700' },
                { label: 'Git', value: s.gitScore, color: 'bg-purple-100 text-purple-700' },
              ].map((c) => (
                <span key={c.label} className={`rounded px-1.5 py-0.5 text-xs font-medium ${c.color}`}>
                  {c.label} {Number(c.value).toFixed(0)}
                </span>
              ))}
            </div>
          </button>
        ))}
      </div>

      {/* Charts */}
      {scores.length > 0 && (
        <div className="grid gap-6 lg:grid-cols-2">
          {/* Bar Chart */}
          <div className="rounded-lg border border-gray-200 bg-white p-4">
            <h3 className="mb-4 text-sm font-semibold text-gray-700">카테고리별 기여도 비교</h3>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={barData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                <YAxis domain={[0, 150]} tick={{ fontSize: 12 }} />
                <Tooltip />
                <Legend />
                <Bar dataKey="태스크" fill="#3b82f6" />
                <Bar dataKey="회의" fill="#10b981" />
                <Bar dataKey="문서" fill="#f59e0b" />
                <Bar dataKey="Git" fill="#8b5cf6" />
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Radar Chart */}
          <div className="rounded-lg border border-gray-200 bg-white p-4">
            <h3 className="mb-4 text-sm font-semibold text-gray-700">
              {selectedUser ? `${selectedUser.userName} 역량 프로파일` : '팀원을 선택하세요'}
            </h3>
            {selectedUser ? (
              <ResponsiveContainer width="100%" height={300}>
                <RadarChart data={radarData}>
                  <PolarGrid />
                  <PolarAngleAxis dataKey="category" tick={{ fontSize: 12 }} />
                  <PolarRadiusAxis domain={[0, 150]} tick={{ fontSize: 10 }} />
                  <Radar name={selectedUser.userName} dataKey="score" stroke="#3b82f6" fill="#3b82f6" fillOpacity={0.3} />
                </RadarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-[300px] items-center justify-center text-sm text-gray-400">
                왼쪽 카드를 클릭하여 역량 레이더 차트를 확인하세요
              </div>
            )}
          </div>
        </div>
      )}

      {/* Total Score Ranking Bar */}
      {scores.length > 0 && (
        <div className="rounded-lg border border-gray-200 bg-white p-4">
          <h3 className="mb-4 text-sm font-semibold text-gray-700">종합 기여도 순위</h3>
          <div className="space-y-3">
            {[...scores]
              .sort((a, b) => Number(b.totalScore) - Number(a.totalScore))
              .map((s, i) => {
                const pct = Math.min(100, (Number(s.totalScore) / 150) * 100);
                return (
                  <div key={s.userId} className="flex items-center gap-3">
                    <span className="w-6 text-center text-sm font-bold text-gray-400">
                      {i + 1}
                    </span>
                    <span className="w-24 truncate text-sm font-medium text-gray-900">
                      {s.userName}
                    </span>
                    <div className="flex-1">
                      <div className="h-6 w-full overflow-hidden rounded-full bg-gray-100">
                        <div
                          className="h-full rounded-full bg-gradient-to-r from-blue-500 to-blue-600 transition-all duration-500"
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                    <span className={`w-12 text-right text-sm font-bold ${getHealthColor(Number(s.totalScore))}`}>
                      {Number(s.totalScore).toFixed(1)}
                    </span>
                  </div>
                );
              })}
          </div>
        </div>
      )}

      {/* Weight Info */}
      {weights && (
        <div className="rounded-lg border border-gray-200 bg-white p-4">
          <h3 className="mb-2 text-sm font-semibold text-gray-700">현재 가중치</h3>
          <div className="flex gap-4 text-sm text-gray-600">
            <span>Git: <strong>{(Number(weights.weightGit) * 100).toFixed(0)}%</strong></span>
            <span>문서: <strong>{(Number(weights.weightDoc) * 100).toFixed(0)}%</strong></span>
            <span>회의: <strong>{(Number(weights.weightMeeting) * 100).toFixed(0)}%</strong></span>
            <span>태스크: <strong>{(Number(weights.weightTask) * 100).toFixed(0)}%</strong></span>
          </div>
          {weights.updatedAt && (
            <div className="mt-1 text-xs text-gray-400">
              마지막 수정: {new Date(weights.updatedAt).toLocaleString('ko-KR')}
            </div>
          )}
        </div>
      )}

      {/* Weight Modal */}
      {showWeightModal && weights && (
        <WeightModal
          weights={weights}
          onClose={() => setShowWeightModal(false)}
          onSave={async (w) => {
            if (projectId) {
              await updateWeights(projectId, w);
              setShowWeightModal(false);
              await recalculate(projectId);
            }
          }}
        />
      )}

      {scores.length === 0 && !loading && (
        <div className="rounded-lg border-2 border-dashed border-gray-300 p-12 text-center">
          <p className="text-gray-500">아직 기여도 데이터가 없습니다</p>
          <p className="mt-1 text-sm text-gray-400">팀원들이 활동을 시작하면 자동으로 계산됩니다</p>
        </div>
      )}
    </div>
  );
}

// --- Weight Modal ---
function WeightModal({
  weights,
  onClose,
  onSave,
}: {
  weights: { weightGit: number; weightDoc: number; weightMeeting: number; weightTask: number };
  onClose: () => void;
  onSave: (w: { weightGit: number; weightDoc: number; weightMeeting: number; weightTask: number }) => Promise<void>;
}) {
  const [wGit, setWGit] = useState(Number(weights.weightGit) * 100);
  const [wDoc, setWDoc] = useState(Number(weights.weightDoc) * 100);
  const [wMeeting, setWMeeting] = useState(Number(weights.weightMeeting) * 100);
  const [wTask, setWTask] = useState(Number(weights.weightTask) * 100);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const sum = wGit + wDoc + wMeeting + wTask;

  const handleSave = async () => {
    if (Math.abs(sum - 100) > 0.01) {
      setError('가중치 합이 100%여야 합니다');
      return;
    }
    setSaving(true);
    try {
      await onSave({
        weightGit: wGit / 100,
        weightDoc: wDoc / 100,
        weightMeeting: wMeeting / 100,
        weightTask: wTask / 100,
      });
    } catch {
      setError('저장 실패');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={onClose}>
      <div className="w-full max-w-md rounded-lg bg-white p-6" onClick={(e) => e.stopPropagation()}>
        <h3 className="mb-4 text-lg font-bold text-gray-900">가중치 설정</h3>
        <div className="space-y-3">
          {[
            { label: 'Git', value: wGit, set: setWGit },
            { label: '문서', value: wDoc, set: setWDoc },
            { label: '회의', value: wMeeting, set: setWMeeting },
            { label: '태스크', value: wTask, set: setWTask },
          ].map((item) => (
            <div key={item.label} className="flex items-center gap-3">
              <span className="w-16 text-sm font-medium text-gray-700">{item.label}</span>
              <input
                type="range"
                min={0}
                max={100}
                step={5}
                value={item.value}
                onChange={(e) => item.set(Number(e.target.value))}
                className="flex-1"
              />
              <span className="w-12 text-right text-sm font-bold text-gray-900">{item.value}%</span>
            </div>
          ))}
        </div>
        <div className={`mt-3 text-sm font-medium ${Math.abs(sum - 100) > 0.01 ? 'text-red-500' : 'text-green-600'}`}>
          합계: {sum}%{Math.abs(sum - 100) > 0.01 ? ' (100%여야 합니다)' : ' ✓'}
        </div>
        {error && <div className="mt-2 text-sm text-red-500">{error}</div>}
        <div className="mt-4 flex justify-end gap-2">
          <button
            onClick={onClose}
            className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
          >
            취소
          </button>
          <button
            onClick={handleSave}
            disabled={saving || Math.abs(sum - 100) > 0.01}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {saving ? '저장 중...' : '저장 & 재계산'}
          </button>
        </div>
      </div>
    </div>
  );
}
