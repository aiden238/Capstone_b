'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { useIntegrationStore, Integration } from '@/store/integration';
import { useProjectStore } from '@/store/project';
import { useAuthStore } from '@/store/auth';

export default function IntegrationsPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { integrations, mappings, loading, fetchIntegrations, createIntegration, deleteIntegration, updateStatus, fetchMappings, createMapping, deleteMapping } = useIntegrationStore();
  const { members, fetchMembers } = useProjectStore();
  const { user } = useAuthStore();

  const [showAddGitHub, setShowAddGitHub] = useState(false);
  const [showAddDrive, setShowAddDrive] = useState(false);
  const [showAddMapping, setShowAddMapping] = useState(false);

  // GitHub form
  const [repoFullName, setRepoFullName] = useState('');
  const [installationId, setInstallationId] = useState('');

  // Drive form
  const [folderId, setFolderId] = useState('');
  const [folderName, setFolderName] = useState('');

  // Mapping form
  const [mappingUserId, setMappingUserId] = useState('');
  const [githubUsername, setGithubUsername] = useState('');

  useEffect(() => {
    if (projectId) {
      fetchIntegrations(projectId);
      fetchMappings(projectId);
      if (members.length === 0) fetchMembers(projectId);
    }
  }, [projectId]);

  const currentMember = members.find((m) => m.userId === user?.id);
  const isLeader = currentMember?.role === 'LEADER';

  const handleAddGitHub = async () => {
    if (!projectId || !repoFullName.trim()) return;
    await createIntegration(projectId, {
      provider: 'GITHUB_APP',
      externalId: repoFullName.trim(),
      externalName: repoFullName.trim(),
      installationId: installationId ? Number(installationId) : undefined,
    });
    setRepoFullName('');
    setInstallationId('');
    setShowAddGitHub(false);
  };

  const handleAddDrive = async () => {
    if (!projectId || !folderId.trim()) return;
    await createIntegration(projectId, {
      provider: 'GOOGLE_DRIVE',
      externalId: folderId.trim(),
      externalName: folderName.trim() || folderId.trim(),
    });
    setFolderId('');
    setFolderName('');
    setShowAddDrive(false);
  };

  const handleAddMapping = async () => {
    if (!projectId || !mappingUserId || !githubUsername.trim()) return;
    await createMapping(projectId, mappingUserId, githubUsername.trim());
    setMappingUserId('');
    setGithubUsername('');
    setShowAddMapping(false);
  };

  const githubIntegrations = integrations.filter((i) => i.provider === 'GITHUB_APP');
  const driveIntegrations = integrations.filter((i) => i.provider === 'GOOGLE_DRIVE');

  return (
    <div className="space-y-8">
      <h2 className="text-lg font-bold text-gray-900">외부 서비스 연동</h2>

      {/* GitHub App Section */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-md bg-gray-900 text-white text-sm font-bold">G</div>
            <h3 className="font-semibold text-gray-800">GitHub 연동</h3>
            <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs text-green-700">
              {githubIntegrations.length}개 저장소
            </span>
          </div>
          {isLeader && (
            <button
              onClick={() => setShowAddGitHub(true)}
              className="rounded-md bg-gray-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-gray-800"
            >
              + 저장소 연결
            </button>
          )}
        </div>

        {githubIntegrations.length === 0 ? (
          <div className="rounded-lg border border-dashed border-gray-300 p-6 text-center text-sm text-gray-500">
            연결된 GitHub 저장소가 없습니다
          </div>
        ) : (
          <div className="space-y-2">
            {githubIntegrations.map((integration) => (
              <IntegrationCard
                key={integration.id}
                integration={integration}
                projectId={projectId}
                isLeader={isLeader}
                onDelete={deleteIntegration}
                onUpdateStatus={updateStatus}
              />
            ))}
          </div>
        )}
      </section>

      {/* Google Drive Section */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-md bg-blue-600 text-white text-sm font-bold">D</div>
            <h3 className="font-semibold text-gray-800">Google Drive 연동</h3>
            <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs text-blue-700">
              {driveIntegrations.length}개 폴더
            </span>
          </div>
          {isLeader && (
            <button
              onClick={() => setShowAddDrive(true)}
              className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700"
            >
              + 폴더 연결
            </button>
          )}
        </div>

        {driveIntegrations.length === 0 ? (
          <div className="rounded-lg border border-dashed border-gray-300 p-6 text-center text-sm text-gray-500">
            연결된 Google Drive 폴더가 없습니다
          </div>
        ) : (
          <div className="space-y-2">
            {driveIntegrations.map((integration) => (
              <IntegrationCard
                key={integration.id}
                integration={integration}
                projectId={projectId}
                isLeader={isLeader}
                onDelete={deleteIntegration}
                onUpdateStatus={updateStatus}
              />
            ))}
          </div>
        )}
      </section>

      {/* GitHub User Mapping Section */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <h3 className="font-semibold text-gray-800">GitHub 사용자 매핑</h3>
          <button
            onClick={() => setShowAddMapping(true)}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
          >
            + 매핑 추가
          </button>
        </div>
        <p className="mb-3 text-xs text-gray-500">
          팀원의 GitHub 계정을 플랫폼 계정에 매핑하면 커밋/PR/이슈가 자동으로 해당 팀원의 기여도에 반영됩니다.
        </p>

        {mappings.length === 0 ? (
          <div className="rounded-lg border border-dashed border-gray-300 p-6 text-center text-sm text-gray-500">
            매핑된 사용자가 없습니다
          </div>
        ) : (
          <div className="overflow-hidden rounded-lg border border-gray-200">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">팀원</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">GitHub 계정</th>
                  <th className="px-4 py-2 text-right text-xs font-medium text-gray-500">작업</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 bg-white">
                {mappings.map((m) => (
                  <tr key={m.id}>
                    <td className="px-4 py-2 text-sm text-gray-900">{m.userName}</td>
                    <td className="px-4 py-2 text-sm text-gray-600">@{m.githubUsername}</td>
                    <td className="px-4 py-2 text-right">
                      <button
                        onClick={() => projectId && deleteMapping(projectId, m.id)}
                        className="text-xs text-red-600 hover:text-red-800"
                      >
                        삭제
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Add GitHub Modal */}
      {showAddGitHub && (
        <Modal title="GitHub 저장소 연결" onClose={() => setShowAddGitHub(false)}>
          <div className="space-y-3">
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">저장소 (owner/repo)</label>
              <input
                type="text"
                value={repoFullName}
                onChange={(e) => setRepoFullName(e.target.value)}
                placeholder="예: team-blackbox/capstone"
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">Installation ID (선택)</label>
              <input
                type="text"
                value={installationId}
                onChange={(e) => setInstallationId(e.target.value)}
                placeholder="GitHub App 설치 후 발급된 ID"
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
            </div>
            <div className="rounded-md bg-yellow-50 p-3 text-xs text-yellow-800">
              <strong>안내:</strong> GitHub App이 해당 저장소에 설치되어 있어야 Webhook이 동작합니다.
              읽기 전용(contents:read, pull_requests:read, issues:read) 권한만 사용합니다.
            </div>
            <div className="flex justify-end gap-2">
              <button onClick={() => setShowAddGitHub(false)} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">취소</button>
              <button onClick={handleAddGitHub} className="rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-800">연결</button>
            </div>
          </div>
        </Modal>
      )}

      {/* Add Drive Modal */}
      {showAddDrive && (
        <Modal title="Google Drive 폴더 연결" onClose={() => setShowAddDrive(false)}>
          <div className="space-y-3">
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">폴더 ID</label>
              <input
                type="text"
                value={folderId}
                onChange={(e) => setFolderId(e.target.value)}
                placeholder="Google Drive 폴더 URL의 ID 부분"
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">폴더 이름 (선택)</label>
              <input
                type="text"
                value={folderName}
                onChange={(e) => setFolderName(e.target.value)}
                placeholder="표시용 이름"
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
            </div>
            <div className="rounded-md bg-blue-50 p-3 text-xs text-blue-800">
              <strong>안내:</strong> Google Drive API의 읽기 전용(drive.readonly) 권한만 사용합니다.
              문서 수정 이력과 댓글 데이터만 수집합니다.
            </div>
            <div className="flex justify-end gap-2">
              <button onClick={() => setShowAddDrive(false)} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">취소</button>
              <button onClick={handleAddDrive} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700">연결</button>
            </div>
          </div>
        </Modal>
      )}

      {/* Add Mapping Modal */}
      {showAddMapping && (
        <Modal title="GitHub 사용자 매핑" onClose={() => setShowAddMapping(false)}>
          <div className="space-y-3">
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">팀원</label>
              <select
                value={mappingUserId}
                onChange={(e) => setMappingUserId(e.target.value)}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
              >
                <option value="">선택하세요</option>
                {members
                  .filter((m) => m.role !== 'OBSERVER')
                  .filter((m) => !mappings.some((map) => map.userId === m.userId))
                  .map((m) => (
                    <option key={m.userId} value={m.userId}>{m.userName}</option>
                  ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">GitHub Username</label>
              <input
                type="text"
                value={githubUsername}
                onChange={(e) => setGithubUsername(e.target.value)}
                placeholder="예: octocat"
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
            </div>
            <div className="flex justify-end gap-2">
              <button onClick={() => setShowAddMapping(false)} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">취소</button>
              <button onClick={handleAddMapping} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700">매핑</button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}

function IntegrationCard({
  integration,
  projectId,
  isLeader,
  onDelete,
  onUpdateStatus,
}: {
  integration: Integration;
  projectId: string;
  isLeader: boolean;
  onDelete: (projectId: string, id: string) => Promise<void>;
  onUpdateStatus: (projectId: string, id: string, status: string) => Promise<void>;
}) {
  const statusColors: Record<string, string> = {
    ACTIVE: 'bg-green-100 text-green-700',
    PAUSED: 'bg-yellow-100 text-yellow-700',
    ERROR: 'bg-red-100 text-red-700',
  };

  const statusLabels: Record<string, string> = {
    ACTIVE: '활성',
    PAUSED: '일시정지',
    ERROR: '오류',
  };

  return (
    <div className="flex items-center justify-between rounded-lg border border-gray-200 bg-white p-4">
      <div>
        <div className="flex items-center gap-2">
          <span className="font-medium text-gray-900">{integration.externalName || integration.externalId}</span>
          <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${statusColors[integration.syncStatus]}`}>
            {statusLabels[integration.syncStatus]}
          </span>
        </div>
        <div className="mt-0.5 text-xs text-gray-500">
          {integration.externalId}
          {integration.lastSynced && (
            <span className="ml-2">
              마지막 동기화: {new Date(integration.lastSynced).toLocaleString('ko-KR')}
            </span>
          )}
        </div>
        {integration.errorMessage && (
          <div className="mt-1 text-xs text-red-600">{integration.errorMessage}</div>
        )}
      </div>
      {isLeader && (
        <div className="flex gap-2">
          {integration.syncStatus === 'ACTIVE' ? (
            <button
              onClick={() => onUpdateStatus(projectId, integration.id, 'PAUSED')}
              className="rounded-md border border-gray-300 px-3 py-1 text-xs text-gray-600 hover:bg-gray-50"
            >
              일시정지
            </button>
          ) : (
            <button
              onClick={() => onUpdateStatus(projectId, integration.id, 'ACTIVE')}
              className="rounded-md border border-green-300 px-3 py-1 text-xs text-green-600 hover:bg-green-50"
            >
              활성화
            </button>
          )}
          <button
            onClick={() => onDelete(projectId, integration.id)}
            className="rounded-md border border-red-300 px-3 py-1 text-xs text-red-600 hover:bg-red-50"
          >
            삭제
          </button>
        </div>
      )}
    </div>
  );
}

function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
        <h3 className="mb-4 text-lg font-bold text-gray-900">{title}</h3>
        {children}
      </div>
    </div>
  );
}
