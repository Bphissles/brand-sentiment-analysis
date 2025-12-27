# Sentiment Analyzer Dashboard

> AI-powered sentiment analysis dashboard visualizing fleet owner and driver feedback across public channels.

---

## What It Does

Aggregates and analyzes publicly available data from social media, forums, and connected vehicle alerts to identify:
- **Product issues** requiring attention
- **Marketing wins** to amplify
- **Emerging trends** in customer sentiment

**Target Users:** Marketing and Sales teams

---

## Features

- ✅ Multi-channel data ingestion (Twitter, YouTube, Reddit, Forums, News)
- ✅ Gemini-powered web scraping with Google Search grounding
- ✅ ML clustering with K-Means + TF-IDF (Python/scikit-learn)
- ✅ Sentiment analysis (NLTK VADER + Gemini 2.0 Flash)
- ✅ Interactive D3.js bubble chart visualization
- ✅ Responsive dashboard UI with TailwindCSS + dark mode
- ✅ Unified local dev scripts (`start-local.sh`, `stop-local.sh`)
- ✅ Comprehensive testing framework (Spock, Vitest, pytest)
- ✅ OpenAPI/Swagger documentation (flasgger + springdoc-openapi)
- ✅ AI-powered insights (Gemini 2.0 Flash) — Trend Analysis, Recommendations, Executive Summary
- ✅ JWT authentication with login/register/logout
- ✅ Admin-only Data Management page
- ✅ Sentiment filtering and sorting in modals

---

## Tech Stack

| Layer | Technology | Deployment |
|-------|------------|------------|
| Frontend | Nuxt 3 + TailwindCSS + D3.js | Netlify |
| API | Grails 6 (Spring Boot) | Render |
| ML Engine | Python (scikit-learn, NLTK/VADER, Flask) | Render |
| AI/NLP | Google Gemini 2.0 Flash | API (Grails + Python) |
| Database | PostgreSQL | Supabase (free tier) |
| Auth | Custom JWT | Grails-issued tokens |

---

## Project Structure

```
sentiment-analyzer/
├── frontend/           # Nuxt 3 + TailwindCSS + D3.js
├── backend/            # Grails 6 REST API (Java 17)
├── ml-engine/          # Python Flask + scikit-learn + NLTK
├── config/             # Auditable taxonomy config
├── data/fixtures/      # Mock data (50 posts)
├── docs/               # Architecture diagrams & API docs
├── logs/               # Local dev logs
├── .pids/              # Process IDs for local dev
├── start-local.sh      # Start all services
├── stop-local.sh       # Stop all services
├── SPRINTS.md          # Sprint breakdown
└── README.md           # This file
```

---

## Cluster Categories

| Cluster | Business Value |
|---------|----------------|
| **EV Adoption** | Product roadmap feedback on 579EV, charging infrastructure |
| **Driver Comfort** | Marketing messaging for sleeper, ergonomics features |
| **Uptime/Reliability** | Service team insights on PACCAR Powertrain, dealer network |
| **Model Demand** | Sales forecasting for 589, 579 wait times |

---

## Quick Start

### Prerequisites

- Node.js 18+
- **Java 17** (required for Grails 6)
- Python 3.11+
- Supabase account (free tier)

### 1. Clone and configure

```bash
git clone <repo-url>
cd sentiment-analyzer
cp .env.example .env
# Edit .env with your Supabase and Gemini credentials
```

### 2. Start all services

```bash
./start-local.sh
```

This starts:
- **Frontend** → http://localhost:3000
- **Backend** → http://localhost:8080  
- **ML Engine** → http://localhost:5000

### 3. Stop all services

```bash
./stop-local.sh
```

### Manual Setup (if needed)

<details>
<summary>Frontend</summary>

```bash
cd frontend
npm install
npm run dev
```
</details>

<details>
<summary>Backend (Grails)</summary>

```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
source ../.env
./gradlew bootRun
```
</details>

<details>
<summary>ML Engine (Python)</summary>

