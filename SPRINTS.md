# Peterbilt "Voice of the Operator" Dashboard — Sprint Breakdown

> Agile sprint plan for building the AI-driven sentiment analysis dashboard. Each sprint is ~1 week for a side project pace.

---

## Project Overview

**Goal:** Visualize what fleet owners and drivers are saying across publicly available data to identify product issues or marketing wins in real-time.

**Target Users:** Marketing and Sales teams at Peterbilt (demo POC)

**Tech Stack:**
| Layer | Technology | Deployment |
|-------|------------|------------|
| Frontend | Nuxt 3 + TailwindCSS + D3.js | Netlify |
| API | Grails (Spring Boot) | Render |
| ML Engine | Python (scikit-learn, NLTK) | Render |
| AI Data Gathering | Gemini 1.5 Flash API | Called from Grails |
| Auth | Nuxt Auth + JWT | Grails-issued tokens |
| Database | PostgreSQL | Supabase (free tier) |

---

## Sprint Overview

| Sprint | Focus | Duration | Status |
|--------|-------|----------|--------|
| Sprint 0 | Foundation & Setup | 3-4 hours | ✅ Complete |
| Sprint 1 | Data Pipeline & Mock Data | 4-6 hours | ✅ Complete |
| Sprint 2 | Python ML Engine | 6-8 hours | ⬜ Pending |
| Sprint 3 | Grails API Layer | 4-6 hours | ⬜ Pending |
| Sprint 4 | D3.js Visualization | 6-8 hours | ⬜ Pending |
| Sprint 5 | Dashboard UI & Auth | 4-6 hours | ⬜ Pending |
| Sprint 6 | Deploy & Demo | 3-4 hours | ⬜ Pending |

**Total estimated time to MVP:** 30-42 hours

---

## Sprint 0: Foundation & Setup
**Goal:** Project scaffolding, API access, development environment

### Tasks

