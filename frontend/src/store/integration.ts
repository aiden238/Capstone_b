'use client';

import { create } from 'zustand';
import api from '@/lib/api';

export interface GithubIntegration {
  id: string;
  projectId: string;
  installationId: number;
  repoFullName: string;
  createdAt: string;
}

export interface DriveIntegration {
  id: string;
  projectId: string;
  driveFolderId: string;
  watchChannelId: string;
  watchExpiry: string | null;
  createdAt: string;
}

interface IntegrationState {
  github: GithubIntegration | null;
  drive: DriveIntegration | null;
  loading: boolean;
  fetchGithub: (projectId: string) => Promise<void>;
  connectGithub: (projectId: string, installationId: number, repoFullName: string) => Promise<void>;
  disconnectGithub: (projectId: string) => Promise<void>;
  fetchDrive: (projectId: string) => Promise<void>;
  disconnectDrive: (projectId: string) => Promise<void>;
}

export const useIntegrationStore = create<IntegrationState>((set) => ({
  github: null,
  drive: null,
  loading: false,

  fetchGithub: async (projectId) => {
    try {
      const { data } = await api.get(`/projects/${projectId}/integrations/github`);
      set({ github: data.data });
    } catch {
      set({ github: null });
    }
  },

  connectGithub: async (projectId, installationId, repoFullName) => {
    const { data } = await api.post(`/projects/${projectId}/integrations/github`, {
      installationId,
      repoFullName,
    });
    set({ github: data.data });
  },

  disconnectGithub: async (projectId) => {
    await api.delete(`/projects/${projectId}/integrations/github`);
    set({ github: null });
  },

  fetchDrive: async (projectId) => {
    try {
      const { data } = await api.get(`/projects/${projectId}/integrations/drive`);
      set({ drive: data.data });
    } catch {
      set({ drive: null });
    }
  },

  disconnectDrive: async (projectId) => {
    await api.delete(`/projects/${projectId}/integrations/drive`);
    set({ drive: null });
  },
}));
