'use client';

import { create } from 'zustand';
import api from '@/lib/api';

export interface ContributionScore {
  userId: string;
  userName: string;
  email: string;
  taskScore: number;
  meetingScore: number;
  docScore: number;
  gitScore: number;
  totalScore: number;
  calculatedAt: string;
}

export interface WeightConfig {
  id: string | null;
  projectId: string;
  weightGit: number;
  weightDoc: number;
  weightMeeting: number;
  weightTask: number;
  updatedAt: string | null;
}

interface ScoreState {
  scores: ContributionScore[];
  myScore: ContributionScore | null;
  weights: WeightConfig | null;
  loading: boolean;
  fetchScores: (projectId: string) => Promise<void>;
  fetchMyScore: (projectId: string) => Promise<void>;
  recalculate: (projectId: string) => Promise<void>;
  fetchWeights: (projectId: string) => Promise<void>;
  updateWeights: (projectId: string, weights: {
    weightGit: number;
    weightDoc: number;
    weightMeeting: number;
    weightTask: number;
  }) => Promise<void>;
}

export const useScoreStore = create<ScoreState>((set) => ({
  scores: [],
  myScore: null,
  weights: null,
  loading: false,

  fetchScores: async (projectId) => {
    set({ loading: true });
    const { data } = await api.get(`/projects/${projectId}/scores`);
    set({ scores: data.data, loading: false });
  },

  fetchMyScore: async (projectId) => {
    const { data } = await api.get(`/projects/${projectId}/scores/me`);
    set({ myScore: data.data });
  },

  recalculate: async (projectId) => {
    set({ loading: true });
    const { data } = await api.post(`/projects/${projectId}/scores/recalculate`);
    set({ scores: data.data, loading: false });
  },

  fetchWeights: async (projectId) => {
    const { data } = await api.get(`/projects/${projectId}/weights`);
    set({ weights: data.data });
  },

  updateWeights: async (projectId, weights) => {
    const { data } = await api.put(`/projects/${projectId}/weights`, weights);
    set({ weights: data.data });
  },
}));
