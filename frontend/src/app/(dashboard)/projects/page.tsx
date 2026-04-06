'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useProjectStore, Project } from '@/store/project';

export default function ProjectsPage() {
  const { projects, loading, fetchProjects, createProject, joinProject } = useProjectStore();
  const [showCreate, setShowCreate] = useState(false);
  const [showJoin, setShowJoin] = useState(false);

  useEffect(() => {
    fetchProjects();
  }, [fetchProjects]);

  if (loading && projects.length === 0) {
    return <div className="py-12 text-center text-gray-500">로딩 중...</div>;
  }

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">내 프로젝트</h1>
        <div className="flex gap-2">
          <button
            onClick={() => setShowJoin(true)}
            className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            초대코드로 참여
          </button>
          <button
            onClick={() => setShowCreate(true)}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            + 새 프로젝트
          </button>
        </div>
      </div>

      {projects.length === 0 ? (
        <div className="rounded-lg border-2 border-dashed border-gray-300 p-12 text-center">
          <p className="text-gray-500">아직 프로젝트가 없습니다</p>
          <p className="mt-1 text-sm text-gray-400">새 프로젝트를 만들거나 초대코드로 참여하세요</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((p: Project) => (
            <Link
              key={p.id}
              href={`/projects/${p.id}/board`}
              className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm transition hover:shadow-md"
            >
              <h3 className="text-lg font-semibold text-gray-900">{p.name}</h3>
              {p.description && (
                <p className="mt-1 text-sm text-gray-500 line-clamp-2">{p.description}</p>
              )}
              <div className="mt-3 flex items-center gap-3 text-xs text-gray-400">
                <span>{p.courseName}</span>
                <span>{p.semester}</span>
              </div>
            </Link>
          ))}
        </div>
      )}

      {/* Create Modal */}
      {showCreate && (
        <CreateProjectModal
          onClose={() => setShowCreate(false)}
          onCreate={async (data) => {
            await createProject(data);
            setShowCreate(false);
            fetchProjects();
          }}
        />
      )}

      {/* Join Modal */}
      {showJoin && (
        <JoinProjectModal
          onClose={() => setShowJoin(false)}
          onJoin={async (code) => {
            await joinProject(code);
            setShowJoin(false);
            fetchProjects();
          }}
        />
      )}
    </div>
  );
}

function CreateProjectModal({
  onClose,
  onCreate,
}: {
  onClose: () => void;
  onCreate: (data: { name: string; description?: string; courseName: string; semester: string }) => Promise<void>;
}) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [courseName, setCourseName] = useState('');
  const [semester, setSemester] = useState('2026-1');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await onCreate({ name, description: description || undefined, courseName, semester });
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { error?: { message?: string } } } })
          ?.response?.data?.error?.message || '프로젝트 생성에 실패했습니다',
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={onClose}>
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl" onClick={(e) => e.stopPropagation()}>
        <h2 className="mb-4 text-lg font-bold text-gray-900">새 프로젝트</h2>
        <form onSubmit={handleSubmit} className="space-y-3">
          <input
            type="text"
            required
            placeholder="프로젝트 이름"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <input
            type="text"
            placeholder="설명 (선택)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <input
            type="text"
            required
            placeholder="과목명"
            value={courseName}
            onChange={(e) => setCourseName(e.target.value)}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <input
            type="text"
            required
            placeholder="학기 (예: 2026-1)"
            value={semester}
            onChange={(e) => setSemester(e.target.value)}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
              취소
            </button>
            <button type="submit" disabled={loading} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
              {loading ? '생성 중...' : '생성'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function JoinProjectModal({
  onClose,
  onJoin,
}: {
  onClose: () => void;
  onJoin: (code: string) => Promise<void>;
}) {
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await onJoin(code);
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { error?: { message?: string } } } })
          ?.response?.data?.error?.message || '참여에 실패했습니다',
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={onClose}>
      <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl" onClick={(e) => e.stopPropagation()}>
        <h2 className="mb-4 text-lg font-bold text-gray-900">초대코드로 참여</h2>
        <form onSubmit={handleSubmit} className="space-y-3">
          <input
            type="text"
            required
            placeholder="초대 코드 입력"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm text-center tracking-widest focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
              취소
            </button>
            <button type="submit" disabled={loading} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
              {loading ? '참여 중...' : '참여'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
