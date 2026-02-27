import type React from "react";
import { useState } from "react";
import { useQuery, useMutation } from "@tanstack/react-query";
import { apiClient } from "./api/client";
import { useServerEvents } from "./hooks/useServerEvents";

// Import UI building blocks from our shared design system
import { Button } from "@death-star/millennium/src/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@death-star/millennium/src/components/ui/card";

/**
 * The primary orchestration component.
 * It strictly delegates UI rendering to 'millennium' and data fetching to 'api/client'.
 */
export default function App() {
  // 1. Initialize Real-time SSE Connection
  // This hook will directly patch the TanStack Query cache in the background
  // when NATS events are received from the Vader backend.
  useServerEvents();

  // 2. Local View State
  const [newTodoTitle, setNewTodoTitle] = useState("");

  // 3. Server State (Query)
  // We rely on the SSE hook to keep this fresh. Stale time is set to infinity in main.tsx.
  const {
    data: todos,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["todos"],
    queryFn: apiClient.getTodos,
  });

  // 4. Server State (Mutation)
  const createMutation = useMutation({
    mutationFn: apiClient.createTodo,
    onSuccess: () => {
      setNewTodoTitle("");
      // Note: We deliberately DO NOT invalidate the 'todos' query here.
      // The backend will broadcast a NATS event which our SSE hook will catch
      // and patch the cache automatically. This is true Event-Driven UI.
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTodoTitle.trim()) return;
    createMutation.mutate({ title: newTodoTitle });
  };

  return (
    <div className="min-h-screen bg-background text-foreground p-8 flex justify-center">
      <div className="w-full max-w-2xl space-y-8">
        <header className="space-y-2">
          <h1 className="text-4xl font-bold tracking-tight">
            Death Star Dashboard
          </h1>
          <p className="text-muted-foreground">
            Powered by Virtual Threads, NATS, and React Query SSE Cache
            Patching.
          </p>
        </header>

        <Card>
          <CardHeader>
            <CardTitle>Mission Objectives</CardTitle>
            <CardDescription>
              Real-time synchronized across all instances.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {/* Creation Form */}
            <form onSubmit={handleSubmit} className="flex gap-4">
              <input
                type="text"
                value={newTodoTitle}
                onChange={(e) => setNewTodoTitle(e.target.value)}
                placeholder="Enter new objective..."
                className="flex-1 rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                disabled={createMutation.isPending}
              />
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? "Deploying..." : "Add Objective"}
              </Button>
            </form>

            {/* List Rendering */}
            <div className="space-y-4">
              {isLoading && (
                <p className="text-sm text-muted-foreground">
                  Loading objectives...
                </p>
              )}
              {isError && (
                <p className="text-sm text-destructive">
                  Failed to load objectives.
                </p>
              )}

              {todos?.length === 0 && !isLoading && (
                <p className="text-sm text-muted-foreground">
                  No objectives assigned.
                </p>
              )}

              <ul className="space-y-3">
                {todos?.map((todo) => (
                  <li
                    key={todo.id}
                    className="flex items-center justify-between p-4 rounded-lg border bg-card text-card-foreground shadow-sm transition-all hover:shadow-md"
                  >
                    <span
                      className={
                        todo.completed
                          ? "line-through text-muted-foreground"
                          : ""
                      }
                    >
                      {todo.title}
                    </span>
                    {/* Timestamp helps verify the event-driven ordering visually */}
                    <span className="text-xs text-muted-foreground">
                      {new Date(todo.createdAt).toLocaleTimeString()}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
