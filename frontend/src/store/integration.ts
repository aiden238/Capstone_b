'use client';

import { create } from 'zustand';
import api from '@/lib/api';

export interface Integration {
  id: string;
  projectId: string;
  provider: 'GITHUB_APP' | 'GOOGLE_DRIVE';
  externalId: string;
  externalName: string | null;
  syncStatus: 'ACTIVE' | 'PAUSED' | 'ERROR';
  errorMessage: string | null;
  lastSynced: string | null;
  createdAt: string;
}

export interface GitHubMapping {
  id: string;
  userId: string;
  userName: string;
  githubUsername: string;
  githubId: number | null;
}

interface IntegrationState {
  integrations: Integration[];
  mappings: GitHubMapping[];
  loading: boolean;
  fetchIntegrations: (projectId: string) => Promise<void>;
  createIntegration: (projectId: string, data: {
    provider: string;
    externalId: string;
    externalName?: string;
    installationId?: number;
  }) => Promise<void>;
  deleteIntegration: (projectId: string, integrationId: string) => Promise<void>;
  updateStatus: (projectId: string, integrationId: string, status: string) => Promise<void>;
  fetchMappings: (projectId: string) => Promise<void>;
  createMapping: (projectId: string, userId: string, githubUsername: string) => Promise<void>;
  deleteMapping: (projectId: string, mappingId: string) => Promise<void>;
}

export const useIntegrationStore = create<IntegrationState>((set) => ({
  integrations: [],
  mappings: [],
  loading: false,

  fetchIntegrations: async (projectId) => {
    set({ loading: true });
    const { data } = await api.get(`/projects/${projectId}/integrations`);
    set({ integrations: data.data, loading: false });
  },

  createIntegration: async (projectId, body) => {
    const { data } = await api.post(`/projects/${projectId}/integrations`, body);
    set((s) => ({ integrations: [...s.integrations, data.data] }));
  },

  deleteIntegration: async (projectId, integrationId) => {
    await api.delete(`/projects/${projectId}/integrations/${integrationId}`);
    set((s) => ({ integrations: s.integrations.filter((i) => i.id !== integrationId) }));
  },

  updateStatus: async (projectId, integrationId, status) => {
    const { data } = await api.patch(
      `/projects/${projectId}/integrations/${integrationId}/status?status=${status}`,
    );
    set((s) => ({
      integrations: s.integrations.map((i) => (i.id === integrationId ? data.data : i)),
    }));
  },

  fetchMappings: async (projectId) => {
    const { data } = await api.get(`/projects/${projectId}/integrations/github-mappings`);
    set({ mappings: data.data });
  },

  createMapping: async (projectId, userId, githubUsername) => {
    const { data } = await api.post(`/projects/${projectId}/integrations/github-mappings`, {
      userId,
      githubUsername,
    });
    set((s) => ({ mappings: [...s.mappings, data.data] }));
  },

  deleteMapping: async (projectId, mappingId) => {
    await api.delete(`/projects/${projectId}/integrations/github-mappings/${mappingId}`);
    set((s) => ({ mappings: s.mappings.filter((m) => m.id !== mappingId) }));
  },
}));
