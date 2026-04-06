'use client';

import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { useTaskStore, Task, TaskStatus, TaskPriority } from '@/store/task';
import { useProjectStore } from '@/store/project';
import {
  DndContext,
  DragEndEvent,
  DragOverlay,
  DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
  closestCenter,
} from '@dnd-kit/core';
import {
  SortableContext,
  verticalListSortingStrategy,
  useSortable,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

const COLUMNS: { key: TaskStatus; label: string; color: string }[] = [
  { key: 'TODO', label: 'To Do', color: 'bg-gray-100' },
  { key: 'IN_PROGRESS', label: 'In Progress', color: 'bg-blue-50' },
  { key: 'DONE', label: 'Done', color: 'bg-green-50' },
];

const PRIORITY_COLORS: Record<TaskPriority, string> = {
  LOW: 'bg-gray-200 text-gray-700',
  MEDIUM: 'bg-blue-100 text-blue-700',
  HIGH: 'bg-orange-100 text-orange-700',
  URGENT: 'bg-red-100 text-red-700',
};

export default function BoardPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { tasks, loading, fetchTasks, updateStatus, createTask, deleteTask } = useTaskStore();
  const { members, fetchMembers } = useProjectStore();
  const [activeTask, setActiveTask] = useState<Task | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [filterAssignee, setFilterAssignee] = useState<string>('');
  const [filterPriority, setFilterPriority] = useState<string>('');
  const [filterTag, setFilterTag] = useState<string>('');

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

  const load = useCallback(() => {
    if (projectId) {
      fetchTasks(projectId);
      fetchMembers(projectId);
    }
  }, [projectId, fetchTasks, fetchMembers]);

  useEffect(() => { load(); }, [load]);

  // Get unique tags from tasks
  const allTags = Array.from(new Set(tasks.map((t) => t.tag).filter((t): t is string => t !== null)));

  // Filter tasks
  const filteredTasks = tasks.filter((t) => {
    if (filterAssignee && !t.assignees.some((a) => a.userId === filterAssignee)) return false;
    if (filterPriority && t.priority !== filterPriority) return false;
    if (filterTag && t.tag !== filterTag) return false;
    return true;
  });

  const hasFilters = filterAssignee || filterPriority || filterTag;

  const tasksByStatus = (status: TaskStatus) =>
    filteredTasks.filter((t) => t.status === status);

  const handleDragStart = (event: DragStartEvent) => {
    const task = tasks.find((t) => t.id === event.active.id);
    if (task) setActiveTask(task);
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    setActiveTask(null);
    const { active, over } = event;
    if (!over) return;

    const taskId = active.id as string;
    const task = tasks.find((t) => t.id === taskId);
    if (!task) return;

    // Determine target column
    let targetStatus: TaskStatus | null = null;
    for (const col of COLUMNS) {
      if (over.id === col.key || tasksByStatus(col.key).some((t) => t.id === over.id)) {
        targetStatus = col.key;
        break;
      }
    }

    if (targetStatus && targetStatus !== task.status) {
      await updateStatus(projectId, taskId, targetStatus);
      fetchTasks(projectId);
    }
  };

  if (loading && tasks.length === 0) {
    return <div className="py-12 text-center text-gray-500">로딩 중...</div>;
  }

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <span className="text-sm text-gray-500">
          총 {tasks.length}개 태스크{hasFilters ? ` (필터: ${filteredTasks.length}개)` : ''}
        </span>
        <button
          onClick={() => setShowCreate(true)}
          className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700"
        >
          + 태스크 추가
        </button>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <select
          value={filterAssignee}
          onChange={(e) => setFilterAssignee(e.target.value)}
          className="rounded-md border border-gray-300 px-2.5 py-1.5 text-xs text-gray-700 focus:border-blue-500 focus:outline-none"
        >
          <option value="">전체 담당자</option>
          {members.filter((m) => m.role !== 'OBSERVER').map((m) => (
            <option key={m.userId} value={m.userId}>{m.name}</option>
          ))}
        </select>
        <select
          value={filterPriority}
          onChange={(e) => setFilterPriority(e.target.value)}
          className="rounded-md border border-gray-300 px-2.5 py-1.5 text-xs text-gray-700 focus:border-blue-500 focus:outline-none"
        >
          <option value="">전체 우선순위</option>
          <option value="URGENT">URGENT</option>
          <option value="HIGH">HIGH</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="LOW">LOW</option>
        </select>
        {allTags.length > 0 && (
          <select
            value={filterTag}
            onChange={(e) => setFilterTag(e.target.value)}
            className="rounded-md border border-gray-300 px-2.5 py-1.5 text-xs text-gray-700 focus:border-blue-500 focus:outline-none"
          >
            <option value="">전체 태그</option>
            {allTags.map((tag) => (
              <option key={tag} value={tag}>{tag}</option>
            ))}
          </select>
        )}
        {hasFilters && (
          <button
            onClick={() => { setFilterAssignee(''); setFilterPriority(''); setFilterTag(''); }}
            className="rounded-md px-2 py-1.5 text-xs text-red-600 hover:bg-red-50"
          >
            필터 초기화
          </button>
        )}
      </div>

      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
      >
        <div className="grid grid-cols-3 gap-4">
          {COLUMNS.map((col) => (
            <Column
              key={col.key}
              column={col}
              tasks={tasksByStatus(col.key)}
              onDelete={async (id) => {
                await deleteTask(projectId, id);
                fetchTasks(projectId);
              }}
            />
          ))}
        </div>

        <DragOverlay>
          {activeTask ? <TaskCardOverlay task={activeTask} /> : null}
        </DragOverlay>
      </DndContext>

      {showCreate && (
        <CreateTaskModal
          members={members}
          onClose={() => setShowCreate(false)}
          onCreate={async (data) => {
            await createTask(projectId, data);
            setShowCreate(false);
            fetchTasks(projectId);
          }}
        />
      )}
    </div>
  );
}

