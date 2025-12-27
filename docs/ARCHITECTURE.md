# Peterbilt Voice of the Operator — Architecture

## System Overview

```mermaid
flowchart TB
    subgraph Sources["📡 Data Sources (Public Web)"]
        S1[Twitter/X<br/>Trucker mentions]
        S2[YouTube<br/>Rig reviews]
        S3[Forums<br/>TruckersReport, Reddit]
    end

    subgraph Gemini["🤖 Gemini 2.0 Flash API"]
        GEM[Web search grounding<br/>& AI insights]
    end

    subgraph Backend["☕ Grails API (Render)"]
        direction TB
        AUTH[AuthController<br/>JWT Auth]
        POST[PostController<br/>Posts + sources]
        CLUSTER[ClusterController<br/>Clusters + summary]
        ANALYSIS[AnalysisController<br/>Trigger ML]
        INGEST[DataIngestionController<br/>Web scraping/import]
        INSIGHTS[AiInsightController<br/>AI insights]
        GEMINI_SVC[GeminiService<br/>Gemini search + insights]
        ML_SVC[MlEngineService<br/>HTTP client]
    end

    subgraph MLEngine["🐍 Python ML (Render)"]
        direction TB
        FLASK[Flask API<br/>:5000]
        PREPROCESS[Preprocessing<br/>TF-IDF]
        KMEANS[K-Means<br/>Clustering]
        SENTIMENT[VADER + Gemini<br/>Sentiment]
    end

    subgraph Database["🗄️ PostgreSQL (Supabase)"]
        DB[(Posts, Clusters,<br/>Users, Insights)]
    end

    subgraph Frontend["⚡ Nuxt 4 (Netlify)"]
        direction TB
        LOGIN[Login/Register]
        DASHBOARD[Dashboard]
        D3[D3.js Graph<br/>Force-directed]
        DETAIL[Cluster Detail<br/>Panel]
    end

    %% Data flow
    Sources --> GEM
    GEM --> GEMINI_SVC
    Backend --> DB
    Backend --> DB
    Backend <--> MLEngine
    Frontend <--> Backend

    %% Internal flows
    ANALYSIS --> ML_SVC
    ML_SVC --> FLASK
    FLASK --> PREPROCESS
    PREPROCESS --> KMEANS
    KMEANS --> SENTIMENT

    INGEST --> GEMINI_SVC
    INSIGHTS --> GEMINI_SVC
    GEMINI_SVC --> GEM
    SENTIMENT --> GEM

    %% Styling
    classDef source fill:#e3f2fd,stroke:#1565c0
    classDef backend fill:#fff3e0,stroke:#e65100
    classDef ml fill:#e8f5e9,stroke:#2e7d32
    classDef frontend fill:#fce4ec,stroke:#c2185b
    classDef db fill:#f3e5f5,stroke:#7b1fa2

    class S1,S2,S3 source
    class AUTH,POST,CLUSTER,ANALYSIS,INGEST,INSIGHTS,GEMINI_SVC,ML_SVC backend
    class FLASK,PREPROCESS,KMEANS,SENTIMENT ml
    class LOGIN,DASHBOARD,D3,DETAIL frontend
    class DB db
```

---

## Data Flow: Analysis Pipeline

```mermaid
sequenceDiagram
    participant User
    participant Nuxt as Nuxt Frontend
    participant Grails as Grails API
    participant Gemini as Gemini API
    participant ML as Python ML Engine
    participant DB as PostgreSQL

    User->>Nuxt: Login
    Nuxt->>Grails: POST /api/auth/login
    Grails-->>Nuxt: JWT Token

    User->>Nuxt: View Dashboard
    Nuxt->>Grails: GET /api/clusters (with JWT)
    Grails->>DB: Fetch clusters
    DB-->>Grails: Cluster data
    Grails-->>Nuxt: Clusters + sentiment
    Nuxt->>Nuxt: Render D3.js graph

    User->>Nuxt: Click cluster bubble
    Nuxt->>Grails: GET /api/clusters/{id}
    Grails->>DB: Fetch cluster posts
    DB-->>Grails: Posts in cluster
    Grails-->>Nuxt: Cluster detail + insight
    Nuxt->>Nuxt: Show detail panel
```

---

## Data Flow: ML Analysis Trigger

