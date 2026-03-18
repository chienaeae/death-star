import { useMemo, useState } from 'react';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { useDroppable } from '@dnd-kit/core';
import { Plus } from 'lucide-react';
import type { components } from '@death-star/holocron';
import { useCreateTask } from '../api/boards';
import { TaskCard } from './TaskCard';

type BoardColumnType = components['schemas']['BoardColumn'];

interface BoardColumnProps {
  column: BoardColumnType;
  onTaskClick?: (task: components['schemas']['BoardTask']) => void;
}

export function BoardColumn(props: BoardColumnProps) {
  const { column } = props;
  const [isAdding, setIsAdding] = useState(false);
  const [newTaskTitle, setNewTaskTitle] = useState('');
  const createTaskMutation = useCreateTask(column.boardId);

  const tasksIds = useMemo(() => column.tasks?.map((t) => t.id) || [], [column.tasks]);

  const { setNodeRef } = useDroppable({
    id: column.id,
    data: {
      type: 'Column',
      column,
    },
  });

  const handleCreateTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTaskTitle.trim()) return;

    try {
      await createTaskMutation.mutateAsync({
        title: newTaskTitle.trim(),
        status: column.id,
      });
      setNewTaskTitle('');
      setIsAdding(false);
    } catch (err) {
      console.error('Failed to create task:', err);
    }
  };

  return (
    <div className="flex flex-col bg-muted/40 border border-border rounded-xl w-80 max-h-full flex-shrink-0 animate-in fade-in zoom-in-95 duration-300">
      {/* Column Header */}
      <div className="p-3 pb-2 flex items-center justify-between group">
        <h3 className="font-semibold text-foreground text-sm tracking-wide">
          {column.title}
        </h3>
        <span className="text-xs text-muted-foreground font-medium bg-muted px-2 py-0.5 rounded-full border border-border/50">
          {column.tasks?.length || 0}
        </span>
      </div>

      {/* Droppable Area */}
      <div
        ref={setNodeRef}
        className="flex-1 overflow-y-auto p-2 space-y-2 relative min-h-[150px] scrollbar-thin scrollbar-thumb-muted-foreground/20 scrollbar-track-transparent"
      >
        <SortableContext items={tasksIds} strategy={verticalListSortingStrategy}>
          {column.tasks?.map((task) => (
            <TaskCard 
              key={task.id} 
              task={task} 
              onClick={() => props.onTaskClick?.(task)}
            />
          ))}
        </SortableContext>

        {isAdding ? (
          <form 
            onSubmit={handleCreateTask}
            className="p-2 border-2 border-primary border-dashed bg-card rounded-lg shadow-sm"
          >
            <textarea
              autoFocus
              className="w-full text-sm outline-none resize-none bg-transparent text-foreground placeholder-muted-foreground"
              placeholder="What needs to be done?"
              rows={2}
              value={newTaskTitle}
              onChange={(e) => setNewTaskTitle(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  handleCreateTask(e);
                }
              }}
              disabled={createTaskMutation.isPending}
            />
            <div className="flex justify-end gap-2 mt-2">
              <button
                type="button"
                onClick={() => setIsAdding(false)}
                className="text-xs text-muted-foreground hover:text-foreground px-2 py-1 font-medium transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={!newTaskTitle.trim() || createTaskMutation.isPending}
                className="text-xs bg-primary text-primary-foreground px-3 py-1 rounded-md hover:bg-primary/90 disabled:opacity-50 font-medium tracking-wide transition-colors"
              >
                Save
              </button>
            </div>
          </form>
        ) : (
          <button
            onClick={() => setIsAdding(true)}
            className="flex items-center gap-2 text-muted-foreground hover:bg-muted hover:text-foreground w-full p-2 mt-1 border border-transparent hover:border-border rounded-lg text-sm font-medium transition-all"
          >
            <Plus className="w-4 h-4" />
            Add task
          </button>
        )}
      </div>
    </div>
  );
}
