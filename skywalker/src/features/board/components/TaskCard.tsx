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
        className="opacity-50 min-h-[80px] bg-white border-2 border-primary border-dashed rounded-lg p-3 shadow-sm"
      />
    );
  }

  return (
    <div
      ref={setNodeRef}
      style={style}
      onClick={props.onClick}
      className={`group relative bg-white border border-gray-200 rounded-lg p-3 pr-8 shadow-sm hover:shadow hover:border-gray-300 transition-all cursor-grab active:cursor-grabbing`}
      {...attributes}
      {...listeners}
    >
      <div className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-300 opacity-0 group-hover:opacity-100 transition-opacity">
        <GripVertical className="w-4 h-4" />
      </div>
      <p className="text-sm font-medium text-gray-700 leading-snug">
        {task.title}
      </p>
    </div>
  );
}
