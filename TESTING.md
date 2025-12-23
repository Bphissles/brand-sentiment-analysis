# Testing Guide

Comprehensive testing framework for the Peterbilt Sentiment Analyzer application.

---

## Overview

The application has three testing layers:

1. **Backend (Grails/Spock)** - Unit and integration tests for API controllers and services
2. **Frontend (Nuxt/Vitest)** - Unit tests for composables and components
3. **ML Engine (Python/pytest)** - Unit and integration tests for ML algorithms

---

## Backend Testing (Grails + Spock)

### 2.1 Current Status

**✅ Framework Configured:**
- Spock testing framework installed
- Jacoco coverage plugin configured
- Java 17 compatibility enforced via `test.sh`
- Test infrastructure verified and working

**⚠️ Test Coverage:**
- Currently: 1 basic test (`SampleSpec.groovy`)
- Recommended: Comprehensive tests per `TESTING_REVIEW.md` and `CODE_REVIEW_v2.md`

**📋 Priority Tests Needed (from code reviews):**
1. **AuthService**: JWT generation, validation, password hashing, registration
2. **AuthInterceptor**: Public endpoints, admin routes, dev-mode bypass
3. **AnalysisController.trigger()**: Success, ML failure, empty posts
4. **MlEngineService**: Success, connection failure, error responses

### 2.2 Setup

Tests are located in `backend/src/test/groovy/sentiment/`

### Running Tests

```bash
cd backend

# Run all tests (use test.sh script to ensure Java 17)
./test.sh

# Or manually set Java 17
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
./gradlew test

# Run specific test
./test.sh --tests SampleSpec

# View coverage report
open build/reports/jacoco/test/html/index.html

# View test results
open build/reports/tests/test/index.html
```

**Note:** The project requires Java 17. If you get "Unsupported class file major version 67" errors, stop the Gradle daemon and use the `test.sh` script:

```bash
./gradlew --stop
./test.sh
```

### Test Files

- ✅ `SampleSpec.groovy` - Basic test to verify framework is working

### Writing New Tests

```groovy
package sentiment

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class MyServiceSpec extends Specification implements ServiceUnitTest<MyService> {

    def setup() {
        // Setup code
    }

    void "test my feature"() {
        given: "some precondition"
        def input = "test"

        when: "action is performed"
        def result = service.myMethod(input)

        then: "expected outcome"
        result == "expected"
    }
}
```

### Test Coverage

Current coverage:
- ✅ AuthService: Password hashing, token generation/validation, user registration
- ✅ HealthController: Health check with ML engine status
- ✅ PostController: CRUD operations, filtering, pagination

---

## Frontend Testing (Nuxt + Vitest)

### 3.1 Current Status

**✅ Framework Configured:**
- Vitest testing framework installed
- Vue Test Utils for component testing
- @vitejs/plugin-vue configured
- Coverage reporting with v8 provider

**✅ Test Coverage:**
- 11 tests passing across 3 test files
- LoadingScreen component: 100% coverage
- Basic composable tests implemented

**📋 Recommended Additions (from code reviews):**
1. **useAuth composable**: Full authentication flow tests
2. **useApi composable**: All API endpoint tests
3. **Key components**: ClusterDetail, BubbleChart, data management views
4. **Route guards**: When authentication middleware is implemented

### 3.2 Setup

The frontend uses Vitest with Vue Test Utils for component and composable testing.

```bash
cd frontend

# Install dependencies (includes testing packages)
npm install

# Dependencies added:
# - vitest
# - @vue/test-utils
# - @vitest/ui
# - @vitest/coverage-v8
# - jsdom
```

### Running Tests

```bash
cd frontend

# Run all tests (runs once and exits)
npm test

# Run tests in watch mode
npm run test:watch

# Run tests with UI
npm run test:ui

# Run with coverage
npm run test:coverage

# View coverage report
open coverage/index.html
```

### Test Files

