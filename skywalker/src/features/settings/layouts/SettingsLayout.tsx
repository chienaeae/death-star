import { Bell, Lock, ShieldCheck, User } from 'lucide-react';
import { NavLink } from 'react-router';

export function SettingsLayout({ children }: { children: React.ReactNode }) {
  // Commenting out the logout here since it's not in the reference side menu
  // const handleLogout = async () => { ... }

  return (
    <div className="w-full min-h-screen bg-muted/20 flex flex-col items-center">
      <div className="w-full max-w-6xl px-8 pt-8 pb-16">
        <h1 className="text-2xl font-bold text-foreground mb-8">Account settings</h1>
        
        <div className="flex gap-8 items-start">
          {/* Left Sidebar Menu */}
          <aside className="w-64 shrink-0 bg-card border border-border shadow-sm rounded-lg overflow-hidden py-2">
            <nav className="flex flex-col">
              <NavLink
                to="/settings/profile"
                className={({ isActive }) =>
                  `flex items-center gap-4 px-6 py-4 text-sm font-medium transition-colors relative ${
                    isActive
                      ? 'bg-accent text-accent-foreground'
                      : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                  }`
                }
              >
                {({ isActive }) => (
                  <>
                    {isActive && (
                      <div className="absolute left-0 top-0 bottom-0 w-1 bg-primary"></div>
                    )}
                    <User className="w-5 h-5" />
                    Profile Settings
                  </>
                )}
              </NavLink>

              <div className="flex items-center gap-4 px-6 py-4 text-sm font-medium text-muted-foreground cursor-not-allowed">
                <Lock className="w-5 h-5" />
                Password
              </div>

              <div className="flex items-center gap-4 px-6 py-4 text-sm font-medium text-muted-foreground cursor-not-allowed">
                <Bell className="w-5 h-5" />
                Notifications
              </div>
              
              <div className="flex items-center gap-4 px-6 py-4 text-sm font-medium text-muted-foreground cursor-not-allowed">
                <ShieldCheck className="w-5 h-5" />
                Verification
              </div>
            </nav>
          </aside>

          {/* Main Content Area */}
          <main className="flex-1 w-full max-w-4xl">
            {children}
          </main>
        </div>
      </div>
    </div>
  );
}

