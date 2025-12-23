/**
 * Data models for Peterbilt Sentiment Analyzer
 * These types are shared across the frontend application
 */

// =============================================================================
// POST - Raw social media/forum post
// =============================================================================
export interface Post {
  id: string;
  source: 'twitter' | 'youtube' | 'forums';
  externalId: string;          // Original platform ID
  content: string;             // Raw text content
  author: string;              // Username or handle
  authorUrl?: string;          // Link to author profile
  postUrl?: string;            // Link to original post
  publishedAt: string | number;  // ISO date string or epoch milliseconds
  fetchedAt: string | number;    // ISO date string or epoch milliseconds
  
  // ML-generated fields (populated after analysis)
  sentimentCompound?: number;      // Overall score: -1 to 1
  sentimentPositive?: number;      // Positive component: 0 to 1
  sentimentNegative?: number;      // Negative component: 0 to 1
  sentimentNeutral?: number;       // Neutral component: 0 to 1
  sentimentLabel?: 'positive' | 'negative' | 'neutral';
  clusterId?: string;
  keywords?: string[];
}

// =============================================================================
// SENTIMENT SCORE
// =============================================================================
export interface SentimentScore {
  compound: number;            // Overall score: -1 (negative) to 1 (positive)
  positive: number;            // Positive component: 0 to 1
  negative: number;            // Negative component: 0 to 1
  neutral: number;             // Neutral component: 0 to 1
  label: 'positive' | 'negative' | 'neutral';
}

// =============================================================================
// CLUSTER - Group of related posts
// =============================================================================
export interface Cluster {
  id: string;
  taxonomyId: string;          // Maps to taxonomy.yaml cluster id
  label: string;               // Human-readable label
  description?: string;        // Business context
  
  // Aggregated data
  keywords: string[];          // Top keywords in this cluster
  sentiment: number;           // Average sentiment: -1 to 1
  sentimentLabel: 'positive' | 'negative' | 'neutral';
  postCount: number;           // Number of posts in cluster
  postIds: string[];           // IDs of posts in this cluster
  
  // AI-generated insight
  insight?: string;            // Gemini-generated business insight
  
  // Metadata
  analysisRunId: string;       // Which analysis run created this
  createdAt: string;
}

// =============================================================================
// INSIGHT - AI-generated business summary
// =============================================================================
export interface Insight {
  id: string;
  clusterId: string;
  clusterLabel: string;
  
  summary: string;             // 2-3 sentence summary
  businessImplication: string; // "Why it matters" for marketing/sales
  recommendedAction?: string;  // Suggested next step
  
  generatedAt: string;
  model: string;               // e.g., "gemini-1.5-flash"
}

// =============================================================================
// ANALYSIS RUN - Tracks a complete analysis job
// =============================================================================
export interface AnalysisRun {
  id: string;
  status: 'pending' | 'processing' | 'completed' | 'failed';
  
  // Counts
  postsAnalyzed: number;
  clustersCreated: number;
  insightsGenerated: number;
  
  // Timing
  startedAt: string;
  completedAt?: string;
  durationMs?: number;
  
  // Error tracking
  error?: string;
}

// =============================================================================
// TAXONOMY - Loaded from config/taxonomy.yaml
// =============================================================================
export interface TaxonomyCluster {
  id: string;
  label: string;
  description: string;
  businessValue: string;
  keywords: string[];
  sentimentIndicators: {
    positive: string[];
    negative: string[];
  };
}

export interface TaxonomySource {
  id: string;
  label: string;
  description: string;
  weight: number;
}

export interface Taxonomy {
  clusters: TaxonomyCluster[];
  sources: TaxonomySource[];
  sentiment: {
    thresholds: {
      positive: number;
      negative: number;
    };
    colors: {
      positive: string;
      neutral: string;
      negative: string;
    };
  };
}

// =============================================================================
// API RESPONSE WRAPPERS
// =============================================================================
export interface ApiResponse<T> {
  data: T;
  success: boolean;
  error?: string;
  timestamp: string;
}

export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
}

// =============================================================================
// DASHBOARD VIEW MODELS
// =============================================================================
export interface ClusterNode {
  id: string;
  label: string;
  sentiment: number;
  sentimentLabel: 'positive' | 'negative' | 'neutral';
  postCount: number;
  keywords: string[];
  x?: number;                  // D3 position
  y?: number;                  // D3 position
}

// Simplified cluster summary for dashboard
export interface ClusterSummary {
  id: string;
  label: string;
  sentiment: number;
  sentimentLabel: 'positive' | 'negative' | 'neutral';
  postCount: number;
}

export interface DashboardSummary {
  totalPosts: number;
  totalClusters: number;
  averageSentiment: number;
  sentimentDistribution: {
    positive: number;
    neutral: number;
    negative: number;
  };
  topClusters: ClusterSummary[];
  recentInsights?: Insight[];
  lastAnalysisAt?: string;
}
