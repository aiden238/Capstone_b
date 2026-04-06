'use client';

import { create } from 'zustand';
import api from '@/lib/api';

export type AlertType = 'CRUNCH_TIME' | 'FREE_RIDE' | 'DROPOUT' | 'OVERLOAD' | 'TAMPER' | 'GAMING_SUSPECT';
export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Alert {
  id: string;
  projectId: string;
  userId: string | null;
  alertType: AlertType;
  severity: AlertSeverity;
  message: string;
  isRead: boolean;
  createdAt: string;
}

interface AlertState {
  alerts: Alert[];
  unreadCount: number;
  loading: boolean;
  fetchAlerts: (projectId: string) => Promise<void>;
  fetchUnread: (projectId: string) => Promise<void>;
  fetchUnreadCount: (projectId: string) => Promise<void>;
  markAsRead: (projectId: string, alertId: string) => Promise<void>;
  markAllAsRead: (projectId: string) => Promise<void>;
  runDetection: (projectId: string) => Promise<void>;
}

export const useAlertStore = create<AlertState>((set, get) => ({
  alerts: [],
  unreadCount: 0,
  loading: false,

  fetchAlerts: async (projectId) => {
    set({ loading: true });
    const { data } = await api.get(`/projects/${projectId}/alerts`);
    set({ alerts: data.data, loading: false });
  },

  fetchUnread: async (projectId) => {
    const { data } = await api.get(`/projects/${projectId}/alerts/unread`);
    set({ alerts: data.data });
  },

  fetchUnreadCount: async (projectId) => {
    const { data } = await api.get(`/projects/${projectId}/alerts/unread/count`);
    set({ unreadCount: data.data.count });
  },

  markAsRead: async (projectId, alertId) => {
    await api.patch(`/projects/${projectId}/alerts/${alertId}/read`);
    set({
      alerts: get().alerts.map((a) =>
        a.id === alertId ? { ...a, isRead: true } : a
      ),
      unreadCount: Math.max(0, get().unreadCount - 1),
    });
  },

  markAllAsRead: async (projectId) => {
    await api.patch(`/projects/${projectId}/alerts/read-all`);
    set({
      alerts: get().alerts.map((a) => ({ ...a, isRead: true })),
      unreadCount: 0,
    });
  },

  runDetection: async (projectId) => {
    await api.post(`/projects/${projectId}/alerts/detect`);
    // Re-fetch all alerts after detection
    const { data } = await api.get(`/projects/${projectId}/alerts`);
    const countRes = await api.get(`/projects/${projectId}/alerts/unread/count`);
    set({ alerts: data.data, unreadCount: countRes.data.data.count });
  },
}));
