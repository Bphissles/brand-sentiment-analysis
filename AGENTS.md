# Repository Guidelines

## Project Structure & Module Organization
- `backend/` – Grails 6 API (Java 17, Spock tests).
- `frontend/` – Nuxt 3 dashboard (TypeScript, Tailwind, Vitest).
- `ml-engine/` – Python Flask ML service (scikit-learn, NLTK, pytest).
- `config/` – Taxonomy and other auditable configuration (`taxonomy.yaml`).
- `data/` – Local fixtures and sample input data.
- `docs/` – Architecture, testing, and review documents.
- `start-local.sh` / `stop-local.sh` – Unified scripts to run and stop all services.

## Build, Test, and Development Commands
- Local stack: `./start-local.sh` (frontend 3000, backend 8080, ML 5000), `./stop-local.sh` to shut down.
- Backend: `cd backend && ./test.sh` for CI-like tests; `./gradlew bootRun` to run the API.
- Frontend: `cd frontend && npm install && npm run dev` for local dev; `npm test` / `npm run test:coverage` for Vitest.
- ML Engine: `cd ml-engine && source venv/bin/activate && pytest` to run tests; `python app/api.py` for the API.
- See `TESTING.md` for detailed test commands and coverage options.

## Coding Style & Naming Conventions
- Backend: Groovy/Java conventions; `UpperCamelCase` classes, `lowerCamelCase` methods/fields; 4-space indentation.
- Frontend: TypeScript + Vue 3 with `<script setup>`; 2-space indentation; `PascalCase.vue` components, `useName.ts` composables.
- ML Engine: Python 3 with type hints; `snake_case` functions/variables, `PascalCase` classes, 4-space indentation.
- Follow the more detailed `AGENTS.md` in each subdirectory when editing code there.

## Testing Guidelines
- Frameworks: Spock/JUnit (backend), Vitest (frontend), pytest (ML engine).
- Add or update tests alongside any behavior change; mirror existing file and package naming patterns.
- Before opening a PR, run the relevant suites (ideally all three layers) as described in `TESTING.md`.

## Commit & Pull Request Guidelines
- Commits: short, imperative subject lines that describe the change (e.g., `Improve sentiment clustering for short posts`), with an optional body explaining why.
- Group related changes into focused commits; avoid mixing refactors with new features.
- PRs should include a clear summary, linked issues or tickets, testing notes (commands run), and screenshots/GIFs for UI-facing changes.
- Keep PRs small and incremental; prefer follow-up PRs for large refactors or unrelated cleanup.

## Security & Configuration
- Never commit secrets; use `.env` files and local environment variables.
- Treat `config/` as auditable configuration—coordinate changes with maintainers and document impactful updates in PR descriptions.

