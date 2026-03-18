import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { GripVertical } from 'lucide-react';
import type { components } from '@death-star/holocron';

type BoardTask = components['schemas']['BoardTask'];

interface TaskCardProps {
  task: BoardTask;
  onClick?: () => void;
}

export function TaskCard(props: TaskCardProps) {
  const { task } = props;
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({
    id: task.id,
    data: {
      type: 'Task',
      task,
    },
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  if (isDragging) {
    return (
      <div 
        ref={setNodeRef}
        style={style}
        className="opacity-60 scale-105 min-h-[80px] bg-background border-2 border-primary border-dashed rounded-xl p-3 shadow-2xl z-50 ring-2 ring-primary/20 backdrop-blur-sm"
      />
    );
  }

  return (
    <div
      ref={setNodeRef}
      style={style}
      onClick={props.onClick}
      className={`group relative bg-card border border-border rounded-lg p-3 pr-8 shadow-sm hover:shadow-md hover:border-primary/50 transition-all duration-200 cursor-grab active:cursor-grabbing hover:-translate-y-[1px]`}
      {...attributes}
      {...listeners}
    >
      <div className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity">
        <GripVertical className="w-4 h-4" />
      </div>
      <p className="text-sm font-medium text-card-foreground leading-snug">
        {task.title}
      </p>
    </div>
  );
}
