import { create } from 'zustand';
import api from '@/lib/api';

export type Role = 'STUDENT' | 'PROFESSOR' | 'TA';

export interface User {
  id: string;
  email: string;
  name: string;
  role: Role;
}

interface AuthState {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  signup: (email: string, password: string, name: string, role: Role) => Promise<void>;
  logout: () => Promise<void>;
  fetchMe: () => Promise<void>;
  reset: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  loading: true,

  login: async (email, password) => {
    const { data } = await api.post('/auth/login', { email, password });
    localStorage.setItem('accessToken', data.data.accessToken);
    localStorage.setItem('refreshToken', data.data.refreshToken);
    const me = await api.get('/auth/me');
    set({ user: me.data.data });
  },

  signup: async (email, password, name, role) => {
    await api.post('/auth/signup', { email, password, name, role });
  },

  logout: async () => {
    try {
      await api.post('/auth/logout');
    } catch { /* ignore */ }
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    set({ user: null });
  },

  fetchMe: async () => {
    try {
      const token = localStorage.getItem('accessToken');
      if (!token) {
        set({ user: null, loading: false });
        return;
      }
      const { data } = await api.get('/auth/me');
      set({ user: data.data, loading: false });
    } catch {
      set({ user: null, loading: false });
    }
  },

  reset: () => set({ user: null, loading: false }),
}));
