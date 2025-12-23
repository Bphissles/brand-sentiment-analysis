# Repository Guidelines

## Project Structure & Modules
- `app.vue`, `pages/` – Nuxt 4 app shell and route views.
- `components/` – Reusable UI components (`PascalCase.vue`).
- `composables/` – Shared logic as `useX.ts` composables.
- `assets/` and `public/` – Static assets and public files.
- `tests/` – Vitest specs, setup, and component/composable tests.

## Build, Test, and Development
- `npm install` – Install frontend dependencies.
- `npm run dev` – Start Nuxt dev server (typically on `localhost:3000`).
- `npm run build` – Production build.
- `npm run preview` – Preview the production build locally.
- `npm run test` / `test:watch` – Run Vitest suite (once / watch mode).
- `npm run test:coverage` – Run tests with coverage reporting.

## Coding Style & Naming
- Use TypeScript, ES modules, and Vue 3 `<script setup>` where practical.
- Indentation: 2 spaces, no tabs; prefer single quotes in TS/JS.
- Components: `PascalCase.vue` in `components/`; composables: `useName.ts`.
- Keep styling via Tailwind utility classes where possible; avoid inline styles.

## Testing Guidelines
- Framework: Vitest with `@vue/test-utils` and a DOM environment.
- Place component tests under `tests/components/` and composable tests under `tests/composables/`.
- Name files `*.spec.ts` and keep assertions focused and deterministic.
- For new features, add or update tests and ensure `npm run test` passes before pushing.

## Commit & Pull Request Guidelines
- Write clear, imperative commit messages (e.g., `Add sentiment filtering to clusters`).
- Group related changes into a single commit or small logical series.
- For PRs, include: a short summary, motivation/linked issues, testing notes (commands run), and screenshots/GIFs for UI changes.
- Keep PRs small and focused; refactors and feature changes should be submitted separately when feasible.

## Agent-Specific Notes
- Respect this file’s instructions when modifying code or tests.
- Prefer minimal, targeted changes that preserve existing architecture, tooling, and conventions.

