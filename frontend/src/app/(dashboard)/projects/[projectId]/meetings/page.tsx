'use client';

import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'next/navigation';
import api from '@/lib/api';
import { useTaskStore } from '@/store/task';

interface Attendee {
  userId: string;
  name: string;
  email: string;
  checkedIn: boolean;
  checkedAt: string | null;
}

interface Meeting {
  id: string;
  title: string;
  meetingDate: string;
  purpose: string | null;
  notes: string | null;
  decisions: string | null;
  checkinCode: string;
  createdBy: string;
  attendees: Attendee[];
  createdAt: string;
}

export default function MeetingsPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Meeting | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [showCheckin, setShowCheckin] = useState<string | null>(null);
  const { createTask, fetchTasks } = useTaskStore();

  const load = useCallback(async () => {
    setLoading(true);
    const { data } = await api.get(`/projects/${projectId}/meetings`);
    setMeetings(data.data);
    setLoading(false);
  }, [projectId]);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (body: { title: string; meetingDate: string; purpose?: string }) => {
    await api.post(`/projects/${projectId}/meetings`, body);
    setShowCreate(false);
    load();
  };

  const handleUpdate = async (meetingId: string, body: { notes?: string; decisions?: string }) => {
    const { data } = await api.patch(`/projects/${projectId}/meetings/${meetingId}`, body);
    setSelected(data.data);
    load();
  };

  const handleCheckin = async (meetingId: string, code: string) => {
    await api.post(`/projects/${projectId}/meetings/${meetingId}/checkin`, { checkinCode: code });
    setShowCheckin(null);
    load();
    // refresh selected if open
    if (selected?.id === meetingId) {
      const { data } = await api.get(`/projects/${projectId}/meetings/${meetingId}`);
      setSelected(data.data);
    }
  };

  const handleDelete = async (meetingId: string) => {
    await api.delete(`/projects/${projectId}/meetings/${meetingId}`);
    if (selected?.id === meetingId) setSelected(null);
    load();
  };

  const handleCreateTask = async (title: string) => {
    if (!projectId) return;
    await createTask(projectId, { title, tag: '회의결정' });
    fetchTasks(projectId);
  };

  if (loading) return <div className="py-12 text-center text-gray-500">로딩 중...</div>;

  return (
    <div className="flex gap-6">
      {/* Meeting List */}
      <div className="w-80 shrink-0">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-gray-700">회의 목록</h2>
          <button
            onClick={() => setShowCreate(true)}
            className="rounded bg-blue-600 px-2.5 py-1 text-xs font-medium text-white hover:bg-blue-700"
          >
            + 새 회의
          </button>
        </div>

        <div className="space-y-2">
          {meetings.length === 0 && (
            <p className="py-8 text-center text-sm text-gray-400">회의가 없습니다</p>
          )}
          {meetings.map((m) => (
            <button
              key={m.id}
              onClick={() => setSelected(m)}
              className={`w-full rounded-lg border p-3 text-left transition ${
                selected?.id === m.id
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-200 bg-white hover:border-gray-300'
              }`}
            >
              <div className="text-sm font-medium text-gray-900">{m.title}</div>
              <div className="mt-1 text-xs text-gray-500">
                {new Date(m.meetingDate).toLocaleDateString('ko-KR', {
                  year: 'numeric', month: 'short', day: 'numeric',
                  hour: '2-digit', minute: '2-digit',
                })}
              </div>
              <div className="mt-1 flex items-center gap-2">
                <span className="text-xs text-gray-400">
                  참석 {m.attendees.filter((a) => a.checkedIn).length}명
                </span>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Meeting Detail */}
      <div className="flex-1">
        {selected ? (
          <MeetingDetail
            meeting={selected}
            onUpdate={(body) => handleUpdate(selected.id, body)}
            onCheckin={() => setShowCheckin(selected.id)}
            onDelete={() => handleDelete(selected.id)}
            onCreateTask={handleCreateTask}
          />
        ) : (
          <div className="flex h-64 items-center justify-center rounded-lg border-2 border-dashed border-gray-200 text-sm text-gray-400">
            회의를 선택하세요
          </div>
        )}
      </div>

      {/* Create Modal */}
      {showCreate && (
        <CreateMeetingModal onClose={() => setShowCreate(false)} onCreate={handleCreate} />
      )}

      {/* Checkin Modal */}
      {showCheckin && (
        <CheckinModal
          meetingId={showCheckin}
          onClose={() => setShowCheckin(null)}
          onCheckin={handleCheckin}
        />
      )}
    </div>
  );
}

function MeetingDetail({
  meeting,
  onUpdate,
  onCheckin,
  onDelete,
  onCreateTask,
}: {
  meeting: Meeting;
  onUpdate: (body: { notes?: string; decisions?: string }) => Promise<void>;
  onCheckin: () => void;
  onDelete: () => void;
  onCreateTask: (title: string) => Promise<void>;
}) {
  const [notes, setNotes] = useState(meeting.notes || '');
  const [decisions, setDecisions] = useState(meeting.decisions || '');
  const [saving, setSaving] = useState(false);
  const [creatingTask, setCreatingTask] = useState(false);

  useEffect(() => {
    setNotes(meeting.notes || '');
    setDecisions(meeting.decisions || '');
  }, [meeting]);

  const save = async () => {
    setSaving(true);
    await onUpdate({ notes, decisions });
    setSaving(false);
  };

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-5">
      <div className="mb-4 flex items-start justify-between">
        <div>
          <h2 className="text-lg font-bold text-gray-900">{meeting.title}</h2>
          <p className="text-sm text-gray-500">
            {new Date(meeting.meetingDate).toLocaleString('ko-KR')}
          </p>
          {meeting.purpose && <p className="mt-1 text-sm text-gray-600">{meeting.purpose}</p>}
        </div>
        <div className="flex gap-2">
          <button onClick={onCheckin} className="rounded-md bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700">
            체크인
          </button>
          <button onClick={onDelete} className="rounded-md bg-red-50 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-100">
            삭제
          </button>
        </div>
      </div>

      {/* Checkin Code */}
      <div className="mb-4 rounded-md bg-gray-50 p-3">
        <span className="text-xs font-medium text-gray-500">체크인 코드: </span>
        <span className="font-mono text-sm font-bold tracking-widest text-gray-900">{meeting.checkinCode}</span>
      </div>

      {/* Attendees */}
      <div className="mb-4">
        <h3 className="mb-2 text-sm font-semibold text-gray-700">참석자</h3>
        {meeting.attendees.length === 0 ? (
          <p className="text-sm text-gray-400">아직 체크인한 참석자가 없습니다</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {meeting.attendees.map((a) => (
              <span
                key={a.userId}
                className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-medium ${
                  a.checkedIn ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                }`}
              >
                {a.checkedIn && '✓ '}{a.name}
              </span>
            ))}
          </div>
        )}
      </div>

      {/* Notes */}
      <div className="mb-3">
        <label className="mb-1 block text-sm font-semibold text-gray-700">회의록</label>
        <textarea
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          rows={5}
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          placeholder="회의 내용을 기록하세요..."
        />
      </div>

      <div className="mb-4">
        <label className="mb-1 block text-sm font-semibold text-gray-700">결정사항</label>
        <textarea
          value={decisions}
          onChange={(e) => setDecisions(e.target.value)}
          rows={3}
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          placeholder="결정사항을 줄 단위로 기록하세요..."
        />
        {decisions.trim() && (
          <button
            onClick={async () => {
              setCreatingTask(true);
              const lines = decisions.split('\n').map((l) => l.replace(/^[-•*]\s*/, '').trim()).filter(Boolean);
              for (const line of lines) {
                await onCreateTask(line);
              }
              setCreatingTask(false);
            }}
            disabled={creatingTask}
            className="mt-1.5 rounded-md bg-purple-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-purple-700 disabled:opacity-50"
          >
            {creatingTask ? '생성 중...' : `📋 결정사항을 태스크로 생성 (${decisions.split('\n').filter((l) => l.trim()).length}건)`}
          </button>
        )}
      </div>

      <button
        onClick={save}
        disabled={saving}
        className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
      >
        {saving ? '저장 중...' : '저장'}
      </button>
    </div>
  );
}

function CreateMeetingModal({
  onClose,
  onCreate,
}: {
  onClose: () => void;
  onCreate: (data: { title: string; meetingDate: string; purpose?: string }) => Promise<void>;
}) {
  const [title, setTitle] = useState('');
  const [meetingDate, setMeetingDate] = useState('');
  const [purpose, setPurpose] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await onCreate({
        title,
        meetingDate: new Date(meetingDate).toISOString(),
        purpose: purpose || undefined,
      });
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { error?: { message?: string } } } })
          ?.response?.data?.error?.message || '회의 생성에 실패했습니다',
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={onClose}>
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl" onClick={(e) => e.stopPropagation()}>
        <h2 className="mb-4 text-lg font-bold text-gray-900">새 회의</h2>
        <form onSubmit={handleSubmit} className="space-y-3">
          <input type="text" required placeholder="회의 제목" value={title} onChange={(e) => setTitle(e.target.value)}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
          <input type="datetime-local" required value={meetingDate} onChange={(e) => setMeetingDate(e.target.value)}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
          <input type="text" placeholder="목적 (선택)" value={purpose} onChange={(e) => setPurpose(e.target.value)}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">취소</button>
            <button type="submit" disabled={loading} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
              {loading ? '생성 중...' : '생성'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function CheckinModal({
  meetingId,
  onClose,
  onCheckin,
}: {
  meetingId: string;
  onClose: () => void;
  onCheckin: (meetingId: string, code: string) => Promise<void>;
}) {
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await onCheckin(meetingId, code);
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { error?: { message?: string } } } })
          ?.response?.data?.error?.message || '체크인에 실패했습니다',
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={onClose}>
      <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl" onClick={(e) => e.stopPropagation()}>
        <h2 className="mb-4 text-lg font-bold text-gray-900">회의 체크인</h2>
        <form onSubmit={handleSubmit} className="space-y-3">
          <input type="text" required placeholder="체크인 코드" value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-center font-mono text-sm tracking-widest focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">취소</button>
            <button type="submit" disabled={loading} className="rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50">
              {loading ? '처리 중...' : '체크인'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
