import sharedConfig from "@death-star/millennium/tailwind.config.js";

/** @type {import('tailwindcss').Config} */
export default {
  presets: [sharedConfig],
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
    "../millennium/src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      // Base design tokens can be extended here
    },
  },
  plugins: [],
};
