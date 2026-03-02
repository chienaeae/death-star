# Millennium

The **Millennium** subproject (`@death-star/millennium`) provides the shared UI component library for the Death Star monorepo. It serves as the single source of truth for the core design system and reusable presentational components.

## Overview

- **UI Components**: Consistent, accessible base components built on top of Radix UI primitives. These components abstract common layout and interactive patterns into easy-to-use building blocks (e.g., Buttons, Cards).
- **Styling Utilities**: Contains standard utilities, powered by Tailwind CSS and `class-variance-authority`, for strongly-typed dynamic CSS class merging.
- **Design Tokens**: Exposes the global design variables and foundational layouts through central CSS files that consumer applications inherit.

Millennium does not handle complex application logic, data fetching, or heavy state management. Rather, it focuses strictly on rendering responsive, standard, and beautiful UI elements that applications (like Skywalker or Vader) can consistently utilize.
