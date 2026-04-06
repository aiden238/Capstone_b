'use client';

import { useEffect } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import Link from 'next/link';
import { useAuthStore } from '@/store/auth';

const baseNavItems = [
  { href: '/projects', label: '프로젝트', icon: '📂' },
];

const professorNavItems = [
  { href: '/overview', label: '대시보드', icon: '📊' },
  { href: '/projects', label: '프로젝트', icon: '📂' },
];

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user, loading, fetchMe, logout } = useAuthStore();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    fetchMe();
  }, [fetchMe]);

  useEffect(() => {
    if (!loading && !user) {
      router.replace('/login');
    }
  }, [loading, user, router]);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-gray-500">로딩 중...</div>
      </div>
    );
  }

  if (!user) return null;

  const isProfessorOrTA = user.role === 'PROFESSOR' || user.role === 'TA';
  const navItems = isProfessorOrTA ? professorNavItems : baseNavItems;

  const handleLogout = async () => {
    await logout();
    router.push('/login');
  };

  return (
    <div className="flex min-h-screen bg-gray-50">
      {/* Sidebar */}
      <aside className="flex w-60 flex-col border-r border-gray-200 bg-white">
        <div className="flex h-14 items-center border-b border-gray-200 px-4">
          <Link href="/projects" className="text-lg font-bold text-gray-900">
            Blackbox
          </Link>
        </div>

        <nav className="flex-1 space-y-1 px-2 py-3">
          {navItems.map((item) => {
            const active = pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium ${
                  active
                    ? 'bg-blue-50 text-blue-700'
                    : 'text-gray-700 hover:bg-gray-100'
                }`}
              >
                <span>{item.icon}</span>
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="border-t border-gray-200 p-3">
          <div className="mb-2 text-xs text-gray-500">{user.email}</div>
          <div className="mb-2 text-sm font-medium text-gray-900">{user.name}</div>
          <span className="mb-3 inline-block rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-600">
            {user.role === 'STUDENT' ? '학생' : user.role === 'PROFESSOR' ? '교수' : '조교'}
          </span>
          <button
            onClick={handleLogout}
            className="mt-1 block w-full rounded-md bg-gray-100 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-200"
          >
            로그아웃
          </button>
        </div>
      </aside>

      {/* Main */}
      <main className="flex-1 overflow-auto">
        <div className="mx-auto max-w-7xl px-6 py-6">{children}</div>
      </main>
    </div>
  );
}
