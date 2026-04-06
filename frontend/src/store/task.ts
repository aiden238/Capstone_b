import { create } from 'zustand';
import api from '@/lib/api';

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface TaskAssignee {
  userId: string;
  name: string;
  email: string;
}

export interface Task {
  id: string;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  tag: string | null;
  dueDate: string | null;
  completedAt: string | null;
  assignees: TaskAssignee[];
  createdAt: string;
}

interface TaskState {
  tasks: Task[];
  loading: boolean;
  fetchTasks: (projectId: string, status?: TaskStatus) => Promise<void>;
  createTask: (projectId: string, data: {
    title: string;
    description?: string;
    priority?: TaskPriority;
    tag?: string;
    dueDate?: string;
    assigneeIds?: string[];
  }) => Promise<Task>;
  updateTask: (projectId: string, taskId: string, data: Partial<{
    title: string;
    description: string;
    priority: TaskPriority;
    tag: string;
    dueDate: string;
  }>) => Promise<void>;
  updateStatus: (projectId: string, taskId: string, status: TaskStatus) => Promise<void>;
  deleteTask: (projectId: string, taskId: string) => Promise<void>;
}

export const useTaskStore = create<TaskState>((set) => ({
  tasks: [],
  loading: false,

  fetchTasks: async (projectId, status) => {
    set({ loading: true });
    const params = status ? `?status=${status}` : '';
    const { data } = await api.get(`/projects/${projectId}/tasks${params}`);
    set({ tasks: data.data, loading: false });
  },

  createTask: async (projectId, body) => {
    const { data } = await api.post(`/projects/${projectId}/tasks`, body);
    return data.data;
  },

  updateTask: async (projectId, taskId, body) => {
    await api.patch(`/projects/${projectId}/tasks/${taskId}`, body);
  },

  updateStatus: async (projectId, taskId, status) => {
    await api.patch(`/projects/${projectId}/tasks/${taskId}/status`, { status });
  },

  deleteTask: async (projectId, taskId) => {
    await api.delete(`/projects/${projectId}/tasks/${taskId}`);
  },
}));
