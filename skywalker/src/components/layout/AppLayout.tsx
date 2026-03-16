import { Outlet, useLocation } from 'react-router';
import { Topbar } from './Topbar';

interface AppLayoutProps {
  authState?: 'PENDING' | 'AUTHENTICATED' | 'UNAUTHENTICATED';
  onLogout?: () => void;
}

export function AppLayout({ authState, onLogout }: AppLayoutProps) {
  const location = useLocation();
  const isAuthPage = location.pathname.startsWith('/auth');
  const showTopbar = authState === 'AUTHENTICATED' && !isAuthPage && !!onLogout;

  return (
    <div className="min-h-screen w-full bg-background bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-muted/50 via-background to-background font-sans antialiased text-foreground flex flex-col">
      {showTopbar && <Topbar onLogout={onLogout!} />}
      <div className="flex-1 w-full relative">
        <Outlet />
      </div>
    </div>
  );
}
