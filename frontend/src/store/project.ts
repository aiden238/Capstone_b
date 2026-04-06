import { create } from 'zustand';
import api from '@/lib/api';

export type ProjectRole = 'LEADER' | 'MEMBER' | 'OBSERVER';

export interface ProjectMember {
  userId: string;
  name: string;
  email: string;
  role: ProjectRole;
  dataConsentAt: string | null;
}

export interface Project {
  id: string;
  name: string;
  description: string;
  courseName: string;
  semester: string;
  inviteCode: string;
  createdAt: string;
}

interface ProjectState {
  projects: Project[];
  currentProject: Project | null;
  members: ProjectMember[];
  loading: boolean;
  fetchProjects: () => Promise<void>;
  fetchProject: (id: string) => Promise<void>;
  fetchMembers: (projectId: string) => Promise<void>;
  createProject: (data: { name: string; description?: string; courseName: string; semester: string }) => Promise<Project>;
  joinProject: (inviteCode: string) => Promise<void>;
  updateConsent: (projectId: string, consent: ConsentData) => Promise<void>;
}

export interface ConsentData {
  consentPlatform?: boolean;
  consentGithub?: boolean;
  consentDrive?: boolean;
  consentAiAnalysis?: boolean;
}

export const useProjectStore = create<ProjectState>((set) => ({
  projects: [],
  currentProject: null,
  members: [],
  loading: false,

  fetchProjects: async () => {
    set({ loading: true });
    const { data } = await api.get('/projects');
    set({ projects: data.data, loading: false });
  },

  fetchProject: async (id) => {
    const { data } = await api.get(`/projects/${id}`);
    set({ currentProject: data.data });
  },

  fetchMembers: async (projectId) => {
    const { data } = await api.get(`/projects/${projectId}/members`);
    set({ members: data.data });
  },

  createProject: async (body) => {
    const { data } = await api.post('/projects', body);
    return data.data;
  },

  joinProject: async (inviteCode) => {
    await api.post('/projects/join', { inviteCode });
  },

  updateConsent: async (projectId, consent) => {
    await api.patch(`/projects/${projectId}/members/me/consent`, consent);
    // refresh members to reflect updated consent
    const { data } = await api.get(`/projects/${projectId}/members`);
    set({ members: data.data });
  },
}));
