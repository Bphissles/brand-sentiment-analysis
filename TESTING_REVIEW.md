# Testing Review – Peterbilt Sentiment Analyzer

Date: 2025-12-23  
Reviewer: Senior Engineer (AI assistant)

---

## 1. Current Testing Landscape

Based on the repository and `TESTING.md`:

- **Backend (Grails/Spock)**
  - Harness is configured with:
    - Spock + Grails testing support.
    - `jacoco` for coverage, wired into `test` via `finalizedBy jacocoTestReport`.
  - A working test wrapper script exists: `backend/test.sh` (enforces Java 17).
  - Actual committed tests are minimal:
    - `backend/src/test/groovy/sentiment/SampleSpec.groovy` is present as a smoke test.

- **Frontend (Nuxt/Vitest)**
  - Vitest is configured with:
    - `frontend/tests/setup.ts` stubbing Nuxt/DOM globals and `$fetch`.
  - Tests include:
    - `tests/components/LoadingScreen.spec.ts` (good coverage of that component’s props and conditional rendering).
    - `tests/composables/useApi.spec.ts` (basic harness checks).
    - `tests/sample.spec.ts` (sanity checks).

- **ML Engine (Python/pytest)**
  - `pytest.ini` defines a clear structure, markers, and coverage options.
  - `TESTING.md` documents a full test suite (`tests/test_preprocessing.py`, `test_sentiment.py`, `test_clustering.py`, `test_api.py`), but these files are not present in the current tree.

Conclusion: **tooling and documentation are ahead of actual test depth**. The system is ready for more comprehensive tests; now you should focus on growing coverage around critical paths and contracts.

---

## 2. Backend Testing Recommendations

### 2.1 Test Strategy

Adopt a layered testing approach:

- **Unit tests (Spock)**
  - Focus on services (`AuthService`, `MlEngineService`, `GeminiService`, `DataLoaderService`).
  - Use mocks/stubs for network and external dependencies.
- **Web/controller tests**
  - Use `grails.testing.web.controllers.ControllerUnitTest` and `grails.testing.web.interceptor.InterceptorUnitTest`.
  - Validate HTTP status codes, JSON response shapes, and auth behavior.
- **Integration tests**
  - For end-to-end flows, especially:
    - Fixture loading → ML analysis → cluster summary.
    - Auth + protected endpoints.

### 2.2 High-Value Test Cases

**AuthService**

- `getSigningKey`:
  - Fails fast when `jwt.secret` is missing in non-`DEVELOPMENT` environments.
  - Uses default secret and logs a warning in `DEVELOPMENT`.
- `authenticate`:
  - Returns `null` for disabled/nonexistent users.
  - Updates `lastLoginAt` and returns user on valid credentials.
- `register`:
  - Creates user with hashed password and correct role.
  - Returns `null` when email already exists.

**AuthInterceptor**

- Public endpoints:
  - `/api/auth/login`, `/api/auth/register`, `/api/health` bypass JWT checks.
- Protected endpoints:
  - Missing/invalid `Authorization` header → `401`.
  - Valid JWT → request proceeds; attributes `userEmail`, `userRole`, `userId` set.
- Admin-only endpoints:
  - Non-admin JWT → `403` for `/api/analysis/clear`, `/api/analysis/load-fixtures`, `/api/ingestion/scrapeAll`, `/api/ingestion/scrape`.
- Development mode:
  - In `DEVELOPMENT` without token, request passes with default dev user attributes and a warning logged.

**AnalysisController.trigger**

- No posts:
  - Creates `AnalysisRun` with `completed` status, zero counts, and returns a friendly “No posts to analyze” message.
- ML success:
  - Calls `MlEngineService.analyzePostsForClusters`.
  - Clears old clusters and resets `clusterId` only when `result.success == true`.
  - Persists clusters with correct fields, updates corresponding posts with cluster IDs and sentiment data.
  - Updates `AnalysisRun` with `completed`, `postsAnalyzed`, `clustersCreated`, and `durationMs`.
- ML failure:
  - Does not clear existing clusters.
  - Marks `AnalysisRun` as `failed` and returns `500` with error details.

**MlEngineService**

- Happy path:
  - Given a mocked ML engine returning a valid JSON payload, the service returns `success: true` and the parsed fields.
- Network/connection errors:
  - Simulate `ConnectException` and assert returned `{ success: false, error: "Cannot connect to ML Engine..." }`.
- Non-200 responses:
  - Mock a 500 or 400 response and assert error messages contain response code and body.

### 2.3 Coverage Targets

- **Short term**:
  - 60%+ coverage on services and controllers most tied to business logic (Auth, Analysis, ML integration).
- **Medium term**:
  - 80%+ on core services; 60–70% on controllers overall.

---

## 3. Frontend Testing Recommendations

### 3.1 Test Strategy

Use Vitest and Vue Test Utils to focus on:

- **Composables**: `useAuth`, `useApi`, and any others that encapsulate logic.
- **Key components**: Data-heavy views (`ClusterDetail`, bubble chart container, data management page).
- **Basic routing/guard behavior**: Via Nuxt route middleware when introduced.

