import { Outlet, useLocation } from 'react-router';
import { Topbar } from './Topbar';
import { Particles } from './Particles';

interface AppLayoutProps {
  authState?: 'PENDING' | 'AUTHENTICATED' | 'UNAUTHENTICATED';
  onLogout?: () => void;
}

export function AppLayout({ authState, onLogout }: AppLayoutProps) {
  const location = useLocation();
  const isAuthPage = location.pathname.startsWith('/auth');
  const showTopbar = authState === 'AUTHENTICATED' && !isAuthPage && !!onLogout;

  return (
    <div className="min-h-screen w-full bg-background font-sans antialiased text-foreground flex flex-col relative z-0 overflow-hidden">
      {/* Dynamic Particle Fragments */}
      <Particles />

      {showTopbar && <Topbar onLogout={onLogout!} />}
      <main className="flex-1 flex flex-col w-full relative">
        <Outlet />
      </main>
    </div>
  );
}
