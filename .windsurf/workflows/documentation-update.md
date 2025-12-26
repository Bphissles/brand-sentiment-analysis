---
description: Validate and align all project documentation with current implementation
---

# Documentation Housekeeping Workflow

**SOURCE OF TRUTH: The implementation (code, config files, actual structure) is always correct. Documentation must be updated to match reality.**

This workflow systematically audits all documentation files and **automatically updates them** to reflect the current project structure, implementation, and configuration. Run this periodically or after major changes.

## Core Principle

When discrepancies are found between documentation and implementation:
- ✅ **Update the documentation** to match the code
- ❌ **Never assume documentation is correct** and code is wrong
- 📝 **Make changes immediately** - don't just note them for later

## Pre-requisites

- All services should be stopped before starting
- Have access to the project root directory
- Understand the mono-repo structure: `ml-engine/`, `backend/`, `frontend/`

---

## Phase 1: Project Structure Validation

**Goal: Update documented structure to match actual filesystem.**

1. **Read actual directory structure**
   - Use `list_dir` and `find_by_name` tools to explore the project
   - Note all top-level directories and key subdirectories

2. **Read documented structure from:**
   - `README.md` (lines ~50-61) - Project Structure section
   - `SPRINTS.md` (lines ~445-514) - Quick Reference: Project Structure

3. **Update documentation immediately:**
   - If directories are missing from docs: Add them with descriptions
   - If documented directories don't exist: Remove them
   - If directory purposes changed: Update descriptions
   - Ensure both `README.md` and `SPRINTS.md` match actual structure
   - Update `docs/ARCHITECTURE.md` diagrams if structure changed significantly

---

## Phase 2: Dependency Validation

**Goal: Update documented dependencies to match actual package files.**

### Frontend Dependencies

1. **Read actual dependencies:**
   - Read `frontend/package.json`
   - Extract all dependencies and devDependencies
   - Note versions for key packages (nuxt, d3, tailwindcss, etc.)

2. **Update documentation:**
   - Update `SPRINTS.md` (lines ~532-541) with actual dependency list
   - Update `frontend/README.md` if it lists dependencies
   - Ensure versions match or use `^X.x` format for flexibility

### Backend Dependencies

1. **Read actual dependencies:**
   - Read `backend/build.gradle`
   - Extract all `implementation` and `runtimeOnly` dependencies
   - Note versions for: Grails, jjwt-api, springdoc-openapi-ui, postgresql

2. **Update documentation:**
   - Update `SPRINTS.md` (lines ~544-551) with actual dependency list
   - Update `backend/README.md` if it lists dependencies
   - Include comments about Spring Boot 2.x compatibility if relevant

### ML Engine Dependencies

1. **Read actual dependencies:**
   - Read `ml-engine/requirements.txt`
   - Note all packages and versions
   - Verify `flasgger` is present for API docs

2. **Update documentation:**
   - Update `SPRINTS.md` (lines ~554-563) with actual dependency list
   - Update `ml-engine/README.md` if it lists dependencies
   - Ensure all packages are documented

---

## Phase 3: API Endpoint Validation

**Goal: Update endpoint documentation to match actual controllers.**

### Backend API Endpoints

1. **Read all controller files:**
   - Read each controller in `backend/grails-app/controllers/sentiment/`
   - For each controller method, note: HTTP method, path, description, auth requirement
   - Read `backend/grails-app/controllers/backend/UrlMappings.groovy` for path mappings

2. **Read OpenAPI spec:**
   - Read `backend/src/main/groovy/backend/config/OpenApiConfig.groovy`
   - Extract all paths from `buildPaths()` method

3. **Update documentation immediately:**
   - Update `README.md` (lines ~184-203) endpoint table
   - Update `docs/API_DOCUMENTATION.md` (lines ~34-85) endpoint tables
   - Update `backend/README.md` endpoint section
   - Add any new endpoints discovered
   - Remove any endpoints that no longer exist
   - Fix HTTP methods, paths, or descriptions that are wrong

### ML Engine API Endpoints

1. **Read Flask routes:**
   - Read `ml-engine/app/api.py`
   - Extract all `@app.route()` definitions
   - Note: path, HTTP methods, description from docstring

