import type { paths, components } from "@death-star/holocron";

type TodosResponse =
  paths["/todos"]["get"]["responses"]["200"]["content"]["application/json"];
type CreateTodoRequest =
  paths["/todos"]["post"]["requestBody"]["content"]["application/json"];

// FIX: components are at the root level of the generated types, not under paths.
type Todo = components["schemas"]["Todo"];

/**
 * A lightweight, typesafe HTTP client interacting with the Vader backend.
 * Uses native fetch API. Proxied via Vite during local development.
 */
export const apiClient = {
  getTodos: async (): Promise<TodosResponse> => {
    const response = await fetch("/api/v1/todos");
    if (!response.ok) {
      throw new Error("Failed to fetch todos");
    }
    return response.json();
  },

  createTodo: async (payload: CreateTodoRequest): Promise<Todo> => {
    const response = await fetch("/api/v1/todos", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error("Failed to create todo");
    }
    return response.json();
  },
};
