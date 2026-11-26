import js from "@eslint/js";
import tseslint from "typescript-eslint";
import reactHooks from "eslint-plugin-react-hooks";
import jsxA11y from "eslint-plugin-jsx-a11y";
import globals from "globals";

export default tseslint.config(
  { ignores: ["dist", "node_modules"] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  reactHooks.configs.flat.recommended,
  // Barrierefreiheit schon beim Schreiben prüfen: Beschriftungen, Rollen, Tastaturbedienung
  jsxA11y.flatConfigs.strict,
  {
    files: ["**/*.{ts,tsx}"],
    languageOptions: { globals: globals.browser },
    rules: {
      // Breite Tabellen scrollen in einem benannten Bereich (role="region"); der muss per Tastatur
      // erreichbar sein, damit man ihn mit den Pfeiltasten scrollen kann (axe: scrollable-region-focusable).
      "jsx-a11y/no-noninteractive-tabindex": ["error", { tags: [], roles: ["tabpanel", "region"] }],
    },
  },
);
