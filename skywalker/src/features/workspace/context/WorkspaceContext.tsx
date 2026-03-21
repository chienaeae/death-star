import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../api/client';
import { boardKeys } from '../../board/api/boards';
import { useSetActiveWorkspace } from '../api/workspaces';

interface WorkspaceContextType {
  activeWorkspaceId: string | null;
  setActiveWorkspaceId: (id: string | null) => void;
}

const WorkspaceContext = createContext<WorkspaceContextType | undefined>(undefined);

export function WorkspaceProvider({ children }: { children: React.ReactNode }) {
  const queryClient = useQueryClient();
  const [activeWorkspaceId, setActiveWorkspaceIdState] = useState<string | null>(null);
  const { mutate: setActiveWorkspaceOnServer } = useSetActiveWorkspace();

  const setActiveWorkspaceId = useCallback((id: string | null) => {
    setActiveWorkspaceIdState(id);
    apiClient.setWorkspaceId(id); // Synchronous update!
    if (id) {
      setActiveWorkspaceOnServer(id, {
        onError: (err: Error) => {
          console.error('Failed to sync active workspace to server', err);
        }
      });
    }
    // Invalidate boards so they refetch immediately under the new workspace
    queryClient.invalidateQueries({ queryKey: boardKeys.all });
  }, [queryClient, setActiveWorkspaceOnServer]);

  useEffect(() => {
    // Initial sync on mount
    apiClient.setWorkspaceId(activeWorkspaceId);
  }, []);

  return (
    <WorkspaceContext.Provider value={{ activeWorkspaceId, setActiveWorkspaceId }}>
      {children}
    </WorkspaceContext.Provider>
  );
}

export function useWorkspaceContext() {
  const context = useContext(WorkspaceContext);
  if (context === undefined) {
    throw new Error('useWorkspaceContext must be used within a WorkspaceProvider');
  }
  return context;
}
