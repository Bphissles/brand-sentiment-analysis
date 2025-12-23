---
description: Execute code review recommendations across all project layers
---

# Execute Code Review Recommendations

This workflow processes code review recommendations from each layer's `CODE_REVIEW_LOG.md` in dependency order, then performs cross-service integration review.

## Pre-requisites

- Read the root `AGENTS.md` for overall project context
- Ensure you understand the mono-repo structure: `ml-engine/`, `backend/`, `frontend/`

---

## Phase 1: ML Engine (Foundation Layer)

The ML engine has no internal dependencies on other layers—fix it first.

1. Read `ml-engine/AGENTS.md` for coding conventions and testing guidelines
2. Read `ml-engine/CODE_REVIEW_LOG.md` to get the list of recommendations
3. Process items in priority order: **Blockers → High → Medium → Low**
4. For each item:
   - Read the referenced file(s) and line ranges
   - Implement the fix following `ml-engine/AGENTS.md` conventions
   - Run `cd ml-engine && source venv/bin/activate && pytest` to verify no regressions
5. After completing all ML engine fixes, update `ml-engine/CODE_REVIEW_LOG.md`:
   - Mark completed items with `[x]` 
   - Add a dated completion note at the bottom

---

## Phase 2: Backend (API Layer)

The backend depends on ML engine responses—fix it after ML engine.

1. Read `backend/AGENTS.md` for Grails/Groovy conventions and testing guidelines
2. Read `backend/CODE_REVIEW_LOG.md` to get the list of recommendations
3. Process items in priority order: **Blockers → High → Medium → Low**
4. For each item:
   - Read the referenced file(s) and line ranges
   - Implement the fix following `backend/AGENTS.md` conventions
   - Run `cd backend && ./gradlew test` to verify no regressions
5. After completing all backend fixes, update `backend/CODE_REVIEW_LOG.md`:
   - Mark completed items with `[x]`
   - Add a dated completion note at the bottom

---

## Phase 3: Frontend (UI Layer)

The frontend depends on backend API contracts—fix it last among service layers.

1. Read `frontend/AGENTS.md` for TypeScript/Vue conventions and testing guidelines
2. Read `frontend/CODE_REVIEW_LOG.md` to get the list of recommendations
3. Process items in priority order: **Blockers → High → Medium → Low**
4. For each item:
   - Read the referenced file(s) and line ranges
   - Implement the fix following `frontend/AGENTS.md` conventions
   - Run `cd frontend && npm test` to verify no regressions
5. After completing all frontend fixes, update `frontend/CODE_REVIEW_LOG.md`:
   - Mark completed items with `[x]`
   - Add a dated completion note at the bottom

---

## Phase 4: Cross-Service Integration Review

After all layer-specific fixes are complete, perform integration review.

1. Read `CODE_REVIEW.md` at the repo root for the cross-service review guide
2. Review the changes made in Phases 1-3 for cross-cutting concerns:
   - API contract alignment (FE ↔ BE ↔ ML)
   - Auth propagation and protection
   - Failure modes and fallbacks
   - Data model and persistence alignment
   - Config and environment consistency
3. Document any integration risks found

---

## Phase 5: Update Root Code Review Log

1. Read `CODE_REVIEW_LOG.md` at the repo root
2. Append a dated entry summarizing:
   - Cross-cutting summary of changes made
   - Any integration risks identified (Blocker/High/Medium/Low)
   - Contracts verified
   - Required follow-ups (if any)
   - Or: "No cross-cutting integration risks detected."

---

## Verification

After all phases complete:

// turbo
1. Run full test suite: `./start-local.sh` to verify services start correctly
2. Run backend tests: `cd backend && ./gradlew test`
3. Run frontend tests: `cd frontend && npm test`
4. Run ML engine tests: `cd ml-engine && source venv/bin/activate && pytest`

---

## Notes

- **Priority order matters**: Always fix Blockers before High, High before Medium, etc.
- **Test after each fix**: Don't batch fixes without testing—catch regressions early
- **Follow AGENTS.md**: Each layer has specific conventions; respect them
- **Update logs**: Mark items complete as you go to track progress
