import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Merges Tailwind CSS classes, resolving conflicts optimally.
 * This is the foundational utility for all Shadcn UI components.
 * * @param inputs - An array of class values (strings, objects, arrays)
 * @returns A strictly merged Tailwind class string
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
