# Peterbilt "Voice of the Operator" Dashboard

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

- ✅ Multi-channel data ingestion (Twitter, YouTube, Forums)
- ✅ ML clustering with K-Means + TF-IDF (Python/scikit-learn)
- ✅ NLTK VADER sentiment analysis per post and cluster
- ✅ Interactive D3.js bubble chart visualization
- ✅ Responsive dashboard UI with TailwindCSS
- ✅ Unified local dev scripts (`start-local.sh`, `stop-local.sh`)
- ✅ Comprehensive testing framework (Spock, Vitest, pytest)
- ⚠️ AI-powered insights (Gemini 1.5 Flash) — stub ready
- ⚠️ JWT authentication — Sprint 5

---

## Tech Stack

| Layer | Technology | Deployment |
|-------|------------|------------|
| Frontend | Nuxt 3 + TailwindCSS + D3.js | Netlify |
| API | Grails 6 (Spring Boot) | Render |
| ML Engine | Python (scikit-learn, NLTK, Flask) | Render |
| AI/NLP | Google Gemini 1.5 Flash | API |
| Database | PostgreSQL | Supabase (free tier) |
| Auth | JWT (Grails Spring Security REST) | — |

---

## Project Structure

```
sentiment-analyzer/
├── frontend/           # Nuxt 3 + TailwindCSS + D3.js
├── backend/            # Grails 6 REST API (Java 17)
├── ml-engine/          # Python Flask + scikit-learn + NLTK
├── config/             # Auditable taxonomy config
├── data/fixtures/      # Mock data (50 posts)
├── docs/               # Architecture diagrams
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

### Frontend (.env)
```
NUXT_PUBLIC_API_URL=http://localhost:8080
```

### Backend (application.yml)
```yaml
gemini:
  apiKey: ${GEMINI_API_KEY}
mlEngine:
  url: http://localhost:5000
```

### ML Engine (.env)
```
FLASK_ENV=development
```

---

## API Endpoints

### Backend (Grails) - Port 8080

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check with ML engine status |
| GET | `/api/posts` | List all posts (paginated) |
| GET | `/api/posts/{id}` | Get single post |
| GET | `/api/posts/sources` | Get source counts |
| GET | `/api/clusters` | Get all clusters with sample posts |
| GET | `/api/clusters/{id}` | Get cluster with all posts |
| GET | `/api/clusters/summary` | Dashboard summary stats |
| POST | `/api/analysis/trigger` | Run ML analysis on all posts |
| POST | `/api/analysis/load-fixtures` | Load mock data into database |
| DELETE | `/api/analysis/clear` | Clear all data (dev only) |

### ML Engine (Python) - Port 5000

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check |
| POST | `/api/analyze` | Analyze posts for clusters + sentiment |
| POST | `/api/sentiment` | Sentiment analysis only |

---

## Documentation

- [Sprint Breakdown](SPRINTS.md) — Development roadmap (Sprints 0-4 complete)
- [Architecture](docs/ARCHITECTURE.md) — System design diagrams
- [Testing Guide](TESTING.md) — Testing setup and execution
- [Testing Status](TESTING_STATUS.md) — Current test coverage and roadmap
- [Implementation Summary](IMPLEMENTATION_SUMMARY.md) — Recent changes and configuration
- [Code Review](CODE_REVIEW_v2.md) — Latest code review findings
- [Taxonomy Config](config/taxonomy.yaml) — Auditable cluster categories

---

## License

Proprietary — Peterbilt POC

---

## Maintainer

**Ben Hislop**
