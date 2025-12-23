# Code Review Log

## 2025-12-23 — Full frontend audit vs CODE_REVIEW checklist

- [ ] Medium: Type `$fetch` responses in `useApi` and related composables for stronger TS safety (`composables/useApi.ts`)
- [ ] Medium: Replace `any` state in pages with typed view models where possible (`pages/index.vue`, `pages/data.vue`)
- [ ] Medium: Add targeted Vitest tests for critical composables (`useAuth`, `useServiceHealth`, `useColorMode`) and dashboard/login flows (`tests/composables`, `tests/components`, `tests/pages`)
- [ ] Low: Consider extracting `DashboardSummary` view model interfaces to reuse instead of `any` (`types/models.ts`, `pages/index.vue`)
- [ ] Low: Refine auth middleware / composable interaction to avoid duplicate localStorage checks and keep a single source of truth (`middleware/auth.global.ts`, `composables/useAuth.ts`)