- [x] **0.1** Set up Gemini API access
  - Go to [Google AI Studio](https://aistudio.google.com/)
  - Create API key for data gathering
  - Test with simple request
  - Note: Free tier = 60 requests/minute, 1500/day

- [x] **0.2** Initialize Nuxt 3 frontend project
  - `npx nuxi init frontend`
  - Install dependencies: TailwindCSS, D3.js, @sidebase/nuxt-auth
  - Create directory structure
  - Set up .env.example and .gitignore

- [x] **0.3** Initialize Grails backend project
  - Create Grails 6.x application (requires Java 17)
  - Configure for REST API mode
  - Set up CORS for local development
  - Create .env.example for secrets

- [x] **0.4** Initialize Python ML project
  - Create virtual environment
  - Install: scikit-learn, nltk, pandas, flask (for API)
  - Create requirements.txt
  - Set up project structure

- [x] **0.5** Set up Supabase (PostgreSQL)
  - Create free Supabase account
  - Configure connection in Grails
  - Document schema requirements

- [x] **0.6** Create project documentation
  - README.md with setup instructions
  - ARCHITECTURE.md with Mermaid diagrams
  - .env.example files for all components

### Deliverables
- [x] Three project directories: `frontend/`, `backend/`, `ml-engine/`
- [x] All dependencies installed
- [ ] Gemini API key working (user to configure)
- [x] Supabase documented (user to configure)

### Definition of Done
Can run all three projects locally without errors

---

## Sprint 1: Data Pipeline & Mock Data
**Goal:** Create mock data connectors and unified data model

### Tasks

- [x] **1.1** Define data models
  - `Post` — Raw social/forum post with source, content, timestamp
  - `Cluster` — Grouped keywords with sentiment score
  - `Insight` — AI-generated business summary
  - Create TypeScript interfaces (frontend)
  - Create Grails domain classes (backend)
  - Create Python dataclasses (ML engine)

- [x] **1.2** Create mock data fixtures
  - Twitter/X: 50-100 mock tweets (trucker hashtags, Peterbilt mentions)
  - YouTube: 30-50 mock comments (rig reviews, walkaround videos)
  - Forums: 20-30 mock posts (TruckersReport, Reddit r/Truckers)
  - All data Peterbilt-themed with realistic keywords

- [x] **1.3** Create Peterbilt keyword taxonomy (config/taxonomy.yaml)
  - EV Adoption: 579EV, charging range, battery-electric, 350kW DC
  - Driver Comfort: sleeper, 80-inch, ergonomics, Platinum Ionic Gray
  - Uptime/Reliability: PACCAR Powertrain, service alerts, dealer network
  - Model Demand: 589, 579, wait times, availability

- [x] **1.4** Build Gemini data gathering service (Grails)
  - Create service class for Gemini API calls
  - Implement web content extraction prompts
  - Add rate limiting and error handling
  - Store raw data in PostgreSQL

- [ ] **1.5** Create data ingestion CLI (deferred to Sprint 3)
  - Command to load mock data into database
  - Command to trigger Gemini data gathering (future)
  - Logging for debugging

### Deliverables
- [x] Mock data JSON files in `data/fixtures/`
- [x] Data models in all three projects
- [x] Gemini service class (ready for real data)
- [x] Taxonomy config file (config/taxonomy.yaml)
- [ ] Database seeded with mock data (Sprint 3)

### Definition of Done
`grails run-app` serves mock data via REST endpoint

---

## Sprint 2: Python ML Engine
**Goal:** Implement clustering and sentiment analysis

### Tasks

- [ ] **2.1** Set up Flask API wrapper
  - Create `/api/analyze` endpoint
  - Accept batch of posts, return clusters
  - Health check endpoint
  - CORS configuration

- [ ] **2.2** Implement text preprocessing
  - Tokenization and normalization
  - Stop word removal (with trucking-specific additions)
  - Lemmatization
  - TF-IDF vectorization

- [ ] **2.3** Implement K-Means clustering
  - Cluster posts into 5-7 topic groups
  - Auto-determine optimal K (elbow method)
  - Extract top keywords per cluster
  - Map clusters to Peterbilt taxonomy

- [ ] **2.4** Implement sentiment analysis
  - Use NLTK VADER for initial sentiment
  - Score each post: positive/negative/neutral
  - Aggregate sentiment per cluster
  - Return confidence scores

- [ ] **2.5** Implement LDA topic modeling (optional)
  - Alternative to K-Means for topic discovery
  - Extract topic distributions
  - Compare results with K-Means

- [ ] **2.6** Create cluster summarization
  - Generate cluster labels from top keywords
  - Calculate cluster size and sentiment distribution
  - Prepare data structure for D3.js visualization

### Deliverables
- [ ] Flask API running on port 5000
- [ ] `/api/analyze` returns clustered, scored data
- [ ] Preprocessing pipeline tested
- [ ] Cluster output matches D3.js input format

### Definition of Done
`curl localhost:5000/api/analyze` returns valid cluster JSON

---

## Sprint 3: Grails API Layer
**Goal:** REST API connecting frontend to ML engine

### Tasks

- [ ] **3.1** Create domain classes
  - `Post` — Raw post data
  - `Cluster` — Cluster metadata
  - `ClusterPost` — Join table
  - `AnalysisRun` — Track analysis jobs
  - `User` — For authentication

- [ ] **3.2** Implement REST controllers
  - `PostController` — CRUD for posts
  - `ClusterController` — Get clusters with posts
  - `AnalysisController` — Trigger ML analysis
  - `AuthController` — Login/logout/register

- [ ] **3.3** Integrate with Python ML engine
  - HTTP client to call Flask API
  - Async job processing for large datasets
  - Store results in PostgreSQL
  - Error handling and retries

- [ ] **3.4** Implement JWT authentication
  - User registration with email/password
  - Login returns JWT token
  - Token validation middleware
  - Refresh token flow

- [ ] **3.5** Create Gemini integration service
  - Call Gemini for cluster insights
  - Generate "Business Insight" summaries
  - Store insights with clusters
  - Rate limiting

- [ ] **3.6** Add API documentation
  - Swagger/OpenAPI spec
  - Document all endpoints
  - Example requests/responses

### Deliverables
- [ ] Grails API running on port 8080
- [ ] All REST endpoints functional
- [ ] JWT auth working
- [ ] ML engine integration tested

### Definition of Done
Frontend can authenticate and fetch cluster data via API

---

## Sprint 4: D3.js Visualization
**Goal:** Interactive force-directed graph for cluster visualization

### Tasks

- [ ] **4.1** Set up D3.js in Nuxt
  - Install d3 package
  - Create `components/ClusterGraph.vue`
  - Set up SVG canvas with responsive sizing
  - Configure force simulation

- [ ] **4.2** Implement force-directed graph
  - Nodes = keyword clusters (bubbles)
  - Node size = cluster post count
  - Node color = sentiment (red negative, green positive)
  - Links = keyword co-occurrence (optional)

- [ ] **4.3** Add interactivity
  - Hover: Show cluster summary tooltip
  - Click: Filter to show posts in that cluster
  - Drag: Reposition nodes
  - Zoom/pan: Navigate large graphs

- [ ] **4.4** Create cluster detail panel
  - Slide-out panel on cluster click
  - Show all posts in cluster
  - Display sentiment breakdown
  - Show AI-generated insight

- [ ] **4.5** Add legend and controls
  - Color legend for sentiment
  - Size legend for post count
  - Filter by source (Twitter, YouTube, etc.)
  - Filter by date range

- [ ] **4.6** Optimize performance
  - Limit nodes for large datasets
  - Debounce interactions
  - Lazy load post details

### Deliverables
- [ ] Interactive D3.js visualization component
- [ ] Cluster detail panel
- [ ] Filtering controls
- [ ] Responsive design

### Definition of Done
Can visualize 50+ clusters with smooth interactions

---

## Sprint 5: Dashboard UI & Auth
**Goal:** Complete dashboard with auth and polish

### Tasks

- [ ] **5.1** Implement Nuxt Auth
  - Configure @sidebase/nuxt-auth
  - Create login/register pages
  - Protected route middleware
  - Token storage and refresh

- [ ] **5.2** Create dashboard layout
  - Header with logo and user menu
  - Sidebar with navigation
  - Main content area for visualization
  - Footer with metadata

- [ ] **5.3** Build dashboard pages
  - `/login` — Authentication
  - `/register` — New user signup
  - `/dashboard` — Main visualization (protected)
  - `/dashboard/posts` — Raw post browser (protected)
  - `/dashboard/insights` — AI insights list (protected)

- [ ] **5.4** Add real-time indicators
  - Last analysis timestamp
  - Data freshness indicator
  - Loading states
  - Error handling UI

- [ ] **5.5** Create summary cards
  - Total posts analyzed
  - Sentiment distribution pie chart
  - Top clusters by volume
  - Recent insights

- [ ] **5.6** Polish and responsive design
  - Mobile-friendly layout
  - Dark/light mode (optional)
  - Accessibility audit
  - Loading skeletons

### Deliverables
- [ ] Complete auth flow
- [ ] Polished dashboard UI
- [ ] All pages functional
- [ ] Responsive design

### Definition of Done
End-to-end flow: login → view dashboard → interact with clusters

---

## Sprint 6: Deploy & Demo
**Goal:** Production deployment and demo preparation

### Tasks

- [ ] **6.1** Deploy Nuxt frontend to Netlify
  - Configure build settings
  - Set environment variables
  - Custom domain (optional)
  - Test production build

- [ ] **6.2** Deploy Grails + Python to Render
  - Create Render web service for Grails
  - Create Render web service for Python ML
  - Configure PostgreSQL addon
  - Set environment variables
  - Test inter-service communication

- [ ] **6.3** Configure production environment
  - CORS settings for production domains
  - Secure JWT secrets
  - Rate limiting
  - Error monitoring (optional: Sentry)

- [ ] **6.4** Seed production database
  - Load mock data for demo
  - Create demo user account
  - Run initial analysis

- [ ] **6.5** Create demo script
  - Talking points for each feature
  - Sample user journey
  - Key insights to highlight
  - Q&A preparation

- [ ] **6.6** Documentation
  - Update README with production URLs
  - Create RUNBOOK.md for operations
  - Document API endpoints
  - Record demo video (optional)

### Deliverables
- [ ] Live production deployment
- [ ] Demo script ready
- [ ] Documentation complete

### Definition of Done
Can demo full application to stakeholders

---

## Quick Reference: Project Structure

```
sentiment-analyzer/
├── frontend/                 # Nuxt 3 application
│   ├── components/
│   │   ├── ClusterGraph.vue  # D3.js visualization
│   │   ├── ClusterDetail.vue # Detail panel
│   │   └── SummaryCards.vue  # Dashboard cards
│   ├── pages/
│   │   ├── login.vue
│   │   ├── register.vue
│   │   └── dashboard/
│   │       ├── index.vue     # Main visualization
│   │       ├── posts.vue     # Post browser
│   │       └── insights.vue  # AI insights
│   ├── composables/
│   │   └── useApi.ts         # API client
│   └── nuxt.config.ts
│
├── backend/                  # Grails application
│   ├── grails-app/
│   │   ├── controllers/
│   │   │   ├── PostController.groovy
│   │   │   ├── ClusterController.groovy
│   │   │   ├── AnalysisController.groovy
│   │   │   └── AuthController.groovy
│   │   ├── domain/
│   │   │   ├── Post.groovy
│   │   │   ├── Cluster.groovy
│   │   │   └── User.groovy
│   │   └── services/
│   │       ├── GeminiService.groovy
│   │       └── MlEngineService.groovy
│   └── application.yml
│
├── ml-engine/                # Python ML service
│   ├── app/
│   │   ├── api.py            # Flask API
│   │   ├── clustering.py     # K-Means/LDA
│   │   ├── sentiment.py      # VADER sentiment
│   │   └── preprocessing.py  # Text processing
│   ├── requirements.txt
│   └── Dockerfile
│
├── data/
│   └── fixtures/             # Mock data JSON files
│       ├── twitter.json
│       ├── youtube.json
│       └── forums.json
│
├── docs/
│   ├── ARCHITECTURE.md
│   ├── RUNBOOK.md
│   └── API.md
│
├── AGENT.md                  # AI assistant persona
├── SPRINTS.md                # This file
└── README.md
```

---

## Peterbilt Cluster Categories

| Cluster | Keywords | Sentiment Indicators |
|---------|----------|---------------------|
| **EV Adoption** | 579EV, charging range, battery-electric, 350kW DC, zero emission | Range anxiety (neg), innovation (pos) |
| **Driver Comfort** | sleeper, 80-inch, ergonomics, Platinum Ionic Gray, interior | Praise (pos), complaints (neg) |
| **Uptime/Reliability** | PACCAR Powertrain, service alerts, dealer network, breakdown | Frustration (neg), dependable (pos) |
| **Model Demand** | 589, 579, wait times, availability, order backlog | Impatience (neg), excitement (pos) |

---

## Dependencies

### Frontend (package.json)
```json
{
  "dependencies": {
    "nuxt": "^3.x",
    "@sidebase/nuxt-auth": "^0.6.x",
    "d3": "^7.x",
    "@nuxtjs/tailwindcss": "^6.x"
  }
}
```

### Backend (build.gradle)
```groovy
dependencies {
    implementation 'org.grails:grails-core:6.x'
    implementation 'org.grails.plugins:spring-security-rest:3.x'
    implementation 'org.postgresql:postgresql:42.x'
}
```

### ML Engine (requirements.txt)
```
flask>=3.0.0
scikit-learn>=1.4.0
nltk>=3.8.0
pandas>=2.1.0
gunicorn>=21.0.0
```

---

## Notes

- **Side project pace:** Each sprint designed for ~1 week of casual work
- **MVP focus:** Sprints 0-4 get you to a working demo
- **Mock data first:** Build UI with fixtures, swap in real data later
- **Iterate on prompts:** Expect 2-3 iterations on Gemini prompts
- **Demo-ready:** Focus on visual impact for stakeholder presentation
