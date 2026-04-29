"use client";

import { create } from "zustand";
import type { UserMe } from "@/lib/types";

type State = {
  user: UserMe | null;
  loading: boolean;
  setUser: (u: UserMe | null) => void;
  setLoading: (b: boolean) => void;
};

export const useAuthStore = create<State>((set) => ({
  user: null,
  loading: true,
  setUser: (user) => set({ user, loading: false }),
  setLoading: (loading) => set({ loading }),
}));
