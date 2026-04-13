'use client';

import { create } from 'zustand';
import api from '@/lib/api';

export type ActivitySource = 'PLATFORM' | 'GITHUB' | 'GOOGLE_DRIVE' | 'MANUAL';
export type ActionType =
  | 'TASK_CREATE' | 'TASK_UPDATE' | 'TASK_STATUS_CHANGE' | 'TASK_COMPLETE' | 'TASK_DELETE' | 'TASK_ASSIGN' | 'TASK_UNASSIGN'
  | 'MEETING_CREATE' | 'MEETING_UPDATE' | 'MEETING_DELETE' | 'MEETING_CHECKIN'
  | 'FILE_UPLOAD'
  | 'COMMIT' | 'PR_OPEN' | 'PR_MERGE' | 'ISSUE_CREATE' | 'ISSUE_CLOSE' | 'CODE_REVIEW'
  | 'DOC_EDIT' | 'DOC_CREATE' | 'DOC_COMMENT';

export interface ActivityLogItem {
  id: string;
  projectId: string;
  userId: string;
  source: ActivitySource;
  actionType: ActionType;
  metadata: Record<string, string> | null;
  externalId: string | null;
  trustLevel: number;
  occurredAt: string;
  qualityScore: number | null;
  qualityReason: string | null;
  analysisMethod: string | null;
}

export interface ActivityPage {
  content: ActivityLogItem[];
  totalPages: number;
  totalElements: number;
  number: number;
}

interface ActivityState {
  activities: ActivityLogItem[];
  totalPages: number;
  currentPage: number;
  loading: boolean;
  fetchActivities: (projectId: string, page?: number) => Promise<void>;
}

export const useActivityStore = create<ActivityState>((set) => ({
  activities: [],
  totalPages: 0,
  currentPage: 0,
  loading: false,

  fetchActivities: async (projectId, page = 0) => {
    set({ loading: true });
    const { data } = await api.get(`/projects/${projectId}/activities?page=${page}&size=30`);
    const pageData: ActivityPage = data.data;
    set({
      activities: pageData.content,
      totalPages: pageData.totalPages,
      currentPage: pageData.number,
      loading: false,
    });
  },
}));
