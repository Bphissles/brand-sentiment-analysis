<script setup lang="ts">
import type {
  Cluster,
  DashboardSummary,
  InsightsResponse,
  Post,
  SourceFilter,
  SentimentFilter,
  SentimentSort
} from '~/types/models'

const api = useApi()
const { initAuth, user } = useAuth()

// Check if user is admin
const isAdmin = computed(() => user.value?.role === 'admin')

// State
const clusters = ref<Cluster[]>([])
const summary = ref<DashboardSummary | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const selectedCluster = ref<Cluster | null>(null)
const selectedClusterPosts = ref<Post[]>([])
const showDetail = ref(false)
const selectedSource = ref<SourceFilter>('all')

// Cluster filter state
const clusterSentimentFilter = ref<SentimentFilter>('all')

// Sentiment posts modal state
const showSentimentPosts = ref(false)
const sentimentPostsFilter = ref<'positive' | 'neutral' | 'negative'>('positive')
const sentimentPosts = ref<Post[]>([])
const loadingSentimentPosts = ref(false)
const sentimentPostsSort = ref<SentimentSort>('strongest')

// AI Insights state
const aiInsights = ref<InsightsResponse | null>(null)
const loadingInsights = ref(false)
const generatingInsights = ref(false)

// Responsive chart dimensions
const chartWidth = ref(600)
const chartHeight = ref(450)

// Update chart dimensions based on screen size
const updateChartDimensions = () => {
  const width = window.innerWidth
  if (width < 640) {
    // Mobile
    chartWidth.value = Math.min(width - 32, 350)
    chartHeight.value = 300
  } else if (width < 1024) {
    // Tablet
    chartWidth.value = Math.min(width - 100, 500)
    chartHeight.value = 400
  } else {
    // Desktop
    chartWidth.value = 600
    chartHeight.value = 450
  }
}

// Listen for window resize
onMounted(() => {
  updateChartDimensions()
  window.addEventListener('resize', updateChartDimensions)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateChartDimensions)
})

// Load data on mount
onMounted(async () => {
  initAuth()
  await loadData()
})

const loadData = async () => {
  loading.value = true
  error.value = null
  
  try {
    const source = selectedSource.value !== 'all' ? selectedSource.value : undefined
    const [clustersData, summaryData] = await Promise.all([
      api.fetchClusters(source),
      api.fetchSummary(source)
    ])
    
    clusters.value = clustersData
    summary.value = summaryData
    
    // Load AI insights (cached)
    await loadInsights()
  } catch (e: any) {
    error.value = e.message || 'Failed to load data'
    console.error('Failed to load data:', e)
  } finally {
    loading.value = false
  }
}

const loadInsights = async () => {
  loadingInsights.value = true
  try {
    const source = selectedSource.value !== 'all' ? selectedSource.value : undefined
    aiInsights.value = await api.fetchInsights(source)
  } catch (e) {
    console.error('Failed to load insights:', e)
  } finally {
    loadingInsights.value = false
  }
}

const generateNewInsights = async () => {
  generatingInsights.value = true
  try {
    const source = selectedSource.value !== 'all' ? selectedSource.value : undefined
    aiInsights.value = await api.generateInsights(source)
  } catch (e) {
    console.error('Failed to generate insights:', e)
  } finally {
    generatingInsights.value = false
  }
}

const handleClusterClick = async (cluster: Cluster) => {
  selectedCluster.value = cluster
  showDetail.value = true
  
  try {
    const data = await api.fetchCluster(cluster.id)
    selectedClusterPosts.value = data.posts || []
  } catch (e) {
    console.error('Failed to load cluster posts:', e)
    selectedClusterPosts.value = []
  }
}

const closeDetail = () => {
  showDetail.value = false
  selectedCluster.value = null
  selectedClusterPosts.value = []
}

const handleSourceChange = async () => {
  await loadData()
}

