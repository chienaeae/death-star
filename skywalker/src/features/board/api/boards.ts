import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type { components } from '@death-star/holocron';
import { customFetch } from '../../../api/client';

type Board = components['schemas']['Board'];
type BoardCreateRequest = components['schemas']['BoardCreateRequest'];
type BoardColumnCreateRequest = components['schemas']['BoardColumnCreateRequest'];
type BoardColumn = components['schemas']['BoardColumn'];
type BoardTaskCreateRequest = components['schemas']['BoardTaskCreateRequest'];
type MoveTaskRequest = components['schemas']['MoveTaskRequest'];
type BoardTaskUpdateRequest = components['schemas']['BoardTaskUpdateRequest'];

const GATEWAY_PREFIX = '/api/v1';

export const boardKeys = {
  all: ['boards'] as const,
  lists: () => [...boardKeys.all, 'list'] as const,
  details: () => [...boardKeys.all, 'detail'] as const,
  detail: (id: string) => [...boardKeys.details(), id] as const,
};

export const fetchBoards = async (): Promise<Board[]> => {
  const response = await customFetch(`${GATEWAY_PREFIX}/boards`);
  if (!response.ok) {
    throw new Error('Failed to fetch boards');
  }
  return response.json();
};

export const createBoard = async (request: BoardCreateRequest): Promise<Board> => {
  const response = await customFetch(`${GATEWAY_PREFIX}/boards`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });
  
  if (!response.ok) {
    throw new Error('Failed to create board');
  }
  return response.json();
};

export const fetchBoard = async (boardId: string): Promise<Board> => {
  const response = await customFetch(`${GATEWAY_PREFIX}/boards/${boardId}`);
  if (!response.ok) {
    throw new Error('Failed to fetch board details');
  }
  return response.json();
};

export const createColumn = async (boardId: string, request: BoardColumnCreateRequest): Promise<BoardColumn> => {
  const response = await customFetch(`${GATEWAY_PREFIX}/boards/${boardId}/columns`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  if (!response.ok) throw new Error('Failed to create column');
  return response.json();
};

export const createTask = async (boardId: string, request: BoardTaskCreateRequest): Promise<string> => {
  const response = await customFetch(`${GATEWAY_PREFIX}/boards/${boardId}/tasks`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  if (!response.ok) throw new Error('Failed to create task');
  return response.text(); // Returns literal UUID string
};

export const moveTask = async (params: { boardId: string, taskId: string, request: MoveTaskRequest }): Promise<void> => {
  const response = await customFetch(`${GATEWAY_PREFIX}/boards/${params.boardId}/tasks/${params.taskId}/move`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params.request),
  });
  if (!response.ok) throw new Error('Failed to move task due to conflict or error');
};

export const updateTask = async (params: { boardId: string, taskId: string, request: BoardTaskUpdateRequest }): Promise<void> => {
  const response = await customFetch(`${GATEWAY_PREFIX}/boards/${params.boardId}/tasks/${params.taskId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params.request),
  });
  if (!response.ok) throw new Error('Failed to update task');
};

// --- React Query Hooks ---

export const useBoards = () => {
  return useQuery({
    queryKey: boardKeys.lists(),
    queryFn: fetchBoards,
  });
};

export const useCreateBoard = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createBoard,
    onSuccess: () => {
      // Invalidate and refetch boards list
      queryClient.invalidateQueries({ queryKey: boardKeys.lists() });
    },
  });
};

export const useBoard = (boardId: string) => {
  return useQuery({
    queryKey: boardKeys.detail(boardId),
    queryFn: () => fetchBoard(boardId),
    enabled: !!boardId,
  });
};

export const useCreateColumn = (boardId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: BoardColumnCreateRequest) => createColumn(boardId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: boardKeys.detail(boardId) });
    },
  });
};

export const useCreateTask = (boardId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: BoardTaskCreateRequest) => createTask(boardId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: boardKeys.detail(boardId) });
    },
  });
};

export const useMoveTask = (boardId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (args: { taskId: string, request: MoveTaskRequest }) => 
      moveTask({ boardId, taskId: args.taskId, request: args.request }),
    onSettled: () => {
      // Regardless of success or optimistic rollback, refetch to guarantee true server state
      queryClient.invalidateQueries({ queryKey: boardKeys.detail(boardId) });
    },
  });
};

export const useUpdateTask = (boardId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (args: { taskId: string, request: BoardTaskUpdateRequest }) =>
      updateTask({ boardId, taskId: args.taskId, request: args.request }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: boardKeys.detail(boardId) });
    },
  });
};