function Column({
  column,
  tasks,
  onDelete,
}: {
  column: { key: TaskStatus; label: string; color: string };
  tasks: Task[];
  onDelete: (id: string) => void;
}) {
  return (
    <SortableContext
      id={column.key}
      items={tasks.map((t) => t.id)}
      strategy={verticalListSortingStrategy}
    >
      <div className={`rounded-lg ${column.color} p-3`}>
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold text-gray-700">{column.label}</h3>
          <span className="rounded-full bg-white px-2 py-0.5 text-xs font-medium text-gray-500">
            {tasks.length}
          </span>
        </div>
        <div className="space-y-2" style={{ minHeight: 60 }}>
          {tasks.map((task) => (
            <SortableTaskCard key={task.id} task={task} onDelete={() => onDelete(task.id)} />
          ))}
          {tasks.length === 0 && (
            <DroppableArea id={column.key} />
          )}
        </div>
      </div>
    </SortableContext>
  );
}

function DroppableArea({ id }: { id: string }) {
  const { setNodeRef } = useSortable({ id });
  return (
    <div
      ref={setNodeRef}
      className="flex h-16 items-center justify-center rounded-md border-2 border-dashed border-gray-300 text-xs text-gray-400"
    >
      여기에 드래그
    </div>
  );
}

function SortableTaskCard({ task, onDelete }: { task: Task; onDelete: () => void }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: task.id,
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.4 : 1,
  };

  return (
    <div ref={setNodeRef} style={style} {...attributes} {...listeners}>
      <TaskCard task={task} onDelete={onDelete} />
    </div>
  );
}

