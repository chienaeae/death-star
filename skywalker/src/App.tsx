import { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router';
import { apiClient } from './api/client';
import { AppLayout } from './components/layout/AppLayout';
import { AuthPortal } from './features/auth/components/AuthPortal';
import { Initializing } from './features/auth/components/Initializing';
import { Dashboard } from './pages/Dashboard';
import { UserProfile } from './features/profile/components/UserProfile';
import { BoardView } from './features/board/components/BoardView';
import { WorkspaceSettings } from './features/workspace/components/WorkspaceSettings';
import { InviteAcceptPage } from './features/workspace/components/InviteAcceptPage';

// --- First Principles: Auth State Machine Definition ---
type AuthState = 'PENDING' | 'AUTHENTICATED' | 'UNAUTHENTICATED';

export default function App() {
  const [authState, setAuthState] = useState<AuthState>('PENDING');
  const [errorMsg, setErrorMsg] = useState('');

  // --- Initialization Flow (Init Container Pattern) ---
  useEffect(() => {
    const initApp = async () => {
      // Silent Renewal: On F5 refresh, automatically sends HttpOnly Cookie to fetch in-memory Access Token
      const success = await apiClient.hydrateSession();
      setAuthState(success ? 'AUTHENTICATED' : 'UNAUTHENTICATED');
    };
    initApp();

    // Listen for cross-tab logout/sync signals (BroadcastChannel)
    const handleAuthSync = (e: MessageEvent) => {
      if (e.data.type === 'SESSION_TERMINATED') {
        setAuthState('UNAUTHENTICATED');
      } else if (e.data.type === 'TOKEN_REFRESHED') {
        setAuthState('AUTHENTICATED');
      }
    };
    const channel = new BroadcastChannel('auth_sync_channel');
    channel.addEventListener('message', handleAuthSync);

    return () => channel.removeEventListener('message', handleAuthSync);
  }, []);

  const handleLogin = async (email: string, password: string) => {
    try {
      await apiClient.login({ email, password });
      setAuthState('AUTHENTICATED');
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Login failed');
    }
  };

  const handleRegister = async (email: string, password: string) => {
    try {
      await apiClient.register({ email, password });
      setAuthState('AUTHENTICATED');
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Registration failed');
    }
  };

  const handleLogout = async () => {
    try {
      await apiClient.logout();
    } finally {
      setAuthState('UNAUTHENTICATED');
    }
  };

  // --- Blocking Render ---
  if (authState === 'PENDING') {
    return <Initializing />;
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout authState={authState} onLogout={handleLogout} />}>
          <Route
            path="/auth"
            element={
              authState === 'AUTHENTICATED' ? (
                <Navigate to="/" replace />
              ) : (
                <AuthPortal
                  errorMsg={errorMsg}
                  setErrorMsg={setErrorMsg}
                  onLogin={handleLogin}
                  onRegister={handleRegister}
                />
              )
            }
          />
          <Route
            path="/"
            element={
              authState === 'UNAUTHENTICATED' ? <Navigate to="/auth" replace /> : <Dashboard />
            }
          />
          <Route
            path="/boards/:boardId"
            element={
              authState === 'UNAUTHENTICATED' ? <Navigate to="/auth" replace /> : <BoardView />
            }
          />
          <Route
            path="/settings"
            element={<Navigate to="/settings/profile" replace />}
          />
          <Route
            path="/settings/profile"
            element={
              authState === 'UNAUTHENTICATED' ? <Navigate to="/auth" replace /> : <UserProfile />
            }
          />
          <Route
            path="/settings/workspace"
            element={
              authState === 'UNAUTHENTICATED' ? <Navigate to="/auth" replace /> : <WorkspaceSettings />
            }
          />
          <Route
            path="/invite"
            element={
              authState === 'UNAUTHENTICATED' ? <Navigate to="/auth" replace /> : <InviteAcceptPage />
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
