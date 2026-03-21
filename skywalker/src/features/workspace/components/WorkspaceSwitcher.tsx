import { Check, ChevronsUpDown, Loader2, PlusCircle, User } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useWorkspaces, useCreateWorkspace } from '../api/workspaces';
import { useWorkspaceContext } from '../context/WorkspaceContext';
import { useNavigate } from 'react-router';
import { useCurrentUser } from '../../auth/api/auth';
import {
  Button,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
  Input,
  Label,
} from '@death-star/millennium';
import { cn } from '@death-star/millennium';

export function WorkspaceSwitcher() {
  const { data: workspaces, isLoading: isWorkspacesLoading } = useWorkspaces();
  const { data: currentUser, isLoading: isUserLoading } = useCurrentUser();
  const { activeWorkspaceId, setActiveWorkspaceId } = useWorkspaceContext();
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [newWorkspaceName, setNewWorkspaceName] = useState('');
  const createWorkspace = useCreateWorkspace();

  useEffect(() => {
    // Synchronize local active workspace with the server's authoritative state ONLY on initial load.
    // Allow the local context state to drive changes thereafter to avoid race conditions with React Query invalidations.
    if (!activeWorkspaceId && workspaces && workspaces.length > 0) {
      const serverActiveWs = workspaces.find(w => w.isActive);
      
      if (serverActiveWs) {
        setActiveWorkspaceId(serverActiveWs.id);
      } else {
        // Fallback: Pick a default if nothing is active
        const defaultWs = workspaces.find(w => w.name?.includes('Personal')) || workspaces[0];
        if (defaultWs?.id) {
          setActiveWorkspaceId(defaultWs.id);
        }
      }
    }
  }, [activeWorkspaceId, workspaces, setActiveWorkspaceId]);

  if (isWorkspacesLoading || isUserLoading) {
    return (
      <div className="flex items-center justify-center w-[240px] h-12 mr-4 bg-neutral-900 border border-neutral-800 rounded-md text-muted-foreground">
        <Loader2 className="h-4 w-4 animate-spin" />
      </div>
    );
  }

  if (!workspaces || workspaces.length === 0) return null;

  const activeWs = workspaces.find(w => w.id === activeWorkspaceId);
  const isActiveOwner = currentUser && activeWs?.ownerId === currentUser.id;

  return (
    <div className="flex items-center mr-4">
      <DropdownMenu open={isOpen} onOpenChange={setIsOpen}>
        <DropdownMenuTrigger asChild>
          <button className="flex items-center justify-between w-[240px] px-3 py-2 bg-[#111111] border border-white/10 rounded-lg outline-none hover:bg-white/5 transition-colors group focus-visible:ring-1 focus-visible:ring-white/20">
            <div className="flex items-center gap-3 overflow-hidden">
              <div className="flex items-center justify-center w-8 h-8 rounded-[6px] bg-white/5 text-neutral-400 group-hover:text-neutral-300 transition-colors shrink-0">
                <User className="w-4 h-4" />
              </div>
              <div className="flex flex-col items-start min-w-0 pr-2 text-left">
                <span className="text-[13px] font-medium text-neutral-200 truncate w-full flex-1">
                  {activeWs ? activeWs.name : 'Select Workspace'}
                </span>
                {activeWs && (activeWs.isDefault || (!isActiveOwner && activeWs.ownerEmail)) && (
                  <span className="text-[10px] font-medium text-neutral-500 uppercase tracking-widest mt-0.5 truncate w-full">
                    {activeWs.isDefault ? 'PERSONAL' : activeWs.ownerEmail}
                  </span>
                )}
              </div>
            </div>
            <ChevronsUpDown className="w-4 h-4 text-neutral-500 shrink-0 group-hover:text-neutral-400" />
          </button>
        </DropdownMenuTrigger>
        
        <DropdownMenuContent align="start" className="w-[240px] bg-[#111111] border-white/10 p-1.5 shadow-2xl rounded-xl">
          <DropdownMenuLabel className="text-[11px] font-medium text-neutral-500 uppercase tracking-wider px-2 pt-2 pb-1.5 flex items-center h-8">
            SELECT WORKSPACE
          </DropdownMenuLabel>
          
          <div className="flex flex-col gap-0.5">
            {workspaces.map((w, index) => {
              const isOwner = currentUser && w.ownerId === currentUser.id;
              const isSelected = activeWorkspaceId === w.id;

              return (
                <DropdownMenuItem 
                  key={w.id} 
                  onSelect={() => {
                    setActiveWorkspaceId(w.id!);
                    setIsOpen(false);
                    navigate('/');
                  }}
                  className={cn(
                    "flex items-center justify-between px-2 py-1.5 rounded-[8px] cursor-default transition-colors outline-none",
                    isSelected ? "bg-white/10" : "focus:bg-white/5 hover:bg-white/5"
                  )}
                >
                  <div className="flex items-center gap-3 min-w-0 flex-1">
                    <div className={cn(
                      "flex items-center justify-center w-8 h-8 rounded-[6px] shrink-0",
                      isOwner ? "bg-white/5 text-neutral-400" : index % 2 === 0 ? "bg-blue-600/20 text-blue-400" : "bg-emerald-600/20 text-emerald-400"
                    )}>
                      <User className="w-4 h-4" />
                    </div>
                    <div className="flex flex-col items-start min-w-0 pr-2 flex-1">
                      <span className={cn(
                        "text-[13px] font-medium flex-1 truncate w-full transition-colors",
                        isSelected ? "text-white" : "text-neutral-300"
                      )}>
                        {w.name}
                      </span>
                      {(w.isDefault || (!isOwner && w.ownerEmail)) && (
                        <span className="text-[10px] font-medium text-neutral-500 uppercase tracking-widest mt-0.5 truncate w-full">
                          {w.isDefault ? 'PERSONAL' : w.ownerEmail}
                        </span>
                      )}
                    </div>
                  </div>
                  {isSelected && <Check className="w-[14px] h-[14px] text-white shrink-0 ml-2" />}
                </DropdownMenuItem>
              );
            })}
          </div>

          <DropdownMenuSeparator className="bg-white/5 my-1.5" />
          
          <DropdownMenuItem 
            onSelect={(e) => {
              e.preventDefault();
              setIsOpen(false);
              setIsCreateModalOpen(true);
            }}
            className="flex items-center gap-3 px-2 py-1.5 rounded-[8px] cursor-default text-neutral-400 outline-none focus:bg-white/5 hover:bg-white/5 focus:text-white hover:text-white transition-colors"
          >
            <div className="flex items-center justify-center w-8 h-8 rounded-[6px] shrink-0 border border-white/5 bg-transparent">
              <PlusCircle className="w-4 h-4 opacity-70" />
            </div>
            <span className="text-[13px] font-medium flex-1">
              Create Workspace
            </span>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <Dialog open={isCreateModalOpen} onOpenChange={setIsCreateModalOpen}>
        <DialogContent className="bg-[#111111] border-white/10 text-white sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>Create Workspace</DialogTitle>
            <DialogDescription className="text-neutral-400">
              Add a new workspace to organize your boards and collaborate with others.
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="name" className="text-white">Workspace Name</Label>
              <Input
                id="name"
                value={newWorkspaceName}
                onChange={(e) => setNewWorkspaceName(e.target.value)}
                placeholder="e.g. Acme Corp"
                className="bg-white/5 border-white/10 text-white placeholder:text-neutral-500"
                autoFocus
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && newWorkspaceName.trim() && !createWorkspace.isPending) {
                    createWorkspace.mutate({ name: newWorkspaceName.trim() }, {
                      onSuccess: () => {
                        setIsCreateModalOpen(false);
                        setNewWorkspaceName('');
                      }
                    });
                  }
                }}
              />
            </div>
          </div>
          <DialogFooter>
            <Button 
               variant="outline" 
               className="bg-transparent border-white/10 text-white hover:bg-white/5 hover:text-white"
               onClick={() => setIsCreateModalOpen(false)}
               disabled={createWorkspace.isPending}
            >
              Cancel
            </Button>
            <Button 
               className="bg-white text-black hover:bg-neutral-200"
               disabled={!newWorkspaceName.trim() || createWorkspace.isPending}
               onClick={() => {
                 createWorkspace.mutate({ name: newWorkspaceName.trim() }, {
                   onSuccess: () => {
                     setIsCreateModalOpen(false);
                     setNewWorkspaceName('');
                   }
                 });
               }}
            >
              {createWorkspace.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Create
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