function TaskCard({ task, onDelete }: { task: Task; onDelete: () => void }) {
  return (
    <div className="group rounded-md border border-gray-200 bg-white p-3 shadow-sm">
      <div className="flex items-start justify-between">
        <h4 className="text-sm font-medium text-gray-900">{task.title}</h4>
        <button
          onClick={(e) => { e.stopPropagation(); onDelete(); }}
          className="ml-2 hidden text-gray-400 hover:text-red-500 group-hover:block"
          title="삭제"
        >
          ×
        </button>
      </div>
      {task.description && (
        <p className="mt-1 text-xs text-gray-500 line-clamp-2">{task.description}</p>
      )}
      <div className="mt-2 flex flex-wrap items-center gap-1.5">
        <span className={`rounded px-1.5 py-0.5 text-xs font-medium ${PRIORITY_COLORS[task.priority]}`}>
          {task.priority}
        </span>
        {task.tag && (
          <span className="rounded bg-purple-100 px-1.5 py-0.5 text-xs text-purple-700">
            {task.tag}
          </span>
        )}
        {task.dueDate && (
          <span className="text-xs text-gray-400">
            ~{task.dueDate}
          </span>
        )}
      </div>
      {task.assignees.length > 0 && (
        <div className="mt-2 flex items-center gap-1">
          {task.assignees.map((a) => (
            <span
              key={a.userId}
              className="inline-flex h-6 w-6 items-center justify-center rounded-full bg-blue-100 text-xs font-medium text-blue-700"
              title={a.name}
            >
              {a.name[0]}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}

function TaskCardOverlay({ task }: { task: Task }) {
  return (
    <div className="rounded-md border border-blue-300 bg-white p-3 shadow-lg">
      <h4 className="text-sm font-medium text-gray-900">{task.title}</h4>
    </div>
  );
}

function CreateTaskModal({
  members,
  onClose,
  onCreate,
}: {
  members: { userId: string; name: string }[];
  onClose: () => void;
  onCreate: (data: {
    title: string;
    description?: string;
    priority?: TaskPriority;
    tag?: string;
    dueDate?: string;
    assigneeIds?: string[];
  }) => Promise<void>;
}) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM');
  const [tag, setTag] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [selectedMembers, setSelectedMembers] = useState<string[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const toggleMember = (id: string) => {
    setSelectedMembers((prev) =>
      prev.includes(id) ? prev.filter((m) => m !== id) : [...prev, id],
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await onCreate({
        title,
        description: description || undefined,
        priority,
        tag: tag || undefined,
        dueDate: dueDate || undefined,
        assigneeIds: selectedMembers.length > 0 ? selectedMembers : undefined,
      });
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { error?: { message?: string } } } })
          ?.response?.data?.error?.message || '태스크 생성에 실패했습니다',
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={onClose}>
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl" onClick={(e) => e.stopPropagation()}>
        <h2 className="mb-4 text-lg font-bold text-gray-900">새 태스크</h2>
        <form onSubmit={handleSubmit} className="space-y-3">
          <input
            type="text"
            required
            placeholder="태스크 제목"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <textarea
            placeholder="설명 (선택)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={2}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <div className="grid grid-cols-2 gap-3">
            <select
              value={priority}
              onChange={(e) => setPriority(e.target.value as TaskPriority)}
              className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            >
              <option value="LOW">낮음</option>
              <option value="MEDIUM">보통</option>
              <option value="HIGH">높음</option>
              <option value="URGENT">긴급</option>
            </select>
            <input
              type="text"
              placeholder="태그"
              value={tag}
              onChange={(e) => setTag(e.target.value)}
              className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>
          <input
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          {members.length > 0 && (
            <div>
              <p className="mb-1 text-xs font-medium text-gray-600">담당자</p>
              <div className="flex flex-wrap gap-2">
                {members.map((m) => (
                  <button
                    key={m.userId}
                    type="button"
                    onClick={() => toggleMember(m.userId)}
                    className={`rounded-full px-3 py-1 text-xs font-medium transition ${
                      selectedMembers.includes(m.userId)
                        ? 'bg-blue-600 text-white'
                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                  >
                    {m.name}
                  </button>
                ))}
              </div>
            </div>
          )}
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