```bash
cd ml-engine
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python app/api.py
```
</details>

### Database Setup (Supabase)

1. Create account at [supabase.com](https://supabase.com)
2. Create new project
3. Get connection string from Settings → Database
4. Get API keys from Settings → API
5. Add to `.env`:
   - `SUPABASE_URL`
   - `SUPABASE_ANON_KEY`
   - `SUPABASE_SERVICE_KEY`
   - `SUPABASE_DB_PASSWORD`

---

## Environment Variables

Copy `.env.example` to `.env` at the project root and configure:

### Root .env (Shared Configuration)
```bash
# Gemini API (for web scraping and AI insights)
GEMINI_API_KEY=your_gemini_api_key_here

# Supabase (Database)
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your_supabase_anon_key
SUPABASE_SERVICE_KEY=your_supabase_service_key
SUPABASE_DB_PASSWORD=your_database_password

# Service URLs
NUXT_PUBLIC_API_URL=http://localhost:8080
ML_ENGINE_URL=http://localhost:5000

# JWT Secret (min 32 characters)
JWT_SECRET=your_jwt_secret_here_min_32_chars
```

### Frontend (.env)
```bash
NUXT_PUBLIC_API_URL=http://localhost:8080
```

### ML Engine (.env)
```bash
FLASK_ENV=development
FLASK_DEBUG=1
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_SERVICE_KEY=your_supabase_service_key
```

---

## API Endpoints

### Backend (Grails) - Port 8080

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check with ML engine status |
| GET | `/api/posts` | List all posts (paginated, filterable by source/sentiment) |
| GET | `/api/posts/{id}` | Get single post |
| GET | `/api/posts/sources` | Get source counts |
| GET | `/api/clusters` | Get all clusters with sample posts |
| GET | `/api/clusters/{id}` | Get cluster with all posts |
| GET | `/api/clusters/summary` | Dashboard summary stats |
| POST | `/api/analysis/trigger` | Run ML analysis on all posts |
| POST | `/api/analysis/load-fixtures` | Load mock data into database |
| DELETE | `/api/analysis/clear` | Clear all data (admin only) |
| GET | `/api/insights` | Get cached AI insights |
| POST | `/api/insights/generate` | Generate new AI insights (Gemini) |
| GET | `/api/ingestion/status` | Get data ingestion status |
| POST | `/api/ingestion/scrapeAll` | Scrape all sources (Gemini web search) |
| POST | `/api/ingestion/scrape/{source}` | Scrape specific source (twitter, youtube, reddit, forums, news) |
| POST | `/api/ingestion/import` | Manual import posts from JSON |
| POST | `/api/auth/login` | User login, returns JWT |
| POST | `/api/auth/register` | User registration |
| GET | `/api/auth/me` | Get current user info |
| POST | `/api/auth/logout` | User logout |

### ML Engine (Python) - Port 5000

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check |
| POST | `/api/analyze` | Analyze posts for clusters + sentiment |
| POST | `/api/sentiment` | Sentiment analysis only |

---

## Documentation

- [Sprint Breakdown](SPRINTS.md) — Development roadmap (Sprints 0-5 complete)
- [Architecture](docs/ARCHITECTURE.md) — System design diagrams
- [API Documentation](docs/API_DOCUMENTATION.md) — OpenAPI/Swagger documentation guide
- [Testing Guide](TESTING.md) — Testing setup and execution
- [Code Review](CODE_REVIEW.md) — Code review guidelines
- [Code Review Log](CODE_REVIEW_LOG.md) — Code review findings and status
- [Agent Guidelines](AGENTS.md) — Development guidelines and conventions
- [Taxonomy Config](config/taxonomy.yaml) — Auditable cluster categories

### Interactive API Documentation

- **ML Engine Swagger UI**: http://localhost:5000/apidocs/
- **Backend Swagger UI**: http://localhost:8080/swagger-ui/index.html

---

## Maintainer

**Ben Hislop**