// Computed
const sentimentTrend = computed(() => {
  if (!summary.value) return 'neutral'
  const avg = summary.value.averageSentiment || 0
  if (avg >= 0.1) return 'up'
  if (avg <= -0.1) return 'down'
  return 'neutral'
})

const positivePostsTrend = computed(() => {
  if (!summary.value || !summary.value.sentimentDistribution) return 'neutral'
  const { positive = 0, negative = 0 } = summary.value.sentimentDistribution
  if (positive > negative) return 'up'
  if (positive < negative) return 'down'
  return 'neutral'
})

// Filtered clusters based on sentiment filter
const filteredClusters = computed(() => {
  if (clusterSentimentFilter.value === 'all') {
    return clusters.value
  }
  return clusters.value.filter(c => c.sentimentLabel === clusterSentimentFilter.value)
})

// View posts by sentiment
const viewPostsBySentiment = async (sentiment: 'positive' | 'neutral' | 'negative') => {
  sentimentPostsFilter.value = sentiment
  showSentimentPosts.value = true
  loadingSentimentPosts.value = true
  
  try {
    // Apply source filter if selected
    const source = selectedSource.value !== 'all' ? selectedSource.value : undefined
    const posts = await api.fetchPosts({ sentiment, source })
    sentimentPosts.value = posts
  } catch (e) {
    console.error('Failed to load sentiment posts:', e)
    sentimentPosts.value = []
  } finally {
    loadingSentimentPosts.value = false
  }
}

const closeSentimentPosts = () => {
  showSentimentPosts.value = false
  sentimentPosts.value = []
  sentimentPostsSort.value = 'strongest'
}

// Sorted sentiment posts
const sortedSentimentPosts = computed(() => {
  const posts = [...sentimentPosts.value]
  
  switch (sentimentPostsSort.value) {
    case 'strongest':
      // Sort by absolute sentiment score (strongest first)
      return posts.sort((a, b) => 
        Math.abs(b.sentimentCompound || 0) - Math.abs(a.sentimentCompound || 0)
      )
    case 'newest':
      return posts.sort((a, b) => 
        new Date(b.publishedAt || 0).getTime() - new Date(a.publishedAt || 0).getTime()
      )
    case 'oldest':
      return posts.sort((a, b) => 
        new Date(a.publishedAt || 0).getTime() - new Date(b.publishedAt || 0).getTime()
      )
    default:
      return posts
  }
})

// ESC key handler for modals
const handleEscKey = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    if (showSentimentPosts.value) {
      closeSentimentPosts()
    }
    if (showDetail.value) {
      closeDetail()
    }
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleEscKey)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleEscKey)
})
</script>

