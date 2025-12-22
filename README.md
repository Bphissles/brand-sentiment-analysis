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
- ✅ AI-powered content extraction (Gemini 1.5 Flash)
- ✅ ML clustering with K-Means/LDA (Python/scikit-learn)
- ✅ Sentiment analysis per cluster
- ✅ Interactive D3.js force-directed visualization
- ✅ Real-time business insights
- ✅ JWT authentication
- ✅ Responsive dashboard UI

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
├── frontend/           # Nuxt 3 application
├── backend/            # Grails REST API
├── ml-engine/          # Python ML service
├── data/fixtures/      # Mock data for development
├── docs/               # Architecture and runbooks
├── AGENT.md            # AI assistant persona
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

## Setup

### Prerequisites

- Node.js 18+
- **Java 17** (required for Grails 6 — Java 23 not supported)
- Python 3.11+
- Supabase account (free tier)

### 1. Clone and install

```bash
git clone <repo-url>
cd sentiment-analyzer
```

### 2. Frontend setup

```bash
cd frontend
npm install
cp .env.example .env
# Edit .env with API URL
npm run dev
```

### 3. Backend setup

```bash
cd backend
# Ensure Java 17 is active
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
./gradlew bootRun
# Runs on http://localhost:8080
```

### 4. ML Engine setup

```bash
cd ml-engine
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python app/api.py
# Runs on http://localhost:5000
```

### 5. Database setup (Supabase)

1. Create a free account at [supabase.com](https://supabase.com)
2. Create a new project
3. Go to Settings → Database to get connection string
4. Go to Settings → API to get your keys
5. Add credentials to `.env` files

```bash
# Copy the .env.example and fill in Supabase credentials
cp .env.example .env
```

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

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Authenticate user |
| POST | `/api/auth/register` | Register new user |
| GET | `/api/clusters` | Get all clusters with sentiment |
| GET | `/api/clusters/{id}` | Get cluster detail with posts |
| GET | `/api/posts` | List all posts |
| POST | `/api/analysis/run` | Trigger new ML analysis |
| GET | `/api/insights` | Get AI-generated insights |

---

## Documentation

- [Sprint Breakdown](SPRINTS.md) — Development roadmap
- [Architecture](docs/ARCHITECTURE.md) — System design diagrams
- [Operations Runbook](docs/RUNBOOK.md) — Deployment and troubleshooting

---

## License

Proprietary — Peterbilt POC

---

## Maintainer

**Ben Hislop**