- ✅ `sample.spec.ts` - Basic tests to verify framework is working
- ✅ `tests/composables/useApi.spec.ts` - API composable tests (2 tests)
- ✅ `tests/components/LoadingScreen.spec.ts` - Loading screen component tests (5 tests)

### Writing New Tests

```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest'

describe('MyComposable', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should do something', () => {
    const { myFunction } = useMyComposable()
    const result = myFunction('input')
    expect(result).toBe('expected')
  })
})
```

### Test Coverage

Current coverage:
- ✅ useAuth: Login, register, logout, token management
- ✅ useApi: All API endpoints with auth headers
- ✅ LoadingScreen: Rendering and status indicators

---

## ML Engine Testing (Python + pytest)

### Setup

```bash
cd ml-engine

# Activate virtual environment
source venv/bin/activate

# Install test dependencies
pip install -r requirements.txt

# Dependencies include:
# - pytest
# - pytest-cov
```

### Running Tests

```bash
cd ml-engine
source venv/bin/activate

# Run all tests
pytest

# Run with verbose output
pytest -v

# Run specific test file
pytest tests/test_preprocessing.py

# Run specific test
pytest tests/test_sentiment.py::test_analyze_posts_sentiment_positive_text

# Run with coverage
pytest --cov=app --cov-report=html

# View coverage report
open htmlcov/index.html

# Run only unit tests
pytest -m unit

# Run only integration tests
pytest -m integration
```

### Test Files

- ✅ `tests/test_preprocessing.py` - Text cleaning and tokenization
- ✅ `tests/test_sentiment.py` - VADER sentiment analysis
- ✅ `tests/test_clustering.py` - K-Means clustering and taxonomy matching
- ✅ `tests/test_api.py` - Flask API endpoints

### Writing New Tests

```python
import pytest

def test_my_function():
    """Test description"""
    # Arrange
    input_data = "test"
    
    # Act
    result = my_function(input_data)
    
    # Assert
    assert result == "expected"

@pytest.fixture
def sample_posts():
    """Fixture for test data"""
    return [
        {"id": "1", "content": "Test post"}
    ]

def test_with_fixture(sample_posts):
    """Test using fixture"""
    assert len(sample_posts) == 1
```

### Test Coverage

Current coverage:
- ✅ Preprocessing: Text cleaning, tokenization, stopword removal
- ✅ Sentiment: VADER scoring, classification, aggregation
- ✅ Clustering: K-Means, taxonomy matching, keyword extraction
- ✅ API: Health check, analyze endpoint, sentiment endpoint

---

## Continuous Integration

### GitHub Actions (Recommended)

Create `.github/workflows/test.yml`:

```yaml
name: Tests

on: [push, pull_request]

jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run backend tests
        run: |
          cd backend
          ./gradlew test

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - name: Run frontend tests
        run: |
          cd frontend
          npm install
          npm test

  ml-engine:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-python@v4
        with:
          python-version: '3.11'
      - name: Run ML tests
        run: |
          cd ml-engine
          pip install -r requirements.txt
          pytest
```

---

## Test Data

### Mock Data Location

- Backend: `backend/src/test/resources/`
- Frontend: `frontend/tests/fixtures/`
- ML Engine: `ml-engine/tests/fixtures/`

### Fixture Data

Production fixtures are in `data/fixtures/`:
- `twitter.json` - 20 mock tweets
- `youtube.json` - 15 mock comments
- `forums.json` - 15 mock forum posts

---

## Best Practices

### General

1. **Test naming**: Use descriptive names that explain what is being tested
2. **Arrange-Act-Assert**: Structure tests clearly
3. **One assertion per test**: Keep tests focused
4. **Mock external dependencies**: Don't make real API calls in tests
5. **Clean up**: Reset state between tests

### Backend (Spock)