### 3.2 High-Value Test Cases

**useAuth**

- `login`:
  - Mocks `$fetch` to return `{ success: true, token, user }`; asserts:
    - `authState` is updated.
    - `localStorage` entries are set.
  - Error paths:
    - `$fetch` rejects or returns `success: false`; `authState` remains unauthenticated; error message is propagated.
- `register`:
  - Mirrors `login` tests but ensures role defaults correctly to `viewer`.
- `logout`:
  - Clears `authState` and removes items from `localStorage`.
- `fetchCurrentUser`:
  - With token: `$fetch` returns user; state is updated.
  - With invalid token: `$fetch` throws; `logout` is called; state cleared.
- `getAuthHeader`:
  - With token: returns `{ Authorization: 'Bearer <token>' }`.
  - Without token: returns `{}`.

**useApi**

- Each method:
  - Mocks `$fetch` and asserts:
    - Correct URL and HTTP method.
    - `getAuthHeader()` is used on protected endpoints.
    - Response is transformed appropriately (e.g., `fetchClusters` returns `response.clusters || []`).
- Error handling:
  - Simulate a 401/403 and ensure the calling code can respond appropriately (e.g., surface error or trigger a logout in higher-level logic).

**Components**

- `LoadingScreen` (already tested; keep as reference).
- `ClusterDetail`:
  - Renders cluster metadata, posts list, and sentiment badges given props.
  - Handles empty posts gracefully.
- Bubble chart container (not necessarily the D3 internals):
  - Given a small set of clusters, ensures the SVG is rendered, click handlers emit `clusterClick`, and responsive sizing logic doesn’t crash.

### 3.3 Coverage Targets

- **Short term**:
  - 60%+ coverage on composables (`useAuth`, `useApi`).
- **Medium term**:
  - 70%+ coverage for composables; 50–60% for components.

---

## 4. ML Engine Testing Recommendations

### 4.1 Restore / Commit the Test Suite

The `TESTING.md` file documents a full pytest suite, but the `tests/` directory is currently absent. First priority:

- Confirm whether:
  - Tests exist locally but were omitted from git, or
  - They are planned but not yet implemented.
- Recreate or restore:
  - `tests/test_preprocessing.py`
  - `tests/test_sentiment.py`
  - `tests/test_clustering.py`
  - `tests/test_api.py`

### 4.2 High-Value Test Cases

**preprocess_posts**

- Text normalization:
  - Strips URLs, punctuation, and converts to lowercase.
- Tokenization:
  - Produces expected tokens for representative Peterbilt-related content.
- Edge cases:
  - Empty strings, very long strings, content with emojis or unusual characters.

**analyze_posts_sentiment**

- Positive/negative/neutral samples:
  - Ensure `compound` and component scores match expectations for known phrases.
- Classification thresholds:
  - Confirm mapping between numeric sentiment and labels matches backend expectations (e.g., thresholds for positive/negative/neutral).

**cluster_posts**

- Basic clustering:
  - Given a small set of posts with clear topical separation, ensures they fall into appropriate clusters.
- Taxonomy mapping:
  - Verify clusters derive taxonomy IDs according to `CLUSTER_TAXONOMY`.
  - Confirm `postIds` and `keywords` in cluster objects are correct.

**API endpoints**

- `/health`:
  - Returns `200` with expected JSON keys.
- `/api/analyze`:
  - Valid input: returns clusters and posts with correct shapes and `processingTimeMs`.
  - Invalid input: missing `posts` → `400` error with message.
- `/api/sentiment`:
  - Valid input: returns sentiment annotations only.

### 4.3 Coverage Targets

- Aim for:
  - 80%+ coverage across `preprocessing.py`, `sentiment.py`, `clustering.py`.
  - 70%+ coverage for `api.py` (happy paths + validation).

---

## 5. CI/CD and Execution

### 5.1 CI Pipelines

Follow the GitHub Actions sketch in `TESTING.md`, with three jobs:

- `backend` job:
  - Set up Java 17.
  - Run `./gradlew test jacocoTestReport`.
  - Publish or archive Jacoco reports.
- `frontend` job:
  - Set up Node 18.
  - Run `npm install && npm run test:coverage`.
  - Archive coverage HTML.
- `ml-engine` job:
  - Set up Python 3.11.
  - Install `requirements.txt`.
  - Run `pytest --cov=app`.

### 5.2 Local Developer Workflow

- Encourage a “narrow → broad” testing habit:
  - When changing backend code:
    - Run targeted Spock specs (e.g., `./gradlew test --tests AuthServiceSpec`).
  - For frontend changes:
    - Run `npm test` for related composables/components.
  - For ML changes:
    - Run pytest on specific modules first, then the full suite.

---

## 6. Summary

The project is well-positioned for strong testing discipline: tooling is in place across all three layers, and `TESTING.md` provides good guidance. The main missing piece is **breadth and depth of actual tests**, especially around authentication, analysis flows, and ML correctness. Focusing the next iteration on implementing the recommended tests above will significantly improve confidence in future changes and make the system safer to iterate on under time pressure.

