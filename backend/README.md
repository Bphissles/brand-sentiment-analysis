# Backend — Grails 6 REST API

> REST API layer connecting the frontend to ML engine and database.

## Tech Stack

- **Grails 6.1** — Groovy-based Spring Boot framework
- **Java 17** — Required runtime
- **PostgreSQL** — Database (Supabase)
- **Gemini 2.0 Flash** — AI insights generation

## Features

- 🔐 Custom JWT authentication
- 📊 REST API for posts, clusters, and insights
- 🤖 Gemini integration for AI-powered insights
- 🔗 ML Engine communication service
- 📁 Fixture data loading

## Project Structure

```
backend/grails-app/
├── controllers/sentiment/
│   ├── AuthController.groovy       # Login/register/logout
│   ├── PostController.groovy       # Post CRUD + filtering
│   ├── ClusterController.groovy    # Cluster data + summary
│   ├── AnalysisController.groovy   # Trigger ML analysis
│   ├── AiInsightController.groovy  # AI insights endpoints
│   ├── HealthController.groovy     # Health check
│   └── AuthInterceptor.groovy      # JWT validation
├── domain/sentiment/
│   ├── Post.groovy                 # Social media post
│   ├── Cluster.groovy              # Topic cluster
│   ├── AnalysisRun.groovy          # Analysis job tracking
│   ├── AiInsight.groovy            # Cached AI insights
│   └── User.groovy                 # User accounts
├── services/sentiment/
│   ├── AuthService.groovy          # JWT generation/validation
│   ├── GeminiService.groovy        # Gemini API integration
│   ├── MlEngineService.groovy      # Python ML communication
│   ├── AiInsightService.groovy     # Insight caching
│   └── DataLoaderService.groovy    # Fixture loading
└── conf/
    └── application.yml             # Configuration
```

## Setup

Requires **Java 17**:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
```

## Development

```bash
# Load environment variables
source ../.env

# Run the application
./gradlew bootRun
```

Opens at http://localhost:8080

## Environment Variables

Set in root `.env` file:

```env
# Database (Supabase)
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_DB_PASSWORD=your_password

# AI
GEMINI_API_KEY=your_gemini_key

# Auth
JWT_SECRET=your_jwt_secret
```

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/login` | No | User login |
| POST | `/api/auth/register` | No | User registration |
| GET | `/api/auth/me` | Yes | Current user info |
| GET | `/api/posts` | Yes | List posts (filterable) |
| GET | `/api/clusters` | Yes | List clusters |
| GET | `/api/clusters/summary` | Yes | Dashboard stats |
| POST | `/api/analysis/trigger` | Admin | Run ML analysis |
| GET | `/api/insights` | Yes | Get AI insights |
| POST | `/api/insights/generate` | Yes | Generate new insights |

## Testing

```bash
# Run tests (use test.sh for Java 17 compatibility)
./test.sh

# Or manually
./gradlew test

# View coverage report
open build/reports/jacoco/test/html/index.html
```

## Key Services

### AuthService
- JWT token generation with HS256
- Password hashing with BCrypt
- Token validation and user lookup

### GeminiService
- Trend analysis generation
- Recommendations generation
- Executive summary generation
- Cluster insight generation

### MlEngineService
- HTTP client to Python ML engine
- Sends posts for clustering/sentiment
- Handles timeouts and errors

## Grails Documentation

- [User Guide](https://docs.grails.org/6.1.1/guide/index.html)
- [API Reference](https://docs.grails.org/6.1.1/api/index.html)
- [Grails Guides](https://guides.grails.org/index.html)

