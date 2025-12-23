# Frontend — Nuxt 3 Dashboard

> Interactive sentiment analysis dashboard with D3.js visualizations.

## Tech Stack

- **Nuxt 3** — Vue 3 framework with SSR/SSG
- **TailwindCSS** — Utility-first styling
- **D3.js** — Interactive bubble chart visualization
- **TypeScript** — Type-safe composables and components

## Features

- 🎨 Dark/light mode with system preference detection
- 📊 D3.js bubble chart for cluster visualization
- 🔐 JWT authentication (login/register/logout)
- 📱 Responsive design for mobile and desktop
- 🔍 Sentiment filtering and sorting
- 🤖 AI-powered insights display (Gemini)

## Project Structure

```
frontend/
├── components/
│   ├── BubbleChart.vue      # D3.js cluster visualization
│   ├── ClusterDetail.vue    # Cluster detail modal
│   ├── UserMenu.vue         # Auth dropdown menu
│   ├── ThemeToggle.vue      # Dark/light mode toggle
│   ├── LoadingScreen.vue    # Loading state component
│   └── StatsCard.vue        # Dashboard stat cards
├── composables/
│   ├── useApi.ts            # API client with auth headers
│   └── useAuth.ts           # Authentication state management
├── pages/
│   ├── index.vue            # Main dashboard
│   ├── data.vue             # Data management (admin)
│   ├── login.vue            # Login page
│   └── register.vue         # Registration page
├── types/
│   └── models.ts            # TypeScript interfaces
└── tests/
    └── components/          # Vitest component tests
```

## Setup

```bash
npm install
```

## Development

```bash
npm run dev
```

Opens at http://localhost:3000

## Environment Variables

Create `.env` in the frontend directory:

```env
NUXT_PUBLIC_API_URL=http://localhost:8080
```

## Testing

```bash
# Run tests
npm test

# Watch mode
npm run test:watch

# Coverage report
npm run test:coverage
```

## Build for Production

```bash
npm run build
npm run preview  # Preview production build
```

## Key Components

### BubbleChart.vue
D3.js pack layout visualization showing clusters as bubbles:
- Size = post count
- Color = sentiment (green/yellow/red)
- Click to view cluster details

### useAuth.ts
Authentication composable managing:
- Login/register/logout
- JWT token storage
- User state (reactive)

### useApi.ts
API client with:
- Auth header injection
- All backend endpoints
- TypeScript return types

## Deployment

Configured for Netlify deployment. See root `SPRINTS.md` for deployment instructions.
