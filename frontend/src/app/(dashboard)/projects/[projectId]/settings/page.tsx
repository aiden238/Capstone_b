'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { useIntegrationStore } from '@/store/integration';
import { useProjectStore } from '@/store/project';
import { useAuthStore } from '@/store/auth';

export default function SettingsPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { github, drive, fetchGithub, connectGithub, disconnectGithub, fetchDrive, disconnectDrive } =
    useIntegrationStore();
  const { members } = useProjectStore();
  const { user } = useAuthStore();
  const [githubForm, setGithubForm] = useState({ installationId: '', repoFullName: '' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isLeader = user && members.find((m) => m.userId === user.id)?.role === 'LEADER';

  useEffect(() => {
    if (projectId) {
      fetchGithub(projectId);
      fetchDrive(projectId);
    }
  }, [projectId, fetchGithub, fetchDrive]);

  const handleConnectGithub = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectId) return;
    setError(null);
    setSaving(true);
    try {
      await connectGithub(projectId, Number(githubForm.installationId), githubForm.repoFullName);
      setGithubForm({ installationId: '', repoFullName: '' });
    } catch {
      setError('GitHub 연동에 실패했습니다. Installation ID와 저장소 이름을 확인해주세요.');
    } finally {
      setSaving(false);
    }
  };

  const handleDisconnectGithub = async () => {
    if (!projectId || !confirm('GitHub 연동을 해제하시겠습니까?')) return;
    await disconnectGithub(projectId);
  };

  const handleDisconnectDrive = async () => {
    if (!projectId || !confirm('Google Drive 연동을 해제하시겠습니까?')) return;
    await disconnectDrive(projectId);
  };

  return (
    <div className="max-w-2xl space-y-6">
      <h2 className="text-lg font-semibold text-gray-900">외부 연동 설정</h2>

      {!isLeader && (
        <div className="rounded-md bg-yellow-50 p-4 text-sm text-yellow-800">
          연동 설정은 팀장(LEADER)만 변경할 수 있습니다.
        </div>
      )}

      {/* GitHub 연동 */}
      <div className="rounded-lg border border-gray-200 bg-white p-5">
        <div className="flex items-center gap-3 mb-4">
          <span className="text-2xl">🐙</span>
          <div>
            <h3 className="font-semibold text-gray-900">GitHub 저장소 연동</h3>
            <p className="text-xs text-gray-500">커밋·PR·이슈 활동이 기여도 점수에 반영됩니다</p>
          </div>
          {github && (
            <span className="ml-auto inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800">
              연동됨
            </span>
          )}
        </div>

        {github ? (
          <div className="space-y-3">
            <div className="rounded-md bg-gray-50 p-3 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">저장소</span>
                <a
                  href={`https://github.com/${github.repoFullName}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-mono text-blue-600 hover:underline"
                >
                  {github.repoFullName}
                </a>
              </div>
              <div className="flex justify-between mt-1">
                <span className="text-gray-500">연동일</span>
                <span>{new Date(github.createdAt).toLocaleDateString('ko-KR')}</span>
              </div>
            </div>
            {isLeader && (
              <button
                onClick={handleDisconnectGithub}
                className="text-sm text-red-600 hover:text-red-700 hover:underline"
              >
                연동 해제
              </button>
            )}
          </div>
        ) : isLeader ? (
          <form onSubmit={handleConnectGithub} className="space-y-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">
                GitHub App Installation ID
              </label>
              <input
                type="number"
                required
                placeholder="12345678"
                value={githubForm.installationId}
                onChange={(e) => setGithubForm((prev) => ({ ...prev, installationId: e.target.value }))}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">
                저장소 이름 (owner/repo)
              </label>
              <input
                type="text"
                required
                placeholder="myorg/myrepo"
                value={githubForm.repoFullName}
                onChange={(e) => setGithubForm((prev) => ({ ...prev, repoFullName: e.target.value }))}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
              />
            </div>
            {error && <p className="text-xs text-red-600">{error}</p>}
            <div className="flex items-center gap-3">
              <button
                type="submit"
                disabled={saving}
                className="rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:opacity-50"
              >
                {saving ? '연동 중...' : 'GitHub 연동하기'}
              </button>
              <a
                href="https://github.com/apps"
                target="_blank"
                rel="noopener noreferrer"
                className="text-xs text-blue-600 hover:underline"
              >
                GitHub App 설치 방법 →
              </a>
            </div>
          </form>
        ) : (
          <p className="text-sm text-gray-400">GitHub 저장소가 연동되지 않았습니다.</p>
        )}
      </div>

      {/* Google Drive 연동 */}
      <div className="rounded-lg border border-gray-200 bg-white p-5">
        <div className="flex items-center gap-3 mb-4">
          <span className="text-2xl">📂</span>
          <div>
            <h3 className="font-semibold text-gray-900">Google Drive 연동</h3>
            <p className="text-xs text-gray-500">문서 편집·생성·댓글 활동이 기여도 점수에 반영됩니다</p>
          </div>
          {drive && (
            <span className="ml-auto inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800">
              연동됨
            </span>
          )}
        </div>

        {drive ? (
          <div className="space-y-3">
            <div className="rounded-md bg-gray-50 p-3 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">폴더 ID</span>
                <span className="font-mono text-gray-700">{drive.driveFolderId}</span>
              </div>
              {drive.watchExpiry && (
                <div className="flex justify-between mt-1">
                  <span className="text-gray-500">Watch 만료</span>
                  <span>{new Date(drive.watchExpiry).toLocaleDateString('ko-KR')}</span>
                </div>
              )}
            </div>
            {isLeader && (
              <button
                onClick={handleDisconnectDrive}
                className="text-sm text-red-600 hover:text-red-700 hover:underline"
              >
                연동 해제
              </button>
            )}
          </div>
        ) : isLeader ? (
          <div className="space-y-3">
            <p className="text-sm text-gray-600">
              Google OAuth로 Drive 폴더를 연동하면 팀 문서 활동이 자동으로 수집됩니다.
            </p>
            <button
              onClick={() =>
                (window.location.href = `/api/projects/${projectId}/integrations/drive/auth`)
              }
              className="flex items-center gap-2 rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              <span>🔑</span> Google 계정으로 연동하기
            </button>
          </div>
        ) : (
          <p className="text-sm text-gray-400">Google Drive가 연동되지 않았습니다.</p>
        )}
      </div>
    </div>
  );
}