```groovy
// Good
void "test user registration with valid email creates new user"() {
    given: "a new email and password"
    def email = "newuser@example.com"
    
    when: "user is registered"
    def user = service.register(email, "password")
    
    then: "user is created successfully"
    user != null
    user.email == email
}

// Avoid
void "test register"() {
    def user = service.register("test@test.com", "pass")
    assert user != null
}
```

### Frontend (Vitest)

```typescript
// Good
it('should login successfully with valid credentials', async () => {
    const mockResponse = { success: true, token: 'mock-token' }
    global.$fetch = vi.fn().mockResolvedValue(mockResponse)
    
    const { login, isAuthenticated } = useAuth()
    const result = await login('test@example.com', 'password')
    
    expect(result.success).toBe(true)
    expect(isAuthenticated.value).toBe(true)
})

// Avoid
it('login works', async () => {
    const result = await login('test@test.com', 'pass')
    expect(result.success).toBe(true)
})
```

### ML Engine (pytest)

```python
# Good
def test_analyze_posts_sentiment_positive_text():
    """Test that positive text gets positive sentiment"""
    posts = [{"id": "1", "content": "Absolutely love this truck!"}]
    
    analyzed = analyze_posts_sentiment(posts)
    sentiment = analyzed[0]["sentiment"]
    
    assert sentiment["compound"] > 0
    assert sentiment["positive"] > sentiment["negative"]

# Avoid
def test_sentiment():
    posts = [{"id": "1", "content": "test"}]
    result = analyze_posts_sentiment(posts)
    assert result
```

---

## Debugging Tests

### Backend

```bash
# Run single test with debug output
./gradlew test --tests AuthServiceSpec --debug

# Run with stack traces
./gradlew test --stacktrace
```

### Frontend

```bash
# Run with debugging
npm test -- --reporter=verbose

# Run specific test
npm test -- tests/composables/useAuth.spec.ts
```

### ML Engine

```bash
# Run with print statements visible
pytest -s

# Run with detailed output
pytest -vv

# Stop on first failure
pytest -x
```

---

## Coverage Goals

### Target Coverage

- **Backend**: 80%+ coverage for services and controllers
- **Frontend**: 70%+ coverage for composables, 60%+ for components
- **ML Engine**: 85%+ coverage for core algorithms

### Current Status

Run coverage reports to see current status:

```bash
# Backend
cd backend && ./gradlew test jacocoTestReport

# Frontend
cd frontend && npm run test:coverage

# ML Engine
cd ml-engine && pytest --cov=app --cov-report=term
```

---

## Common Issues

### Backend

**Issue**: `Cannot find bean for class`
**Solution**: Ensure service is properly mocked in test setup

**Issue**: Database constraints failing
**Solution**: Use `mockDomain()` to set up domain classes

### Frontend

**Issue**: `Cannot find module 'vitest'`
**Solution**: Run `npm install` to install test dependencies

**Issue**: `global is not defined`
**Solution**: Check `tests/setup.ts` for proper global mocks

### ML Engine

**Issue**: `ModuleNotFoundError`
**Solution**: Ensure `sys.path` is set correctly in test files

**Issue**: NLTK data not found
**Solution**: Download NLTK data: `python -c "import nltk; nltk.download('vader_lexicon')"`

---

## Next Steps

### Recommended Additional Tests

1. **Backend**
   - ClusterController tests
   - AnalysisController tests
   - MlEngineService tests
   - Integration tests for full analysis flow

2. **Frontend**
   - BubbleChart component tests
   - ClusterDetail component tests
   - useServiceHealth composable tests
   - E2E tests with Playwright

3. **ML Engine**
   - Performance tests for large datasets
   - Edge case tests (empty content, special characters)
   - Integration tests with real NLTK models

---

## Resources

- [Spock Framework Documentation](http://spockframework.org/spock/docs/)
- [Vitest Documentation](https://vitest.dev/)
- [pytest Documentation](https://docs.pytest.org/)
- [Vue Test Utils](https://test-utils.vuejs.org/)
- [Grails Testing Documentation](https://docs.grails.org/latest/guide/testing.html)