2. **Update documentation immediately:**
   - Update `README.md` (lines ~205-211) endpoint table
   - Update `docs/API_DOCUMENTATION.md` (lines ~24-30) endpoint table
   - Ensure all routes are documented
   - Fix any incorrect paths or methods

---

## Phase 4: Configuration Validation

**Goal: Update configuration documentation to match actual config files.**

### Application Configuration

1. **Backend configuration:**
   - Read `backend/grails-app/conf/application.yml`
   - Extract: springdoc paths, Gemini config, ML Engine URL, JWT settings, Supabase connection
   - **Update immediately:**
     - `docs/API_DOCUMENTATION.md` (lines ~154-163) - Backend configuration section
     - `backend/README.md` - Configuration examples
     - Fix any incorrect paths, URLs, or settings

2. **ML Engine configuration:**
   - Read `ml-engine/app/api.py` for swagger_config and swagger_template
   - **Update immediately:**
     - `docs/API_DOCUMENTATION.md` (lines ~148-151) - ML Engine configuration section
     - Fix Swagger UI path, spec route, or metadata

3. **Frontend configuration:**
   - Read `frontend/nuxt.config.ts`
   - Extract API URL and other runtime config
   - **Update immediately:**
     - `README.md` (lines ~162-165) - Environment Variables section
     - Fix any incorrect environment variable names or values

### Environment Variables

1. **Read all `.env.example` files:**
   - Root `.env.example`
   - `ml-engine/.env.example`
   - `frontend/.env.example`
   - Extract all variable names and example values

2. **Update documentation immediately:**
   - Update `README.md` (lines ~160-178) with all required env vars
   - Update each layer's README with layer-specific vars
   - Add any new variables discovered
   - Remove variables that are no longer used

---

## Phase 5: Feature Status Validation

**Goal: Update sprint task statuses to match actual implementation.**

1. **For each sprint in `SPRINTS.md`:**
   - Sprint 0: Foundation (lines ~42-88)
   - Sprint 1: Data Pipeline (lines ~92-136)
   - Sprint 2: ML Engine (lines ~140-186)
   - Sprint 3: Grails API (lines ~189-235)
   - Sprint 4: D3.js Visualization (lines ~239-284)
   - Sprint 4.5: Testing (lines ~288-325)
   - Sprint 5: Dashboard UI (lines ~329-388)
   - Sprint 6: Deploy (lines ~392-439)

2. **For each task marked `[x]`:**
   - Verify the implementation actually exists (check files, features, endpoints)
   - Verify deliverables are present
   - **If not implemented:** Change `[x]` to `[ ]` immediately

3. **For each task marked `[ ]`:**
   - Check if it's actually been implemented
   - **If implemented:** Change `[ ]` to `[x]` immediately
   - Update any task descriptions that are inaccurate

4. **Update `README.md` features list** (lines ~18-31):
   - Add any new features that have been implemented
   - Remove or update features that are inaccurate
   - Ensure checkmarks match sprint completion status

---

## Phase 6: Documentation Cross-References

Verify all internal documentation links are valid.

1. **Check links in `README.md`** (lines ~216-230):
   - `SPRINTS.md` exists ✓
   - `docs/ARCHITECTURE.md` exists ✓
   - `docs/API_DOCUMENTATION.md` exists ✓
   - `TESTING.md` exists ✓
   - `TESTING_STATUS.md` exists (verify)
   - `IMPLEMENTATION_SUMMARY.md` exists (verify)
   - `CODE_REVIEW_v2.md` exists (verify)
   - `config/taxonomy.yaml` exists ✓

2. **Check Swagger UI URLs are correct:**
   - ML Engine: http://localhost:5000/apidocs/
   - Backend: http://localhost:8080/swagger-ui/index.html (NOT `/swagger-ui.html`)

3. **Verify layer-specific README references:**
   - Each layer's `AGENTS.md` referenced correctly
   - Each layer's `CODE_REVIEW.md` and `CODE_REVIEW_LOG.md` exist

---

## Phase 7: Tech Stack Alignment

Verify the documented tech stack matches implementation.

1. **Read tech stack table in:**
   - `README.md` (lines ~35-43)
   - `SPRINTS.md` (lines ~13-21)

