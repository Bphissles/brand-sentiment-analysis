<script setup lang="ts">
import type { Cluster, Post } from '~/types/models'

const api = useApi()

// State
const clusters = ref<Cluster[]>([])
const summary = ref<any>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const selectedCluster = ref<Cluster | null>(null)
const selectedClusterPosts = ref<Post[]>([])
const showDetail = ref(false)
const selectedSource = ref<string>('all')
const lastAnalysis = ref<string | null>(null)

// Load data on mount
onMounted(async () => {
  await loadData()
})

const loadData = async () => {
  loading.value = true
  error.value = null
  
  try {
    const [clustersData, summaryData] = await Promise.all([
      api.fetchClusters(),
      api.fetchSummary()
    ])
    
    clusters.value = clustersData
    summary.value = summaryData
  } catch (e: any) {
    error.value = e.message || 'Failed to load data'
    console.error('Failed to load data:', e)
  } finally {
    loading.value = false
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

const runAnalysis = async () => {
  loading.value = true
  try {
    await api.triggerAnalysis()
    await loadData()
  } catch (e: any) {
    error.value = e.message || 'Analysis failed'
  } finally {
    loading.value = false
  }
}

// Computed
const sentimentTrend = computed(() => {
  if (!summary.value) return 'neutral'
  const avg = summary.value.averageSentiment || 0
  if (avg >= 0.1) return 'up'
  if (avg <= -0.1) return 'down'
  return 'neutral'
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
              <h1 class="text-xl font-semibold text-white tracking-tight">
                Voice of the Operator
              </h1>
              <p class="text-sm text-slate-400">Real-time Sentiment Intelligence</p>
            </div>
          </div>
          <div class="flex items-center gap-3">
            <select 
              v-model="selectedSource"
              class="bg-slate-700 text-slate-200 text-sm rounded-lg px-3 py-2 border border-slate-600 focus:ring-2 focus:ring-cyan-500 focus:border-transparent"
            >
              <option value="all">All Sources</option>
              <option value="twitter">Twitter/X</option>
              <option value="youtube">YouTube</option>
              <option value="forums">Forums</option>
            </select>
            <NuxtLink 
              to="/data"
              class="px-3 py-2 bg-slate-700 text-slate-200 text-sm font-medium rounded-lg hover:bg-slate-600 border border-slate-600 transition-colors"
              title="Data Management"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
              </svg>
            </NuxtLink>
            <ThemeToggle />
            <button
              @click="runAnalysis"
              :disabled="loading"
              class="px-5 py-2.5 bg-gradient-to-r from-cyan-500 to-blue-500 text-white text-sm font-medium rounded-lg hover:from-cyan-600 hover:to-blue-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 transition-all shadow-md hover:shadow-lg"
            >
              <svg v-if="loading" class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span>{{ loading ? 'Analyzing...' : 'Run Analysis' }}</span>
            </button>
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
          <svg class="animate-spin h-10 w-10 text-cyan-500 mx-auto mb-3" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p class="text-slate-500 dark:text-slate-400 font-medium">Loading dashboard...</p>
        </div>
      </div>

      <!-- Dashboard Content -->
      <template v-else>
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
            trend="up"
          />
        </div>

        <!-- Main Content: 75/25 Split -->
        <div class="flex gap-6 mb-6">
          <!-- Left: Visualization (75%) -->
          <div class="w-3/4 bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-200 dark:border-slate-700 p-6">
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
                :width="600" 
                :height="450"
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

          <!-- Right: Cluster List (25%) -->
          <div class="w-1/4 bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-200 dark:border-slate-700 p-4 flex flex-col">
            <h2 class="text-lg font-semibold text-slate-800 dark:text-slate-100 mb-3">Cluster Breakdown</h2>
            <div class="flex-1 overflow-y-auto space-y-3 pr-1" style="max-height: 480px;">
              <ClusterCard 
                v-for="cluster in clusters" 
                :key="cluster.id"
                :cluster="cluster"
                @click="handleClusterClick"
              />
            </div>
          </div>
        </div>

        <!-- Footer Info -->
        <div class="flex justify-between items-center text-xs text-slate-400 dark:text-slate-500 pt-4 border-t border-slate-200 dark:border-slate-700">
          <span>Data sources: Twitter/X, YouTube, Forums</span>
          <span>Powered by K-Means clustering + VADER sentiment analysis</span>
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
  </div>
</template>
