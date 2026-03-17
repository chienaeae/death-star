import { useServerEvents } from '../hooks/useServerEvents';
import { BoardList } from '../features/board/components/BoardList';

export function Dashboard() {
  useServerEvents();

  return (
    <div className="p-8">
      <div className="max-w-6xl mx-auto">
        <BoardList />
      </div>
    </div>
  );
}