2. **Verify each technology:**
   - **Frontend**: Nuxt 3 (check `frontend/package.json`)
   - **Backend**: Grails 6 (check `backend/build.gradle`)
   - **ML Engine**: Python + Flask (check `ml-engine/requirements.txt`)
   - **Auth**: Custom JWT with `jjwt` (check `backend/build.gradle`)
   - **Database**: PostgreSQL via Supabase (check `backend/grails-app/conf/application.yml`)
   - **AI**: Gemini 2.0 Flash (check service files)

3. **Update if technologies have changed**

---

## Phase 8: Testing Documentation

Verify testing documentation matches actual test setup.

1. **Read `TESTING.md` for documented test commands**

2. **Verify test frameworks:**
   - Backend: Spock (check `backend/build.gradle` and `backend/src/test/`)
   - Frontend: Vitest (check `frontend/package.json` and `frontend/test/`)
   - ML Engine: pytest (check `ml-engine/requirements.txt` and `ml-engine/tests/`)

3. **Verify test execution scripts:**
   - `backend/test.sh` exists and is documented
   - Test commands in each layer's README are correct

4. **Check coverage configuration:**
   - Jacoco for backend
   - Vitest coverage for frontend
   - pytest-cov for ML engine

---

## Phase 9: Startup Scripts Validation

Verify startup/shutdown scripts match documentation.

1. **Check scripts exist:**
   - `./start-local.sh` ✓
   - `./stop-local.sh` ✓

2. **Verify documented ports:**
   - Frontend: 3000
   - Backend: 8080
   - ML Engine: 5000

3. **Test scripts work:**
   ```bash
   ./stop-local.sh
   ./start-local.sh
   # Verify all services start successfully
   ./stop-local.sh
   ```

4. **Update documentation if ports or behavior changed**

---

## Phase 10: Final Consistency Check

**Goal: Ensure all documentation updates are consistent across files.**

1. **Review all changes made in Phases 1-9:**
   - List which files were updated
   - Note what was changed in each file

2. **Check for cross-file consistency:**
   - If you updated an endpoint in `README.md`, did you also update `docs/API_DOCUMENTATION.md`?
   - If you updated dependencies in `SPRINTS.md`, did you update layer READMEs?
   - If you updated project structure, did you update both `README.md` and `SPRINTS.md`?

3. **Make final consistency updates:**
   - Fix any remaining inconsistencies between files
   - Ensure terminology is consistent (e.g., "Swagger UI" path is same everywhere)
   - Verify all cross-references still work

4. **Create a summary:**
   - List all files that were updated
   - Briefly describe what was changed in each
   - Note any significant discrepancies that were found

---

## Verification Checklist

After completing all phases, verify you have:

- [ ] Updated project structure diagrams to match filesystem
- [ ] Updated all dependency lists to match package files
- [ ] Updated API endpoint tables to match controllers
- [ ] Updated configuration examples to match actual config files
- [ ] Updated sprint task statuses (`[x]` or `[ ]`) to reflect reality
- [ ] Fixed all broken internal links
- [ ] Updated tech stack tables to match implementation
- [ ] Updated testing documentation to match test setup
- [ ] Verified startup script documentation is accurate
- [ ] Fixed all Swagger UI URLs to correct paths

---

## Notes

- **Run this workflow quarterly** or after major feature additions
- **Implementation is truth**: Code, config files, and actual structure are always correct
- **Update immediately**: Don't defer updates - make changes as you discover discrepancies
- **Update atomically**: Complete one phase before moving to the next
- **Be thorough**: Check every file mentioned, don't skip steps
- **Version control**: Commit documentation updates with clear, descriptive messages

---

## Common Issues to Watch For

1. **Outdated API endpoints** - Controllers added/removed but docs not updated
2. **Wrong Swagger UI paths** - `/swagger-ui.html` vs `/swagger-ui/index.html`
3. **Dependency version drift** - Package versions updated but docs show old versions
4. **Missing new features** - Features implemented but not added to README
5. **Broken internal links** - Files moved/renamed but links not updated
6. **Sprint status lag** - Tasks completed but still marked `[ ]` in SPRINTS.md
7. **Port conflicts** - Services moved to different ports but docs show old ports
8. **Environment variables** - New env vars added but not documented in .env.example
