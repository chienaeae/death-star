import { Button } from '@death-star/millennium';

import { useServerEvents } from '../hooks/useServerEvents';

interface DashboardProps {
  onLogout: () => void;
}

export function Dashboard({ onLogout }: DashboardProps) {
  useServerEvents();



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


      </div>
    </div>
  );
}