```mermaid
sequenceDiagram
    participant Admin
    participant Nuxt as Nuxt Frontend
    participant Grails as Grails API
    participant ML as Python ML Engine
    participant Gemini as Gemini API
    participant DB as PostgreSQL

    Admin->>Nuxt: Trigger Analysis
    Nuxt->>Grails: POST /api/analysis/trigger

    Grails->>DB: Fetch posts
    DB-->>Grails: Raw posts

    Grails->>ML: POST /api/analyze (posts batch)
    
    Note over ML: Preprocessing
    ML->>ML: Tokenize, TF-IDF

    Note over ML: Clustering
    ML->>ML: K-Means (k=5-7)

    Note over ML: Sentiment
    ML->>ML: VADER scoring

    ML-->>Grails: Clusters + sentiment scores

    Grails->>DB: Store clusters
    
    loop For each cluster
        Grails->>Gemini: Generate insight
        Gemini-->>Grails: Business insight text
        Grails->>DB: Store insight
    end

    Grails-->>Nuxt: Analysis complete
    Nuxt-->>Admin: Refresh dashboard
```

---

## Component Responsibilities

```mermaid
graph LR
    subgraph frontend["frontend/"]
        PAGES[pages/<br/>index.vue, data.vue,<br/>login.vue]
        COMPONENTS[components/<br/>BubbleChart, ClusterDetail,<br/>UserMenu, ThemeToggle,<br/>StatsCard, SentimentBadge]
        COMPOSABLES[composables/<br/>useApi, useAuth,<br/>useColorMode, useServiceHealth]
    end

    subgraph backend["backend/"]
        CONTROLLERS[controllers/<br/>Post, Cluster, Analysis,<br/>Auth, AiInsight, Health]
        SERVICES[services/<br/>Gemini, MlEngine,<br/>AiInsight, Auth, DataLoader]
        DOMAIN[domain/<br/>Post, Cluster, AnalysisRun,<br/>AiInsight, User]
    end

    subgraph mlengine["ml-engine/"]
        API[api.py<br/>Flask routes]
        CLUSTERING[clustering.py<br/>K-Means + TF-IDF]
        SENTIMENT[sentiment.py<br/>VADER + Gemini]
        GEMINI_PY[sentiment_gemini.py<br/>Gemini API]
        PREPROCESS[preprocessing.py<br/>Tokenization]
    end

    subgraph config["config/"]
        TAXONOMY[taxonomy.yaml<br/>Auditable categories]
    end

    subgraph data["data/"]
        FIXTURES[fixtures/<br/>50 mock posts]
    end

    PAGES --> COMPOSABLES
    COMPONENTS --> COMPOSABLES
    COMPOSABLES --> CONTROLLERS
    CONTROLLERS --> SERVICES
    CONTROLLERS --> DOMAIN
    SERVICES --> API
    API --> CLUSTERING
    API --> SENTIMENT
    CLUSTERING --> PREPROCESS
```

---

## D3.js Visualization Structure

```mermaid
graph TD
    subgraph Graph["Bubble Chart (D3.js Pack Layout)"]
        subgraph Nodes["Cluster Bubbles"]
            N1["🟢 Model Demand<br/>+0.34 sentiment<br/>22 posts"]
            N2["🟡 Uptime & Reliability<br/>-0.14 sentiment<br/>13 posts"]
            N3["🟡 EV Adoption<br/>+0.12 sentiment<br/>10 posts"]
            N4["🟡 Model Demand (567)<br/>+0.23 sentiment<br/>5 posts"]
        end
    end

    subgraph Legend["Legend"]
        GREEN["🟢 Positive (≥0.3)"]
        YELLOW["🟡 Neutral (-0.3 to 0.3)"]
        RED["🔴 Negative (≤-0.3)"]
        SIZE["Bubble size = post count"]
    end

    subgraph Interaction["User Interactions"]
        HOVER["Hover → Highlight"]
        CLICK["Click → Detail Panel"]
    end
```

---

## Authentication Flow

