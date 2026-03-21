import { SettingsLayout } from '../../settings/layouts/SettingsLayout';
import { useWorkspaceContext } from '../context/WorkspaceContext';
import { useWorkspaces, useInviteToWorkspace } from '../api/workspaces';
import { useState } from 'react';
import { Loader2, Mail, CheckCircle2 } from 'lucide-react';

export function WorkspaceSettings() {
  const { activeWorkspaceId } = useWorkspaceContext();
  const { data: workspaces } = useWorkspaces();
  const inviteMutation = useInviteToWorkspace(activeWorkspaceId || '');
  
  const [email, setEmail] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const activeWorkspace = workspaces?.find(w => w.id === activeWorkspaceId);

  const handleInvite = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !activeWorkspaceId) return;
    
    try {
      await inviteMutation.mutateAsync({ email });
      setSuccessMsg(`Invitation sent specifically to ${email}`);
      setEmail('');
      setTimeout(() => setSuccessMsg(''), 5000);
    } catch (err) {
      console.error('Failed to invite:', err);
    }
  };

  return (
    <SettingsLayout>
      <div className="bg-card text-card-foreground border shadow-sm rounded-lg p-10 animate-in fade-in duration-500">
        <div className="space-y-8">
          <div>
            <h2 className="text-xl font-semibold mb-2">Workspace Settings</h2>
            <p className="text-sm text-muted-foreground">
              Manage your active workspace and invite members.
            </p>
          </div>

          <div className="p-6 bg-muted/30 rounded-lg border border-border">
            <h3 className="text-sm font-medium mb-1 text-muted-foreground">Active Workspace</h3>
            <p className="text-lg font-semibold">{activeWorkspace?.name || 'Loading...'}</p>
          </div>

          <div className="space-y-4">
            <h3 className="text-lg font-medium">Invite Members</h3>
            <p className="text-sm text-muted-foreground">
              Send an email invitation allowing a user to join this workspace.
            </p>

            <form onSubmit={handleInvite} className="flex gap-3 mt-4">
              <div className="relative flex-1 max-w-md">
                <div className="absolute inset-y-0 left-3 flex items-center pointer-events-none text-muted-foreground">
                  <Mail className="w-4 h-4" />
                </div>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="colleague@example.com"
                  className="w-full h-10 pl-10 pr-4 rounded-md border border-input bg-transparent text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring transition-colors"
                  required
                />
              </div>
              <button
                type="submit"
                disabled={inviteMutation.isPending || !activeWorkspaceId}
                className="h-10 px-6 bg-primary text-primary-foreground font-medium rounded-md text-sm hover:bg-primary/90 transition-colors flex items-center justify-center disabled:opacity-50 min-w-[100px]"
              >
                {inviteMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Send Invite'}
              </button>
            </form>

            {successMsg && (
              <div className="flex items-center gap-2 mt-4 text-sm text-emerald-500 bg-emerald-500/10 p-3 rounded-md">
                <CheckCircle2 className="w-4 h-4" />
                {successMsg}
              </div>
            )}
            
            {inviteMutation.isError && (
              <div className="flex items-center gap-2 mt-4 text-sm text-destructive bg-destructive/10 p-3 rounded-md">
                Error sending invitation. Please try again.
              </div>
            )}
          </div>
        </div>
      </div>
    </SettingsLayout>
  );
}
