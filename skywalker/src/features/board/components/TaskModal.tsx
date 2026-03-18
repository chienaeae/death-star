import { useState, useEffect } from 'react';
import { Calendar, Flag, AlignLeft, Tags, Plus, Trash2 } from 'lucide-react';
import type { components } from '@death-star/holocron';
import { useUpdateTask } from '../api/boards';
import {
  Drawer,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
  DrawerFooter,
  Button
} from '@death-star/millennium';

type BoardTask = components['schemas']['BoardTask'];

interface TaskModalProps {
  boardId: string;
  task: BoardTask;
  onClose: () => void;
}

export function TaskModal({ boardId, task, onClose }: TaskModalProps) {
  const updateTaskMutation = useUpdateTask(boardId);
  const [title, setTitle] = useState(task.title || '');
  const [description, setDescription] = useState(task.description || '');
  const [priority, setPriority] = useState(task.priority || '');
  const [dueDate, setDueDate] = useState(task.dueDate || '');
  
  // Custom Attributes Management
  const [attributes, setAttributes] = useState<Record<string, string>>(task.attributes || {});
  const [newAttrKey, setNewAttrKey] = useState('');
  const [newAttrValue, setNewAttrValue] = useState('');

  // Reset state if underlying task changes (e.g., opened a different task)
  useEffect(() => {
    setTitle(task.title || '');
    setDescription(task.description || '');
    setPriority(task.priority || '');
    setDueDate(task.dueDate || '');
    setAttributes(task.attributes || {});
    setNewAttrKey('');
    setNewAttrValue('');
  }, [task]);

  const handleAddAttribute = () => {
    if (newAttrKey.trim() && newAttrValue.trim()) {
      setAttributes(prev => ({
        ...prev,
        [newAttrKey.trim()]: newAttrValue.trim()
      }));
      setNewAttrKey('');
      setNewAttrValue('');
    }
  };

  const handleRemoveAttribute = (key: string) => {
    setAttributes(prev => {
      const next = { ...prev };
      delete next[key];
      return next;
    });
  };

  const handleSave = async () => {
    try {
      await updateTaskMutation.mutateAsync({
        taskId: task.id,
        request: {
          currentVersion: task.version,
          title: title.trim(),
          description: description.trim() || undefined,
          priority: priority.trim() || undefined,
          dueDate: dueDate || undefined,
          attributes: Object.keys(attributes).length > 0 ? attributes : undefined
        }
      });
      onClose();
    } catch (err) {
      console.error('Failed to update task', err);
    }
  };

  return (
    <Drawer open={true} direction="right" onOpenChange={(open) => { if (!open) onClose(); }}>
      <DrawerContent className="max-h-screen">
        <DrawerHeader className="border-b border-border pb-4">
          <DrawerTitle className="text-xl font-bold">Edit Task</DrawerTitle>
        </DrawerHeader>

        {/* Content */}
        <div className="overflow-y-auto px-6 py-6 space-y-6 form-control">
          
          {/* Title input */}
          <div className="space-y-2">
            <label className="text-sm font-medium flex items-center gap-2">
              <span className="text-destructive">*</span> Title
            </label>
            <input 
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-4 py-3 text-lg font-medium bg-background border border-input rounded-xl focus:ring-2 focus:ring-ring focus:border-ring transition-all outline-none"
              placeholder="Task name"
            />
          </div>

          <div className="h-px bg-border" />

          {/* Core Properties */}
          <div className="space-y-5">
            <div className="flex items-start gap-4">
              <div className="mt-2.5 text-muted-foreground"><AlignLeft className="w-5 h-5" /></div>
              <div className="flex-1">
                <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2 block">Description</label>
                <textarea 
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full px-4 py-3 text-sm bg-muted/50 border border-transparent rounded-xl focus:bg-background focus:border-ring focus:ring-2 focus:ring-ring/20 resize-none min-h-[120px] outline-none transition-all placeholder:text-muted-foreground"
                  placeholder="Add a more detailed description..."
                />
              </div>
            </div>

            <div className="flex items-center gap-4">
              <div className="text-muted-foreground"><Flag className="w-5 h-5" /></div>
              <div className="flex-1">
                <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2 block">Priority</label>
                <select 
                  value={priority}
                  onChange={(e) => setPriority(e.target.value)}
                  className="w-full px-4 py-3 text-sm bg-muted/50 border border-transparent rounded-xl focus:bg-background focus:border-ring focus:ring-2 focus:ring-ring/20 outline-none transition-all"
                >
                  <option value="">None</option>
                  <option value="Low">Low</option>
                  <option value="Medium">Medium</option>
                  <option value="High">High</option>
                  <option value="Critical">Critical</option>
                </select>
              </div>
            </div>

            <div className="flex items-center gap-4">
              <div className="text-muted-foreground"><Calendar className="w-5 h-5" /></div>
              <div className="flex-1">
                <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2 block">Due Date</label>
                <input 
                  type="date"
                  value={dueDate}
                  onChange={(e) => setDueDate(e.target.value)}
                  className="w-full px-4 py-3 text-sm bg-muted/50 border border-transparent rounded-xl focus:bg-background focus:border-ring focus:ring-2 focus:ring-ring/20 outline-none transition-all"
                />
              </div>
            </div>
          </div>

          <div className="h-px bg-border" />

          {/* Dynamic Attributes Map */}
          <div className="space-y-4 pb-8">
            <div className="flex items-center gap-2 font-medium">
              <Tags className="w-4 h-4 text-muted-foreground" />
              Custom Attributes
            </div>
            
            <div className="bg-muted/30 rounded-xl p-5 border border-border space-y-3">
              {Object.entries(attributes).map(([key, value]) => (
                <div key={key} className="flex items-center gap-3 bg-background p-3 rounded-lg border border-border shadow-sm group hover:border-primary/50 transition-colors">
                  <span className="text-xs font-bold text-muted-foreground uppercase w-1/3 truncate" title={key}>{key}</span>
                  <span className="text-sm flex-1 truncate">{value}</span>
                  <button 
                    onClick={() => handleRemoveAttribute(key)}
                    className="p-1.5 text-muted-foreground hover:bg-destructive/10 hover:text-destructive rounded-md opacity-0 group-hover:opacity-100 transition-all"
                    title="Remove Attribute"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
              
              {/* Add New Attribute Row */}
              <div className="flex gap-3 items-center pt-3">
                <input 
                  placeholder="Key (e.g. Estimate)" 
                  value={newAttrKey}
                  onChange={(e) => setNewAttrKey(e.target.value)}
                  className="w-1/3 text-sm px-3 py-2 bg-background border border-border rounded-lg outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
                />
                <input 
                  placeholder="Value (e.g. 5d)" 
                  value={newAttrValue}
                  onChange={(e) => setNewAttrValue(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') handleAddAttribute(); }}
                  className="flex-1 text-sm px-3 py-2 bg-background border border-border rounded-lg outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
                />
                <Button 
                  onClick={handleAddAttribute}
                  disabled={!newAttrKey.trim() || !newAttrValue.trim()}
                  variant="secondary"
                  size="icon"
                >
                  <Plus className="w-4 h-4" />
                </Button>
              </div>
            </div>
          </div>

        </div>

        {/* Footer */}
        <DrawerFooter className="border-t border-border bg-muted/20 flex-row justify-end gap-3 pt-4 pb-6 px-6">
          <Button 
            onClick={onClose}
            variant="outline"
          >
            Cancel
          </Button>
          <Button 
            onClick={handleSave}
            disabled={updateTaskMutation.isPending || !title.trim()}
          >
            {updateTaskMutation.isPending ? 'Saving...' : 'Save Changes'}
          </Button>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}
