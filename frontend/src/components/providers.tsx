"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { fetchMe } from "@/lib/queries";
import { useAuthStore } from "@/store/authStore";

export function Providers({ children }: { children: React.ReactNode }) {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30 * 1000,
            refetchOnWindowFocus: false,
            retry: (failureCount, err) => {
              if (err instanceof ApiError && (err.status === 401 || err.status === 404)) return false;
              return failureCount < 2;
            },
          },
        },
      })
  );
  return <QueryClientProvider client={client}>
    <AuthBootstrap />
    {children}
  </QueryClientProvider>;
}

function AuthBootstrap() {
  const setUser = useAuthStore((s) => s.setUser);
  const setLoading = useAuthStore((s) => s.setLoading);

  useEffect(() => {
    setLoading(true);
    fetchMe()
      .then(setUser)
      .catch(() => setUser(null));
  }, [setUser, setLoading]);

  return null;
}
