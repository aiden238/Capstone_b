'use client';

import { create } from 'zustand';
import api from '@/lib/api';

export interface TimelineEntry {
  id: string;
  projectId: string;
  userId: string;
  userName: string;
  source: 'PLATFORM' | 'GITHUB' | 'GOOGLE_DRIVE';
  actionType: string;
  metadata: string | null;
  externalId: string | null;
  trustLevel: number;
  occurredAt: string;
}

interface TimelineState {
  entries: TimelineEntry[];
  loading: boolean;
  hasMore: boolean;
  page: number;
  fetchTimeline: (projectId: string, params?: {
    source?: string;
    userId?: string;
    page?: number;
    size?: number;
  }) => Promise<void>;
  loadMore: (projectId: string, params?: {
    source?: string;
    userId?: string;
  }) => Promise<void>;
  reset: () => void;
}

export const useTimelineStore = create<TimelineState>((set, get) => ({
  entries: [],
  loading: false,
  hasMore: true,
  page: 0,

  fetchTimeline: async (projectId, params = {}) => {
    set({ loading: true, page: 0 });
    const queryParams = new URLSearchParams();
    if (params.source) queryParams.set('source', params.source);
    if (params.userId) queryParams.set('userId', params.userId);
    queryParams.set('page', '0');
    queryParams.set('size', String(params.size || 50));

    const { data } = await api.get(`/projects/${projectId}/timeline?${queryParams}`);
    const entries: TimelineEntry[] = data.data;
    set({
      entries,
      loading: false,
      hasMore: entries.length >= (params.size || 50),
      page: 0,
    });
  },

  loadMore: async (projectId, params = {}) => {
    const { page, hasMore, loading } = get();
    if (!hasMore || loading) return;

    const nextPage = page + 1;
    set({ loading: true });
    const queryParams = new URLSearchParams();
    if (params.source) queryParams.set('source', params.source);
    if (params.userId) queryParams.set('userId', params.userId);
    queryParams.set('page', String(nextPage));
    queryParams.set('size', '50');

    const { data } = await api.get(`/projects/${projectId}/timeline?${queryParams}`);
    const newEntries: TimelineEntry[] = data.data;
    set((s) => ({
      entries: [...s.entries, ...newEntries],
      loading: false,
      hasMore: newEntries.length >= 50,
      page: nextPage,
    }));
  },

  reset: () => set({ entries: [], loading: false, hasMore: true, page: 0 }),
}));
