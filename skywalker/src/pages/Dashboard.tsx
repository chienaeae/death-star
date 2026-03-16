import { useServerEvents } from '../hooks/useServerEvents';

export function Dashboard() {
  useServerEvents();

  return (
    <div className="p-8">
      <div className="max-w-4xl mx-auto">
        <div className="mb-10">
          <h2 className="text-2xl font-bold text-gray-800">Welcome to your Dashboard</h2>
        </div>
      </div>
    </div>
  );
}
