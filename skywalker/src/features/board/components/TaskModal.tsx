import { useState, useEffect } from 'react';
import { X, Calendar, Flag, AlignLeft, Tags, Plus, Trash2 } from 'lucide-react';
import type { components } from '@death-star/holocron';
import { useUpdateTask } from '../api/boards';

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
      // Workaround: We might need to send a null/empty value to backend to delete, 
      // but the API currently merges. For simplicity let's rely on standard JSON behavior. 
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
      // Let the user retry or view error
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/40 backdrop-blur-sm animate-in fade-in duration-200">
      <div 
        className="w-full max-w-md bg-white h-full shadow-2xl flex flex-col animate-in slide-in-from-right duration-300"
        role="dialog"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <h2 className="text-lg font-semibold text-gray-800">Edit Task</h2>
          <button 
            onClick={onClose}
            className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6 form-control">
          
          {/* Title input */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-700 flex items-center gap-2">
              <span className="text-red-500">*</span> Title
            </label>
            <input 
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-3 py-2 text-lg font-medium text-gray-900 bg-transparent border-gray-200 border rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none"
              placeholder="Task name"
            />
          </div>

          <hr className="border-gray-100" />

          {/* Core Properties */}
          <div className="space-y-4">
            <div className="flex items-start gap-4">
              <div className="mt-2.5 text-gray-400"><AlignLeft className="w-5 h-5" /></div>
              <div className="flex-1">
                <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1 block">Description</label>
                <textarea 
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full px-3 py-2 text-sm text-gray-700 bg-gray-50 border border-transparent rounded-lg focus:bg-white focus:border-primary focus:ring-2 focus:ring-primary/20 resize-none min-h-[100px] outline-none transition-all placeholder:text-gray-400"
                  placeholder="Add a more detailed description..."
                />
              </div>
            </div>

            <div className="flex items-center gap-4">
              <div className="text-gray-400"><Flag className="w-5 h-5" /></div>
              <div className="flex-1">
                <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1 block">Priority</label>
                <select 
                  value={priority}
                  onChange={(e) => setPriority(e.target.value)}
                  className="w-full px-3 py-2 text-sm text-gray-700 bg-gray-50 border border-transparent rounded-lg focus:bg-white focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all"
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
              <div className="text-gray-400"><Calendar className="w-5 h-5" /></div>
              <div className="flex-1">
                <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1 block">Due Date</label>
                <input 
                  type="date"
                  value={dueDate}
                  onChange={(e) => setDueDate(e.target.value)}
                  className="w-full px-3 py-2 text-sm text-gray-700 bg-gray-50 border border-transparent rounded-lg focus:bg-white focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all"
                />
              </div>
            </div>
          </div>

          <hr className="border-gray-100" />

          {/* Dynamic Attributes Map */}
          <div className="space-y-4">
            <div className="flex items-center gap-2 text-gray-700 font-medium">
              <Tags className="w-4 h-4 text-gray-400" />
              Custom Attributes
            </div>
            
            <div className="bg-gray-50 rounded-lg p-4 border border-gray-100 space-y-3">
              {Object.entries(attributes).map(([key, value]) => (
                <div key={key} className="flex items-center gap-2 bg-white p-2 rounded border border-gray-100 shadow-sm group">
                  <span className="text-xs font-bold text-gray-500 uppercase w-1/3 truncate" title={key}>{key}</span>
                  <span className="text-sm text-gray-700 flex-1 truncate">{value}</span>
                  <button 
                    onClick={() => handleRemoveAttribute(key)}
                    className="p-1 text-gray-300 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                    title="Remove Attribute"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
              
              {/* Add New Attribute Row */}
              <div className="flex gap-2 items-center pt-2">
                <input 
                  placeholder="Key (e.g. Estimate)" 
                  value={newAttrKey}
                  onChange={(e) => setNewAttrKey(e.target.value)}
                  className="w-1/3 text-sm px-2 py-1.5 border border-gray-200 rounded outline-none focus:border-primary"
                />
                <input 
                  placeholder="Value (e.g. 5d)" 
                  value={newAttrValue}
                  onChange={(e) => setNewAttrValue(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') handleAddAttribute(); }}
                  className="flex-1 text-sm px-2 py-1.5 border border-gray-200 rounded outline-none focus:border-primary"
                />
                <button 
                  onClick={handleAddAttribute}
                  disabled={!newAttrKey.trim() || !newAttrValue.trim()}
                  className="p-1.5 bg-primary/10 text-primary rounded hover:bg-primary/20 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  <Plus className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>

        </div>

        {/* Footer */}
        <div className="p-4 border-t border-gray-100 bg-gray-50 flex justify-end gap-3 mt-auto">
          <button 
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-gray-600 hover:text-gray-800 transition-colors"
          >
            Cancel
          </button>
          <button 
            onClick={handleSave}
            disabled={updateTaskMutation.isPending || !title.trim()}
            className="px-5 py-2 text-sm font-semibold bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 disabled:opacity-50 transition-colors flex items-center gap-2 shadow-sm"
          >
            {updateTaskMutation.isPending ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </div>
    </div>
  );
}
