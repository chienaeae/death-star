import { Button } from '@death-star/millennium';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../api/client';
import { ObjectiveForm } from '../features/objectives/components/ObjectiveForm';
import { ObjectiveList } from '../features/objectives/components/ObjectiveList';
import { useServerEvents } from '../hooks/useServerEvents';

interface DashboardProps {
  onLogout: () => void;
}

export function Dashboard({ onLogout }: DashboardProps) {
  useServerEvents();

  const { data: todos, isLoading } = useQuery({
    queryKey: ['todos'],
    queryFn: () => apiClient.getTodos(),
  });

  const handleCreateObjective = async (title: string) => {
    await apiClient.createTodo({ title });
  };

  return (
    <div className="min-h-screen p-8 pt-16">
      <div className="max-w-4xl mx-auto">
        <div className="flex justify-between items-center mb-10 pb-4 border-b border-gray-200/50">
          <h1 className="text-3xl font-bold text-gray-900 tracking-tight">Death Star Objectives</h1>
          <Button
            onClick={onLogout}
            variant="outline"
            className="border-gray-200 text-gray-600 hover:text-gray-900 hover:bg-white bg-white/50 transition-colors shadow-sm rounded-xl"
          >
            Logout
          </Button>
        </div>

        <ObjectiveForm onSubmit={handleCreateObjective} />
        <ObjectiveList todos={todos} isLoading={isLoading} />
      </div>
    </div>
  );
}
