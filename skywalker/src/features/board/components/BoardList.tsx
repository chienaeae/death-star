import { useState } from 'react';
import { useNavigate } from 'react-router';
import { Plus, KanbanSquare } from 'lucide-react';
import { useBoards, useCreateBoard } from '../api/boards';

export function BoardList() {
  const { data: boards, isLoading, error } = useBoards();
  const createBoardMutation = useCreateBoard();
  const navigate = useNavigate();

  const [isCreating, setIsCreating] = useState(false);
  const [newTitle, setNewTitle] = useState('');

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;
    
    try {
      const newBoard = await createBoardMutation.mutateAsync({ title: newTitle.trim() });
      setNewTitle('');
      setIsCreating(false);
      navigate(`/boards/${newBoard.id}`);
    } catch (err) {
      console.error('Failed to create board:', err);
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-destructive text-sm text-center py-12">
        Failed to load boards. Please try again.
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-semibold tracking-tight">Your Boards</h2>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Create New Board Card */}
        {isCreating ? (
          <form 
            onSubmit={handleCreate}
            className="flex flex-col justify-center h-32 p-4 bg-white border-2 border-dashed border-primary/50 shadow-sm rounded-xl space-y-3"
          >
            <input
              autoFocus
              className="text-sm font-medium w-full bg-transparent outline-none border-b border-gray-200 pb-1 px-1 focus:border-primary transition-colors"
              placeholder="Board title..."
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              disabled={createBoardMutation.isPending}
            />
            <div className="flex gap-2 justify-end">
              <button
                type="button"
                onClick={() => setIsCreating(false)}
                className="text-xs px-3 py-1.5 text-gray-500 hover:text-gray-700 font-medium"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={!newTitle.trim() || createBoardMutation.isPending}
                className="text-xs px-3 py-1.5 bg-primary text-primary-foreground rounded-md font-medium hover:bg-primary/90 disabled:opacity-50"
              >
                {createBoardMutation.isPending ? 'Saving...' : 'Create'}
              </button>
            </div>
          </form>
        ) : (
          <button
            onClick={() => setIsCreating(true)}
            className="flex flex-col items-center justify-center h-32 bg-gray-50 hover:bg-gray-100 border-2 border-dashed border-gray-200 hover:border-gray-300 rounded-xl transition-all group"
          >
            <div className="h-10 w-10 rounded-full bg-white shadow-sm flex items-center justify-center mb-2 group-hover:scale-105 transition-transform">
              <Plus className="w-5 h-5 text-gray-400 group-hover:text-gray-600" />
            </div>
            <span className="text-sm font-medium text-gray-500 group-hover:text-gray-700">
              Create new board
            </span>
          </button>
        )}

        {/* Existing Boards */}
        {boards?.map((board) => (
          <button
            key={board.id}
            onClick={() => navigate(`/boards/${board.id}`)}
            className="flex flex-col items-start h-32 p-5 bg-white border border-gray-200 rounded-xl hover:border-primary/50 hover:shadow-md transition-all group text-left relative overflow-hidden"
          >
            {/* Subtle Gradient Overlay */}
            <div className="absolute inset-0 bg-gradient-to-br from-primary/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
            
            <KanbanSquare className="w-6 h-6 text-primary mb-3 relative z-10" />
            <span className="font-semibold text-gray-800 line-clamp-1 relative z-10 group-hover:text-primary transition-colors hover:cursor-pointer">
              {board.title}
            </span>
          </button>
        ))}
      </div>
    </div>
  );
}
