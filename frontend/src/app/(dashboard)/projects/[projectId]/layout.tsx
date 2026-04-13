'use client';

import { useEffect, useState } from 'react';
import { useParams, usePathname } from 'next/navigation';
import Link from 'next/link';
import { useProjectStore } from '@/store/project';
import { useAuthStore } from '@/store/auth';
import { useAlertStore } from '@/store/alert';

const tabs = [
  { href: 'board', label: '칸반 보드', icon: '📋' },
  { href: 'meetings', label: '회의록', icon: '📝' },
  { href: 'files', label: '파일', icon: '📁' },
  { href: 'scores', label: '기여도', icon: '📊' },
  { href: 'alerts', label: '경보', icon: '🔔', showBadge: true },
  { href: 'members', label: '멤버', icon: '👥' },
  { href: 'timeline', label: '타임라인', icon: '🕐' },
  { href: 'settings', label: '설정', icon: '⚙️' },
];

export default function ProjectLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { projectId } = useParams<{ projectId: string }>();
  const pathname = usePathname();
  const { currentProject, members, fetchProject, fetchMembers, updateConsent } = useProjectStore();
  const { user } = useAuthStore();
  const { unreadCount, fetchUnreadCount } = useAlertStore();
  const [showConsent, setShowConsent] = useState(false);

  useEffect(() => {
    if (projectId) {
      fetchProject(projectId);
      fetchMembers(projectId);
      fetchUnreadCount(projectId);
    }
  }, [projectId, fetchProject, fetchMembers, fetchUnreadCount]);

  // Check if current user needs consent
  useEffect(() => {
    if (user && members.length > 0) {
      const me = members.find((m) => m.userId === user.id);
      if (me && !me.dataConsentAt && me.role !== 'OBSERVER') {
        setShowConsent(true);
      }
    }
  }, [user, members]);

  const handleConsent = async () => {
    if (!projectId) return;
    await updateConsent(projectId, { consentPlatform: true });
    setShowConsent(false);
  };

  return (
    <div>
      <div className="mb-4">
        <Link href="/projects" className="text-sm text-gray-500 hover:text-gray-700">
          ← 프로젝트 목록
        </Link>
      </div>

      {currentProject && (
        <div className="mb-4">
          <h1 className="text-xl font-bold text-gray-900">{currentProject.name}</h1>
          <p className="text-sm text-gray-500">
            {currentProject.courseName} · {currentProject.semester}
          </p>
        </div>
      )}

      <div className="mb-6 flex gap-1 border-b border-gray-200 overflow-x-auto">
        {tabs.map((tab) => {
          const href = `/projects/${projectId}/${tab.href}`;
          const active = pathname.startsWith(href);
          return (
            <Link
              key={tab.href}
              href={href}
              className={`flex items-center gap-1.5 border-b-2 px-4 py-2.5 text-sm font-medium transition whitespace-nowrap ${
                active
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
              }`}
            >
              <span>{tab.icon}</span>
              {tab.label}
              {tab.showBadge && unreadCount > 0 && (
                <span className="ml-1 inline-flex h-5 min-w-[20px] items-center justify-center rounded-full bg-red-500 px-1.5 text-xs font-bold text-white">
                  {unreadCount > 99 ? '99+' : unreadCount}
                </span>
              )}
            </Link>
          );
        })}
      </div>

      {children}

      {/* Consent Modal */}
      {showConsent && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
            <h2 className="mb-2 text-lg font-bold text-gray-900">📋 데이터 수집 동의</h2>
            <p className="mb-4 text-sm text-gray-600">
              이 프로젝트는 팀 기여도 분석을 위해 플랫폼 내 활동 데이터(태스크, 회의 참석, 파일 업로드 등)를 수집합니다.
              수집된 데이터는 기여도 산출에만 사용됩니다.
            </p>
            <div className="mb-4 rounded-md bg-blue-50 p-3 text-sm text-blue-800">
              <strong>수집 항목:</strong> 태스크 활동, 회의 체크인, 파일 업로드 기록
            </div>
            <div className="flex justify-end gap-2">
              <button
                onClick={() => setShowConsent(false)}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
              >
                나중에
              </button>
              <button
                onClick={handleConsent}
                className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
              >
                동의합니다
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
