import { Outlet } from 'react-router';

export function AppLayout() {
  return (
    <div className="min-h-screen w-full bg-gradient-to-b from-[#a1d6f5] to-[#f4faff] font-sans antialiased text-gray-900">
      <Outlet />
    </div>
  );
}