```mermaid
sequenceDiagram
    participant User
    participant Nuxt as Nuxt Frontend
    participant Grails as Grails API
    participant DB as PostgreSQL

    User->>Nuxt: Enter credentials
    Nuxt->>Grails: POST /api/auth/login
    Grails->>DB: Validate user
    DB-->>Grails: User record
    Grails->>Grails: Generate JWT
    Grails-->>Nuxt: { success, token, user }
    Nuxt->>Nuxt: Store token + user in localStorage

    Note over Nuxt: Subsequent requests

    Nuxt->>Grails: GET /api/clusters<br/>Authorization: Bearer {token}
    Grails->>Grails: Validate JWT
    Grails-->>Nuxt: Protected data

    Note over Nuxt: Optional user refresh

    Nuxt->>Grails: GET /api/auth/me<br/>Authorization: Bearer {token}
    Grails-->>Nuxt: { user }
```

---

## Deployment Architecture

```mermaid
flowchart LR
    subgraph Netlify["Netlify (Frontend)"]
        NUXT[Nuxt 4 SSR/Static]
    end

    subgraph Render["Render (Backend)"]
        GRAILS[Grails Web Service<br/>Java 17]
        PYTHON[Python Web Service<br/>Flask + Gunicorn]
        PG[(PostgreSQL<br/>Managed)]
    end

    subgraph External["External APIs"]
        GEMINI[Google Gemini<br/>2.0 Flash]
    end

    NUXT <-->|HTTPS| GRAILS
    GRAILS <-->|Internal| PYTHON
    GRAILS <-->|HTTPS| GEMINI
    GRAILS <--> PG
    PYTHON <--> PG

    %% Styling
    classDef netlify fill:#00c7b7,stroke:#004d40,color:#fff
    classDef render fill:#46e3b7,stroke:#1b5e20
    classDef external fill:#4285f4,stroke:#1a237e,color:#fff

    class NUXT netlify
    class GRAILS,PYTHON,PG render
    class GEMINI external
```

---

## Data Models

### Post
```
Post {
  id: Long
  externalId: String       // Original platform ID
  source: String           // twitter, youtube, reddit, forums, news
  content: String          // Raw text content
  author: String?
  authorUrl: String?
  postUrl: String?
  publishedAt: DateTime?
  fetchedAt: DateTime?

  // ML-generated fields
  sentimentCompound: Float?   // -1.0 to 1.0
  sentimentPositive: Float?
  sentimentNegative: Float?
  sentimentNeutral: Float?
  sentimentLabel: String?     // positive, negative, neutral

  clusterId: String?          // FK to Cluster
  keywords: String[]?         // Comma-separated keywords

  createdAt: DateTime
  updatedAt: DateTime
}
```

### Cluster
```
Cluster {
  id: Long
  taxonomyId: String         // Maps to config/taxonomy.yaml
  label: String              // Human-readable label
  description: String?

  // Aggregated data
  keywords: String[]?        // Top keywords
  sentiment: Float?          // Average sentiment (-1 to 1)
  sentimentLabel: String?    // positive, negative, neutral
  postCount: Integer?

  // AI-generated insight
  insight: String?           // Gemini-generated insight text

  analysisRunId: String?     // Analysis run that created this cluster
  createdAt: DateTime
  updatedAt: DateTime
}
```

### User
```
User {
  id: Long
  email: String
  passwordHash: String
  role: String               // admin, viewer

  enabled: Boolean
  accountExpired: Boolean
  accountLocked: Boolean
  passwordExpired: Boolean

  lastLoginAt: DateTime?
  createdAt: DateTime
  updatedAt: DateTime
}
```

### AnalysisRun
```
AnalysisRun {
  id: Long
  status: String              // pending, processing, completed, failed

  // Counts
  postsAnalyzed: Integer?
  clustersCreated: Integer?
  insightsGenerated: Integer?

  // Timing
  startedAt: DateTime?
  completedAt: DateTime?
  durationMs: Long?

  // Error tracking
  error: String?

  createdAt: DateTime
  updatedAt: DateTime
}
```

### AiInsight
```
AiInsight {
  id: Long
  type: String               // trend_analysis, recommendations, executive_summary
  content: String            // Generated insight text
  source: String?            // all, twitter, youtube, forums, reddit, news

  analysisRunId: Long?       // AnalysisRun that generated this insight

  // Metadata at generation time
  postsAnalyzed: Integer?
  clustersCount: Integer?

  createdAt: DateTime
  updatedAt: DateTime
}
```

---

## Viewing These Diagrams

These Mermaid diagrams render in:
- **GitHub** — Automatic rendering in markdown files
- **VS Code** — Install "Markdown Preview Mermaid Support" extension
- **Mermaid Live Editor** — [mermaid.live](https://mermaid.live)
