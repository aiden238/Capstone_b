'use client';

import { useEffect } from 'react';
import { useParams } from 'next/navigation';
import { useProjectStore } from '@/store/project';

const ROLE_LABEL: Record<string, string> = {
  LEADER: '팀장',
  MEMBER: '팀원',
  OBSERVER: '옵저버',
};

export default function MembersPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { members, currentProject, fetchMembers } = useProjectStore();

  useEffect(() => {
    if (projectId) fetchMembers(projectId);
  }, [projectId, fetchMembers]);

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-sm font-semibold text-gray-700">
          멤버 ({members.length}명)
        </h2>
        {currentProject && (
          <div className="rounded-md bg-gray-50 px-3 py-1.5">
            <span className="text-xs text-gray-500">초대코드: </span>
            <span className="font-mono text-sm font-bold tracking-widest text-gray-900">
              {currentProject.inviteCode}
            </span>
          </div>
        )}
      </div>

      <div className="rounded-lg border border-gray-200 bg-white">
        <table className="w-full">
          <thead>
            <tr className="border-b border-gray-200 bg-gray-50 text-left text-xs font-medium uppercase text-gray-500">
              <th className="px-4 py-3">이름</th>
              <th className="px-4 py-3">이메일</th>
              <th className="px-4 py-3">역할</th>
              <th className="px-4 py-3">데이터 동의</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {members.map((m) => (
              <tr key={m.userId}>
                <td className="px-4 py-3 text-sm font-medium text-gray-900">{m.name}</td>
                <td className="px-4 py-3 text-sm text-gray-500">{m.email}</td>
                <td className="px-4 py-3">
                  <span
                    className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
                      m.role === 'LEADER'
                        ? 'bg-yellow-100 text-yellow-800'
                        : m.role === 'OBSERVER'
                        ? 'bg-purple-100 text-purple-700'
                        : 'bg-gray-100 text-gray-700'
                    }`}
                  >
                    {ROLE_LABEL[m.role] || m.role}
                  </span>
                </td>
                <td className="px-4 py-3 text-sm">
                  {m.dataConsentAt ? (
                    <span className="text-green-600">✓ 동의완료</span>
                  ) : (
                    <span className="text-gray-400">미동의</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
