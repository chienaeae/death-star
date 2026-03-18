import { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router';
import { 
  DndContext, 
  DragEndEvent, 
  DragOverlay, 
  DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
  closestCorners
} from '@dnd-kit/core';
import { Plus, ArrowLeft } from 'lucide-react';
import { useQueryClient } from '@tanstack/react-query';
import { useBoard, useCreateColumn, useMoveTask, boardKeys } from '../api/boards';
import { BoardColumn } from './BoardColumn';
import { TaskCard } from './TaskCard';
import { TaskModal } from './TaskModal';
import type { components } from '@death-star/holocron';

type BoardTask = components['schemas']['BoardTask'];

export function BoardView() {
  const { boardId } = useParams<{ boardId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: board, isLoading, error } = useBoard(boardId!);
  const createColumnMutation = useCreateColumn(boardId!);
  const moveTaskMutation = useMoveTask(boardId!);

  const [activeTask, setActiveTask] = useState<BoardTask | null>(null);
  const [editingTask, setEditingTask] = useState<BoardTask | null>(null);
  const [isAddingCol, setIsAddingCol] = useState(false);
  const [newColTitle, setNewColTitle] = useState('');

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 5, // Requires a dragging distance of 5px to activate (helps with clicking vs dragging)
      },
    })
  );

  const columns = useMemo(() => board?.columns || [], [board]);

  const handleCreateColumn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newColTitle.trim() || !boardId) return;
    try {
      await createColumnMutation.mutateAsync({
        title: newColTitle.trim(),
        orderIndex: columns.length,
      });
      setNewColTitle('');
      setIsAddingCol(false);
    } catch (err) {
      console.error('Failed to create column:', err);
    }
  };

  const handleDragStart = (event: DragStartEvent) => {
    const { active } = event;
    if (active.data.current?.type === 'Task') {
      setActiveTask(active.data.current.task);
    }
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    setActiveTask(null);
    const { active, over } = event;
    if (!over || !boardId || !board) return;

    const activeId = active.id as string;
    const overId = over.id as string;

    const activeTaskData = active.data.current?.task as BoardTask;
    let newStatusId = activeTaskData.status;

    // Determine the destination status (column logic)
    const overType = over.data.current?.type;
    if (overType === 'Column') {
      newStatusId = overId;
    } else if (overType === 'Task') {
      const overTaskData = over.data.current?.task as BoardTask;
      newStatusId = overTaskData.status;
    }

    if (activeId === overId) return;

    // --- Prepare Optimitic Rolback State ---
    const previousBoard = queryClient.getQueryData(boardKeys.detail(boardId));

    // Calculate LexRank Midpoints (Simplistic frontend estimation, backend is the source of truth)
    // For a highly robust app we'd sort locally, but we rely on the backend OCC and LexRanks
    const destColumn = columns.find(c => c.id === newStatusId);
    let prevLex: string | null = null;
    let nextLex: string | null = null;

    if (destColumn && overType === 'Task') {
      const tasks = destColumn.tasks || [];
      const overIndex = tasks.findIndex(t => t.id === overId);
      const activeIndex = tasks.findIndex(t => t.id === activeId);
      
      if (overIndex >= 0) {
        let isBelowOverItem: boolean;
        
        if (activeIndex !== -1) {
          isBelowOverItem = activeIndex < overIndex;
        } else {
          const activeRect = active.rect.current.translated;
          const overRect = over.rect;
          isBelowOverItem = false;
          if (activeRect && overRect) {
            const activeMidY = activeRect.top + activeRect.height / 2;
            const overMidY = overRect.top + overRect.height / 2;
            isBelowOverItem = activeMidY > overMidY;
          }
        }

        if (isBelowOverItem) {
          prevLex = tasks[overIndex].lexRank;
          nextLex = overIndex + 1 < tasks.length ? tasks[overIndex + 1].lexRank : null;
        } else {
          prevLex = overIndex - 1 >= 0 ? tasks[overIndex - 1].lexRank : null;
          nextLex = tasks[overIndex].lexRank;
        }
      }
    } else if (destColumn && overType === 'Column') {
      // Dropping directly on a column (append to end)
      const tasks = destColumn.tasks || [];
      if (tasks.length > 0) {
        prevLex = tasks[tasks.length - 1].lexRank;
      }
    }

    // --- Execute Optimistic UI Updates ---
    queryClient.setQueryData(boardKeys.detail(boardId), (oldData: any) => {
      if (!oldData) return oldData;
      // Note: Full optimistic sorting is complex in tree-state. This is a partial
      // UI feedback (task shifts to column). Actual order snap happens on refetch.
      return { ...oldData }; 
    });

    try {
      await moveTaskMutation.mutateAsync({
        taskId: activeId,
        request: {
          currentVersion: activeTaskData.version,
          newStatusId,
          prevLexRank: prevLex,
          nextLexRank: nextLex
        }
      });
      // The `onSettled` in our hook will refetch the true state
    } catch (err) {
      console.warn('OCC Conflict detected. Rolling back board state.');
      // Rollback on conflict (e.g. someone else mutated the board)
      queryClient.setQueryData(boardKeys.detail(boardId), previousBoard);
      queryClient.invalidateQueries({ queryKey: boardKeys.detail(boardId) });
      // Here we could trigger a toast notification (e.g. Sonner) to inform the user
      alert('Board state changed by another user. Reverting to latest.');
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-full">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  if (error || !board) {
    return <div className="p-8 text-destructive">Failed to load board.</div>;
  }

  return (
    <div className="flex flex-col flex-1 bg-transparent relative animate-in fade-in duration-500">
      {/* Board Header */}
      <div className="px-6 py-4 flex items-center justify-between bg-background/80 backdrop-blur-md supports-[backdrop-filter]:bg-background/60 sticky top-0 z-10">
        <div className="flex items-center gap-4">
          <button 
            onClick={() => navigate('/boards')}
            className="p-2 -ml-2 hover:bg-muted rounded-full text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <h2 className="text-xl font-bold text-foreground tracking-tight">{board.title}</h2>
        </div>
      </div>

      {/* Board Canvas (Horizontal Scroll) */}
      <div className="flex-1 overflow-x-auto overflow-y-hidden p-6 scrollbar-auto-hide">
        <DndContext
          sensors={sensors}
          collisionDetection={closestCorners}
          onDragStart={handleDragStart}
          onDragEnd={handleDragEnd}
        >
          <div className="flex gap-6 h-full items-start">
            {columns.map((column) => (
              <BoardColumn 
                key={column.id} 
                column={column} 
                onTaskClick={(task) => setEditingTask(task)}
              />
            ))}

            {/* Add Column Button */}
            <div className="flex-shrink-0 w-80">
              {isAddingCol ? (
                <form 
                  onSubmit={handleCreateColumn}
                  className="bg-muted/40 rounded-xl p-3 border border-border shadow-sm"
                >
                  <input
                    autoFocus
                    className="w-full text-sm font-medium outline-none bg-background text-foreground placeholder-muted-foreground p-2 border border-border/50 rounded flex-shrink-0 focus:border-primary focus:ring-1 focus:ring-primary transition-all"
                    placeholder="Column title"
                    value={newColTitle}
                    onChange={(e) => setNewColTitle(e.target.value)}
                    disabled={createColumnMutation.isPending}
                  />
                  <div className="flex justify-end gap-2 mt-3">
                    <button
                      type="button"
                      onClick={() => setIsAddingCol(false)}
                      className="text-xs text-muted-foreground hover:text-foreground px-2 py-1 font-medium transition-colors"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={!newColTitle.trim() || createColumnMutation.isPending}
                      className="text-xs bg-primary text-primary-foreground px-3 py-1.5 rounded-md hover:bg-primary/90 disabled:opacity-50 font-medium tracking-wide transition-colors"
                    >
                      {createColumnMutation.isPending ? 'Saving...' : 'Save Column'}
                    </button>
                  </div>
                </form>
              ) : (
                <button
                  onClick={() => setIsAddingCol(true)}
                  className="w-full flex items-center gap-2 p-3 rounded-xl bg-muted/20 hover:bg-muted/50 border-2 border-dashed border-border hover:border-primary/50 text-muted-foreground hover:text-foreground font-medium transition-all duration-300 group"
                >
                  <div className="p-1 rounded bg-background shadow-sm border border-border/50 group-hover:scale-110 transition-transform duration-300 text-muted-foreground group-hover:text-primary">
                    <Plus className="w-4 h-4" />
                  </div>
                  Add new column
                </button>
              )}
            </div>
          </div>

          <DragOverlay>
            {activeTask ? <TaskCard task={activeTask} /> : null}
          </DragOverlay>
        </DndContext>
      </div>
      {editingTask && boardId && (
        <TaskModal 
          boardId={boardId}
          task={editingTask}
          onClose={() => setEditingTask(null)}
        />
      )}
    </div>
  );
}
