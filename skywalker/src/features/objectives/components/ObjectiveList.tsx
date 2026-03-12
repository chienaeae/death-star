import type { components } from '@death-star/holocron';
import { Card, CardContent, CardHeader, CardTitle } from '@death-star/millennium';

interface ObjectiveListProps {
  todos: components['schemas']['Todo'][] | undefined;
  isLoading: boolean;
}

export function ObjectiveList({ todos, isLoading }: ObjectiveListProps) {
  return (
    <Card className="bg-white border-0 shadow-xl rounded-3xl">
      <CardHeader>
        <CardTitle className="text-xl font-bold text-gray-900">Active Objectives</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {isLoading && <p className="text-gray-500 animate-pulse">Scanning database...</p>}
        {todos?.length === 0 && !isLoading && (
          <p className="text-gray-500">No active objectives.</p>
        )}

        <ul className="space-y-3">
          {todos?.map((todo) => (
            <li
              key={todo.id}
              className="flex items-center justify-between p-4 bg-[#f9fafb] border border-gray-100 rounded-2xl shadow-sm"
            >
              <span
                className={`text-lg ${todo.completed ? 'line-through text-gray-400' : 'text-gray-800'}`}
              >
                {todo.title}
              </span>
              <span className="text-xs text-gray-400">
                {new Date(todo.createdAt ?? Date.now()).toLocaleString()}
              </span>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}
