import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../api/client';
import type { components } from '@death-star/holocron';

export type Workspace = components['schemas']['Workspace'];
export type WorkspaceCreateRequest = components['schemas']['WorkspaceCreateRequest'];
export type WorkspaceInviteRequest = components['schemas']['WorkspaceInviteRequest'];

export const useWorkspaces = () => {
  return useQuery({
    queryKey: ['workspaces'],
    queryFn: async () => {
      const res = await apiClient.customFetch('/api/v1/workspaces');
      if (!res.ok) throw new Error('Failed to fetch workspaces');
      return (await res.json()) as Workspace[];
    },
  });
};

export const useCreateWorkspace = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (req: WorkspaceCreateRequest) => {
      const res = await apiClient.customFetch('/api/v1/workspaces', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(req),
      });
      if (!res.ok) throw new Error('Failed to create workspace');
      return (await res.json()) as Workspace;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workspaces'] });
    },
  });
};

export const useInviteToWorkspace = (workspaceId: string) => {
  return useMutation({
    mutationFn: async (req: WorkspaceInviteRequest) => {
      const res = await apiClient.customFetch(`/api/v1/workspaces/${workspaceId}/invitations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(req),
      });
      if (!res.ok) throw new Error('Failed to invite user');
    },
  });
};

export const useSetActiveWorkspace = () => {
  return useMutation({
    mutationFn: async (workspaceId: string) => {
      const res = await apiClient.customFetch(`/api/v1/workspaces/${workspaceId}/active`, {
        method: 'POST',
      });
      if (!res.ok) throw new Error('Failed to set active workspace');
    },
  });
};

export const useAcceptInvite = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (token: string) => {
      const res = await apiClient.customFetch(`/api/v1/workspaces/invitations/${token}/accept`, {
        method: 'POST',
      });
      if (!res.ok) throw new Error('Failed to accept invite');
      return (await res.json()) as Workspace;
    },
    onSuccess: () => {
      // Refresh workspaces
      queryClient.invalidateQueries({ queryKey: ['workspaces'] });
    }
  });
};
