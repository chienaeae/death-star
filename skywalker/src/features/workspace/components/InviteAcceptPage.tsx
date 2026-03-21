import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { Loader2 } from 'lucide-react';
import { useAcceptInvite } from '../api/workspaces';
import { useWorkspaceContext } from '../context/WorkspaceContext';

export function InviteAcceptPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();
  const { setActiveWorkspaceId } = useWorkspaceContext();
  const hasFired = useRef(false);
  const { mutateAsync: acceptInvite, isPending, isError, error } = useAcceptInvite();

  useEffect(() => {
    if (token && !hasFired.current) {
      hasFired.current = true;
      acceptInvite(token)
        .then((workspace) => {
          setActiveWorkspaceId(workspace.id!);
          // Add a small delay so user can see success message or immediately redirect
          setTimeout(() => navigate('/'), 1000);
        })
        .catch((err) => {
          console.error('Invite acceptance failed', err);
        });
    }
  }, [token, acceptInvite, navigate]);

  if (!token) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen">
        <h2 className="text-xl font-semibold mb-2">Invalid Invite Link</h2>
        <p className="text-muted-foreground">No token provided in the URL.</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-background text-foreground relative overflow-hidden">
      {/* Dynamic Background */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-primary/10 via-background to-background pointer-events-none" />

      <div className="z-10 bg-card p-8 rounded-2xl border border-border shadow-2xl backdrop-blur-md max-w-md w-full text-center">
        <h2 className="text-2xl font-bold mb-4 tracking-tight">Joining Workspace</h2>
        
        {isPending && (
          <div className="flex flex-col items-center text-muted-foreground">
            <Loader2 className="h-8 w-8 animate-spin mb-4 text-primary" />
            <p>Accepting your invitation...</p>
          </div>
        )}
        
        {isError && (
          <div className="text-destructive mt-2">
            <p className="font-semibold">Failed to join workspace</p>
            <p className="text-sm mt-1">{error?.message || 'The invite might be expired or invalid.'}</p>
          </div>
        )}

        {!isPending && !isError && (
          <div className="text-emerald-500 mt-2">
            <p className="font-semibold text-lg">Successfully joined!</p>
            <p className="text-sm mt-1">Redirecting you to the dashboard...</p>
          </div>
        )}
      </div>
    </div>
  );
}
