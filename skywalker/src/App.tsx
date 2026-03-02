import type React from "react";
import { useEffect, useState } from "react";
import { useQueryClient, useQuery } from "@tanstack/react-query";
import { apiClient } from "./api/client";
import { useServerEvents } from "./hooks/useServerEvents";
import type { components } from "@death-star/holocron";

type Todo = components["schemas"]["Todo"];

// --- First Principles: Auth State Machine Definition ---
type AuthState = "PENDING" | "AUTHENTICATED" | "UNAUTHENTICATED";

export default function App() {
  const [authState, setAuthState] = useState<AuthState>("PENDING");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errorMsg, setErrorMsg] = useState("");

  // --- Initialization Flow (Init Container Pattern) ---
  useEffect(() => {
    const initApp = async () => {
      // Silent Renewal: On F5 refresh, automatically sends HttpOnly Cookie to fetch in-memory Access Token
      const success = await apiClient.hydrateSession();
      setAuthState(success ? "AUTHENTICATED" : "UNAUTHENTICATED");
    };
    initApp();

    // Listen for cross-tab logout/sync signals (BroadcastChannel)
    const handleAuthSync = (e: MessageEvent) => {
      if (e.data.type === "SESSION_TERMINATED") {
        setAuthState("UNAUTHENTICATED");
      } else if (e.data.type === "TOKEN_REFRESHED") {
        setAuthState("AUTHENTICATED");
      }
    };
    const channel = new BroadcastChannel("auth_sync_channel");
    channel.addEventListener("message", handleAuthSync);

    return () => channel.removeEventListener("message", handleAuthSync);
  }, []);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg("");
    try {
      await apiClient.login({ email, password });
      setAuthState("AUTHENTICATED");
    } catch (err: any) {
      setErrorMsg(err.message || "Login failed");
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg("");
    try {
      await apiClient.register({ email, password });
      setAuthState("AUTHENTICATED");
    } catch (err: any) {
      setErrorMsg(err.message || "Registration failed");
    }
  };

  const handleLogout = async () => {
    try {
      await apiClient.logout();
    } finally {
      setAuthState("UNAUTHENTICATED");
    }
  };

  // --- Blocking Render ---
  if (authState === "PENDING") {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-gray-950">
        <div className="text-white text-xl animate-pulse">
          Initializing Death Star Systems...
        </div>
      </div>
    );
  }

  if (authState === "UNAUTHENTICATED") {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-gray-950">
        <div className="w-full max-w-md p-8 bg-gray-900 border border-gray-800 rounded-xl shadow-2xl">
          <h2 className="text-2xl font-bold text-white mb-6">
            IAM Access Portal
          </h2>
          {errorMsg && (
            <div className="mb-4 p-3 bg-red-900/50 border border-red-500 text-red-200 rounded text-sm">
              {errorMsg}
            </div>
          )}
          <form className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-400 mb-1">
                Clearance Code (Email)
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded text-white focus:outline-none focus:border-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-400 mb-1">
                Passphrase
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded text-white focus:outline-none focus:border-blue-500"
              />
            </div>
            <div className="flex gap-4 pt-4">
              <button
                onClick={handleLogin}
                className="flex-1 bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded transition-colors"
              >
                Authenticate
              </button>
              <button
                onClick={handleRegister}
                className="flex-1 bg-gray-700 hover:bg-gray-600 text-white font-medium py-2 px-4 rounded transition-colors"
              >
                Request Access
              </button>
            </div>
          </form>
        </div>
      </div>
    );
  }

  // --- Authenticated Zone Business Logic ---
  return <AuthenticatedDashboard onLogout={handleLogout} />;
}

function AuthenticatedDashboard({ onLogout }: { onLogout: () => void }) {
  const queryClient = useQueryClient();
  const [newTodo, setNewTodo] = useState("");

  // Start SSE listener (Underlying customFetch is grouped with the token state machine)
  useServerEvents();

  const { data: todos, isLoading } = useQuery({
    queryKey: ["todos"],
    queryFn: () => apiClient.getTodos(),
  });

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTodo.trim()) return;
    try {
      await apiClient.createTodo({ title: newTodo });
      setNewTodo("");
    } catch (err) {
      console.error("Failed to create todo", err);
    }
  };

  return (
    <div className="min-h-screen bg-gray-950 text-gray-300 p-8">
      <div className="max-w-4xl mx-auto">
        <div className="flex justify-between items-center mb-8 pb-4 border-b border-gray-800">
          <h1 className="text-3xl font-bold text-white">
            Death Star Objectives
          </h1>
          <button
            onClick={onLogout}
            className="px-4 py-2 text-sm font-medium text-red-400 hover:text-red-300 border border-red-900/50 hover:bg-red-900/20 rounded transition-colors"
          >
            Terminate Session
          </button>
        </div>

        <form onSubmit={handleCreate} className="mb-8 flex gap-4">
          <input
            type="text"
            value={newTodo}
            onChange={(e) => setNewTodo(e.target.value)}
            placeholder="Assign new objective..."
            className="flex-1 px-4 py-3 bg-gray-900 border border-gray-800 rounded-lg text-white focus:outline-none focus:border-blue-500"
          />
          <button
            type="submit"
            className="px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg transition-colors"
          >
            Deploy
          </button>
        </form>

        <div className="space-y-4">
          {isLoading && (
            <p className="text-gray-500 animate-pulse">Scanning database...</p>
          )}
          {todos?.length === 0 && !isLoading && (
            <p className="text-gray-500">No active objectives.</p>
          )}

          <ul className="space-y-3">
            {todos?.map((todo) => (
              <li
                key={todo.id}
                className="flex items-center justify-between p-4 bg-gray-900 border border-gray-800 rounded-lg shadow-sm"
              >
                <span
                  className={`text-lg ${todo.completed ? "line-through text-gray-600" : "text-gray-200"}`}
                >
                  {todo.title}
                </span>
                <span className="text-xs text-gray-600">
                  {/* Defensive programming: Ensure Date parsing does not crash due to undefined */}
                  {new Date(todo.createdAt ?? Date.now()).toLocaleString()}
                </span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
