import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../../../api/client';
import type { components } from '@death-star/holocron';

type User = components['schemas']['User'];

export const authKeys = {
  all: ['auth'] as const,
  me: () => [...authKeys.all, 'me'] as const,
};

export function useCurrentUser() {
  return useQuery({
    queryKey: authKeys.me(),
    queryFn: async (): Promise<User> => {
      const res = await apiClient.customFetch('/api/v1/auth/me');
      if (!res.ok) throw new Error('Failed to fetch current user');
      return res.json();
    },
    staleTime: Infinity, // The user ID never changes during a session
  });
}
