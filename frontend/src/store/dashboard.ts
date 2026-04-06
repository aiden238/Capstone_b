import { create } from 'zustand';
import api from '@/lib/api';

export interface ProjectSummary {
  projectId: string;
  projectName: string;
  courseName: string;
  semester: string;
  memberCount: number;
  taskTotal: number;
  taskTodo: number;
  taskInProgress: number;
  taskDone: number;
  scoreAvg: number;
  scoreMin: number;
  scoreMax: number;
  unreadAlertCount: number;
  totalAlertCount: number;
  healthStatus: 'HEALTHY' | 'WARNING' | 'DANGER';
  lastActivityAt: string | null;
}

interface DashboardState {
  summaries: ProjectSummary[];
  loading: boolean;
  fetchOverview: () => Promise<void>;
}

export const useDashboardStore = create<DashboardState>((set) => ({
  summaries: [],
  loading: false,
  fetchOverview: async () => {
    set({ loading: true });
    try {
      const { data } = await api.get('/dashboard/overview');
      set({ summaries: data.data || [] });
    } catch {
      set({ summaries: [] });
    } finally {
      set({ loading: false });
    }
  },
}));