<template>
  <div class="min-h-screen bg-slate-50 dark:bg-slate-900 transition-colors">
    <!-- Header -->
    <header class="bg-gradient-to-r from-slate-900 to-slate-800 shadow-lg">
      <div class="max-w-7xl mx-auto px-4 py-5 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center">
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-lg flex items-center justify-center">
              <span class="text-white font-bold text-lg">V</span>
            </div>
            <div>
              <h1 class="text-lg font-semibold text-white tracking-tight">
                Voice of the Operator
              </h1>
              <p class="text-sm text-slate-400">Real-time Sentiment Intelligence</p>
            </div>
          </div>
          <div class="flex items-center gap-2 lg:gap-3">
            <select 
              v-model="selectedSource"
              @change="handleSourceChange"
              class="px-2 lg:px-3 py-2 bg-white  border border-slate-300 dark:border-slate-600 rounded-lg text-sm focus:ring-2 focus:ring-cyan-500 focus:border-transparent"
            >
              <option value="all">All Sources</option>
              <option value="twitter">Twitter/X</option>
              <option value="youtube">YouTube</option>
              <option value="reddit">Reddit</option>
              <option value="forums">Forums</option>
              <option value="news">News</option>
            </select>
            <NuxtLink 
              v-if="isAdmin"
              to="/data"
              class="px-2 lg:px-3 py-2 bg-slate-700 text-slate-200 text-sm font-medium rounded-lg hover:bg-slate-600 border border-slate-600 transition-colors"
              title="Data Management"
            >
              <svg class="w-4 h-4 lg:w-5 lg:h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
              </svg>
            </NuxtLink>
            <ThemeToggle />
            <UserMenu />
          </div>
        </div>
      </div>
    </header>

    <main class="max-w-7xl mx-auto px-4 py-6 sm:px-6 lg:px-8">
      <!-- Error Alert -->
      <div v-if="error" class="mb-6 p-4 bg-red-100 dark:bg-red-900/30 border border-red-300 dark:border-red-800 text-red-700 dark:text-red-400 rounded-lg">
        {{ error }}
        <button @click="error = null" class="ml-2 font-bold">×</button>
      </div>

      <!-- Loading State -->
      <div v-if="loading && !clusters.length" class="flex justify-center items-center h-64">
        <div class="text-center">
          <LoadingSpinner size="lg" class="mx-auto mb-3" />
          <p class="text-slate-500 dark:text-slate-400 font-medium">Loading dashboard...</p>
        </div>
      </div>

      <!-- Dashboard Content -->
      <template v-else>
        
        <!-- Executive Summary Section -->
        <div class="bg-slate-700/50 rounded-xl shadow-lg p-6 mb-6 border border-slate-600/50">
          <div class="flex items-center justify-between mb-4">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 bg-gradient-to-br from-amber-400 to-orange-500 rounded-lg flex items-center justify-center">
                <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <div>
                <h3 class="text-lg font-semibold text-white">Executive Summary</h3>
                <p class="text-sm text-slate-400">AI-generated narrative for stakeholder reporting</p>
              </div>
            </div>
            <span 
              v-if="aiInsights?.cached && aiInsights?.executiveSummary"
              class="px-3 py-1 bg-emerald-500/20 text-emerald-400 text-xs font-medium rounded-full border border-emerald-500/30"
            >
              AI Generated
            </span>
          </div>
          
          <div class="bg-slate-700/50 rounded-lg p-4 border border-slate-600/50">
            <p class="text-slate-300 text-sm leading-relaxed">
              <template v-if="loadingInsights">
                <span class="text-slate-400 italic">Loading executive summary...</span>
              </template>
              <template v-else-if="aiInsights?.executiveSummary">
                {{ aiInsights.executiveSummary }}
              </template>
              <template v-else-if="clusters.length > 0">
                <span class="text-white font-medium">Summary:</span> Analysis of {{ summary?.totalPosts || 0 }} social media posts reveals {{ clusters.length }} distinct topic clusters. 
                The overall brand sentiment is <span :class="{'text-emerald-400': (summary?.averageSentiment || 0) >= 0.1, 'text-rose-400': (summary?.averageSentiment || 0) <= -0.1, 'text-amber-400': Math.abs(summary?.averageSentiment || 0) < 0.1}">{{ (summary?.averageSentiment || 0) >= 0.1 ? 'positive' : (summary?.averageSentiment || 0) <= -0.1 ? 'negative' : 'neutral' }}</span> 
                with a compound score of {{ (summary?.averageSentiment || 0).toFixed(2) }}. 
                Key discussion topics include {{ clusters.slice(0, 3).map(c => c.label).join(', ') || 'various themes' }}.
                <span class="text-slate-400 italic"> Run analysis from the Data page for enhanced AI narrative.</span>
              </template>
              <template v-else>
                <span class="text-slate-400 italic">Load data and run analysis to generate an executive summary. This section will provide a comprehensive AI-generated narrative suitable for stakeholder presentations and reports.</span>
              </template>
            </p>
          </div>
        </div>

        <!-- Stats Row -->
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <StatsCard 
            title="Total Posts" 
            :value="summary?.totalPosts || 0" 
          />
          <StatsCard 
            title="Clusters" 
            :value="summary?.totalClusters || 0" 
          />
          <StatsCard 
            title="Avg Sentiment" 
            :value="(summary?.averageSentiment || 0).toFixed(2)"
            :trend="sentimentTrend"
          />
          <StatsCard 
            title="Positive Posts" 
            :value="summary?.sentimentDistribution?.positive || 0"
            :trend="positivePostsTrend"
          />
        </div>

        <!-- Main Content: Responsive Layout -->
        <div class="flex flex-col lg:flex-row gap-6 mb-6">
          <!-- Left: Visualization -->
          <div class="flex-1 bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-200 dark:border-slate-700 p-4 lg:p-6">
            <div class="flex justify-between items-start mb-4">
              <div>
                <h2 class="text-lg font-semibold text-slate-800 dark:text-slate-100">Topic Clusters</h2>
                <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">
                  Click on a bubble to explore posts. Size indicates volume, color shows sentiment.
                </p>
              </div>
              <div class="flex items-center gap-2 text-xs text-slate-400 dark:text-slate-500">
                <span class="flex items-center gap-1">
                  <span class="w-2 h-2 rounded-full bg-emerald-500"></span> Positive
                </span>
                <span class="flex items-center gap-1">
                  <span class="w-2 h-2 rounded-full bg-amber-500"></span> Neutral
                </span>
                <span class="flex items-center gap-1">
                  <span class="w-2 h-2 rounded-full bg-rose-500"></span> Negative
                </span>
              </div>
            </div>
            
            <div v-if="clusters.length" class="flex justify-center">
              <BubbleChart 
                :clusters="clusters" 
                :width="chartWidth" 
                :height="chartHeight"
                @cluster-click="handleClusterClick"
              />
            </div>
            <div v-else class="text-center py-16 text-slate-400 dark:text-slate-500">
              <svg class="w-12 h-12 mx-auto mb-3 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
              <p class="font-medium">No clusters found</p>
              <p class="text-sm mt-1">Click "Run Analysis" to analyze posts</p>
            </div>
          </div>

          <!-- Right: Cluster List -->
          <div class="w-full lg:w-1/3 xl:w-1/4 bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-200 dark:border-slate-700 p-4 flex flex-col">
            <div class="flex items-center justify-between mb-3">
              <h2 class="text-lg font-semibold text-slate-800 dark:text-slate-100">Cluster Breakdown</h2>
              <!-- Sentiment Filter -->
              <select 
                v-model="clusterSentimentFilter"
                class="text-xs px-2 py-1 bg-slate-100 dark:bg-slate-700 border border-slate-200 dark:border-slate-600 rounded-lg text-slate-600 dark:text-slate-300 focus:ring-2 focus:ring-cyan-500 focus:border-transparent"
              >
                <option value="all">All</option>
                <option value="positive">Positive</option>
                <option value="neutral">Neutral</option>
                <option value="negative">Negative</option>
              </select>
            </div>
            <div class="flex-1 overflow-y-auto space-y-3 pr-2 scrollbar-custom max-h-96 lg:max-h-[480px]">
              <ClusterCard 
                v-for="cluster in filteredClusters" 
                :key="cluster.id"
                :cluster="cluster"
                @click="handleClusterClick"
              />
              <div v-if="filteredClusters.length === 0" class="text-center py-8 text-slate-400 dark:text-slate-500">
                <p class="text-sm">No clusters match this filter</p>
              </div>
            </div>
          </div>
        </div>

        <!-- AI Insights Section -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
          <!-- Sentiment Trends -->
          <div class="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-200 dark:border-slate-700 p-6">
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center gap-2">
                <div class="w-8 h-8 bg-gradient-to-br from-violet-500 to-purple-600 rounded-lg flex items-center justify-center">
                  <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                  </svg>
                </div>
                <h3 class="text-lg font-semibold text-slate-800 dark:text-slate-100">Sentiment Trends</h3>
              </div>
              <span class="px-2 py-1 bg-violet-100 dark:bg-violet-900/30 text-violet-600 dark:text-violet-400 text-xs font-medium rounded-full">
                AI Enhanced
              </span>
            </div>
            
            <!-- Sentiment Distribution Bar -->
            <div class="mb-4">
              <div class="flex justify-between text-sm text-slate-600 dark:text-slate-400 mb-2">
                <span>Sentiment Distribution</span>
                <span>{{ summary?.totalPosts || 0 }} posts analyzed</span>
              </div>
              <div class="h-4 bg-slate-100 dark:bg-slate-700 rounded-full overflow-hidden flex">
                <div 
                  class="bg-emerald-500 transition-all duration-500"
                  :style="{ width: `${(summary?.sentimentDistribution?.positive || 0) / Math.max(summary?.totalPosts || 1, 1) * 100}%` }"
                ></div>
                <div 
                  class="bg-amber-500 transition-all duration-500"
                  :style="{ width: `${(summary?.sentimentDistribution?.neutral || 0) / Math.max(summary?.totalPosts || 1, 1) * 100}%` }"
                ></div>
                <div 
                  class="bg-rose-500 transition-all duration-500"
                  :style="{ width: `${(summary?.sentimentDistribution?.negative || 0) / Math.max(summary?.totalPosts || 1, 1) * 100}%` }"
                ></div>
              </div>
              <div class="flex justify-between text-xs mt-2">
                <button 
                  @click="viewPostsBySentiment('positive')"
                  class="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-emerald-100 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400 hover:bg-emerald-200 dark:hover:bg-emerald-900/50 transition-colors cursor-pointer font-medium"
                  :disabled="!summary?.sentimentDistribution?.positive"
                  :class="{ 'opacity-50 cursor-not-allowed': !summary?.sentimentDistribution?.positive }"
                >
                  <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
                  Positive ({{ summary?.sentimentDistribution?.positive || 0 }})
                </button>
                <button 
                  @click="viewPostsBySentiment('neutral')"
                  class="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400 hover:bg-amber-200 dark:hover:bg-amber-900/50 transition-colors cursor-pointer font-medium"
                  :disabled="!summary?.sentimentDistribution?.neutral"
                  :class="{ 'opacity-50 cursor-not-allowed': !summary?.sentimentDistribution?.neutral }"
                >
                  <span class="w-2 h-2 rounded-full bg-amber-500"></span>
                  Neutral ({{ summary?.sentimentDistribution?.neutral || 0 }})
                </button>
                <button 
                  @click="viewPostsBySentiment('negative')"
                  class="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-rose-100 dark:bg-rose-900/30 text-rose-700 dark:text-rose-400 hover:bg-rose-200 dark:hover:bg-rose-900/50 transition-colors cursor-pointer font-medium"
                  :disabled="!summary?.sentimentDistribution?.negative"
                  :class="{ 'opacity-50 cursor-not-allowed': !summary?.sentimentDistribution?.negative }"
                >
                  <span class="w-2 h-2 rounded-full bg-rose-500"></span>
                  Negative ({{ summary?.sentimentDistribution?.negative || 0 }})
                </button>
              </div>
            </div>

            <!-- AI Trend Analysis -->
            <div class="p-4 bg-slate-50 dark:bg-slate-700/50 rounded-lg border border-slate-200 dark:border-slate-600">
              <div class="flex items-start gap-3">
                <div class="w-6 h-6 bg-violet-100 dark:bg-violet-900/50 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
                  <svg class="w-3 h-3 text-violet-600 dark:text-violet-400" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                  </svg>
                </div>
                <div class="flex-1">
                  <p class="text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">AI Trend Analysis</p>
                  <p class="text-sm text-slate-600 dark:text-slate-300">
                    <template v-if="loadingInsights">
                      <span class="italic text-slate-400">Loading insights...</span>
                    </template>
                    <template v-else-if="aiInsights?.trendAnalysis">
                      {{ aiInsights.trendAnalysis }}
                    </template>
                    <template v-else>
                      <span class="italic text-slate-400">Run analysis from the Data page to generate AI-powered insights.</span>
                    </template>
                  </p>
                </div>
              </div>
            </div>
          </div>

          <!-- Key Themes & Recommendations -->
          <div class="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-200 dark:border-slate-700 p-6">
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center gap-2">
                <div class="w-8 h-8 bg-gradient-to-br from-cyan-500 to-blue-600 rounded-lg flex items-center justify-center">
                  <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                  </svg>
                </div>
                <h3 class="text-lg font-semibold text-slate-800 dark:text-slate-100">Key Themes</h3>
              </div>
              <span class="px-2 py-1 bg-cyan-100 dark:bg-cyan-900/30 text-cyan-600 dark:text-cyan-400 text-xs font-medium rounded-full">
                AI Synthesized
              </span>
            </div>

            <!-- Theme Tags -->
            <div class="flex flex-wrap gap-2 mb-4">
              <template v-if="clusters.length > 0">
                <span 
                  v-for="cluster in clusters.slice(0, 6)" 
                  :key="cluster.id"
                  class="px-3 py-1.5 rounded-full text-sm font-medium transition-colors cursor-pointer hover:opacity-80"
                  :class="{
                    'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400': cluster.sentimentLabel === 'positive',
                    'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400': cluster.sentimentLabel === 'neutral',
                    'bg-rose-100 dark:bg-rose-900/30 text-rose-700 dark:text-rose-400': cluster.sentimentLabel === 'negative'
                  }"
                  @click="handleClusterClick(cluster)"
                >
                  {{ cluster.label }}
                </span>
              </template>
              <template v-else>
                <span class="px-3 py-1.5 bg-slate-100 dark:bg-slate-700 text-slate-400 rounded-full text-sm">
                  No themes detected
                </span>
              </template>
            </div>

            <!-- AI Recommendations -->
            <div class="p-4 bg-slate-50 dark:bg-slate-700/50 rounded-lg border border-slate-200 dark:border-slate-600">
              <div class="flex items-start gap-3">
                <div class="w-6 h-6 bg-cyan-100 dark:bg-cyan-900/50 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
                  <svg class="w-3 h-3 text-cyan-600 dark:text-cyan-400" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                  </svg>
                </div>
                <div class="flex-1">
                  <p class="text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">AI Recommendations</p>
                  <p class="text-sm text-slate-600 dark:text-slate-300">
                    <template v-if="loadingInsights">
                      <span class="italic text-slate-400">Loading insights...</span>
                    </template>
                    <template v-else-if="aiInsights?.recommendations">
                      {{ aiInsights.recommendations }}
                    </template>
                    <template v-else>
                      <span class="italic text-slate-400">Run analysis from the Data page to generate AI-powered recommendations.</span>
                    </template>
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer Info -->
        <div class="flex justify-between items-center text-xs text-slate-400 dark:text-slate-500 pt-4 border-t border-slate-200 dark:border-slate-700">
          <span>Data sources: Twitter/X, YouTube, Reddit, Forums, News</span>
          <span>Powered by K-Means clustering + Google Gemini sentiment analysis</span>
        </div>
      </template>
    </main>

    <!-- Cluster Detail Modal -->
    <ClusterDetail 
      v-if="showDetail && selectedCluster"
      :cluster="selectedCluster"
      :posts="selectedClusterPosts"
      @close="closeDetail"
    />

    <!-- Sentiment Posts Modal -->
    <Teleport to="body">
      <div 
        v-if="showSentimentPosts" 
        class="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
        @click.self="closeSentimentPosts"
      >
        <div class="bg-white dark:bg-slate-800 rounded-xl shadow-2xl w-full max-w-3xl max-h-[85vh] flex flex-col">
          <!-- Header -->
          <div class="flex items-center justify-between p-4 border-b border-slate-200 dark:border-slate-700">
            <div class="flex items-center gap-3">
              <div 
                class="w-8 h-8 rounded-lg flex items-center justify-center"
                :class="{
                  'bg-emerald-100 dark:bg-emerald-900/30': sentimentPostsFilter === 'positive',
                  'bg-amber-100 dark:bg-amber-900/30': sentimentPostsFilter === 'neutral',
                  'bg-rose-100 dark:bg-rose-900/30': sentimentPostsFilter === 'negative'
                }"
              >
                <span 
                  class="w-3 h-3 rounded-full"
                  :class="{
                    'bg-emerald-500': sentimentPostsFilter === 'positive',
                    'bg-amber-500': sentimentPostsFilter === 'neutral',
                    'bg-rose-500': sentimentPostsFilter === 'negative'
                  }"
                ></span>
              </div>
              <div>
                <h3 class="text-lg font-semibold text-slate-800 dark:text-slate-100 capitalize">
                  {{ sentimentPostsFilter }} Posts
                </h3>
                <p class="text-sm text-slate-500 dark:text-slate-400">
                  {{ sentimentPosts.length }} posts found
                </p>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <select
                v-model="sentimentPostsSort"
                class="text-xs px-2 py-1.5 bg-slate-100 dark:bg-slate-700 border border-slate-200 dark:border-slate-600 rounded-lg text-slate-600 dark:text-slate-300 focus:ring-2 focus:ring-cyan-500 focus:border-transparent"
              >
                <option value="strongest">Strongest First</option>
                <option value="newest">Newest First</option>
                <option value="oldest">Oldest First</option>
              </select>
            </div>
            <button 
              @click="closeSentimentPosts"
              class="p-2 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
            >
              <svg class="w-5 h-5 text-slate-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- Content -->
          <div class="flex-1 overflow-y-auto p-4 scrollbar-custom">
            <div v-if="loadingSentimentPosts" class="flex justify-center py-12">
              <LoadingSpinner size="md" />
            </div>
            
            <div v-else-if="sentimentPosts.length === 0" class="text-center py-12 text-slate-400">
              <p>No posts found with this sentiment</p>
            </div>
            
            <div v-else class="space-y-3">
              <div 
                v-for="post in sortedSentimentPosts" 
                :key="post.id"
                class="p-4 bg-slate-50 dark:bg-slate-700/50 rounded-lg border border-slate-200 dark:border-slate-600"
              >
                <div class="flex items-start justify-between gap-3 mb-2">
                  <div class="flex items-center gap-2">
                    <span class="font-medium text-slate-800 dark:text-slate-200 text-sm">
                      {{ post.author || 'Anonymous' }}
                    </span>
                    <span class="text-xs px-2 py-0.5 bg-slate-200 dark:bg-slate-600 text-slate-600 dark:text-slate-300 rounded">
                      {{ post.source }}
                    </span>
                  </div>
                  <div class="flex items-center gap-1 text-xs">
                    <span 
                      class="w-2 h-2 rounded-full"
                      :class="{
                        'bg-emerald-500': post.sentimentLabel === 'positive',
                        'bg-amber-500': post.sentimentLabel === 'neutral',
                        'bg-rose-500': post.sentimentLabel === 'negative'
                      }"
                    ></span>
                    <span class="text-slate-500 dark:text-slate-400">
                      {{ (post.sentimentCompound || 0).toFixed(2) }}
                    </span>
                  </div>
                </div>
                <p class="text-sm text-slate-600 dark:text-slate-300 leading-relaxed">
                  {{ post.content }}
                </p>
                <div class="mt-2 text-xs text-slate-400 dark:text-slate-500">
                  {{ post.publishedAt ? new Date(post.publishedAt).toLocaleDateString() : 'Unknown date' }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
