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
  <div class="min-h-screen bg-gray-100">
    <!-- Header -->
    <header class="bg-white shadow">
      <div class="max-w-7xl mx-auto px-4 py-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center">
          <div>
            <h1 class="text-2xl font-bold text-gray-900">
              Voice of the Operator
            </h1>
            <p class="text-sm text-gray-500">Peterbilt Sentiment Dashboard</p>
          </div>
          <button
            @click="runAnalysis"
            :disabled="loading"
            class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            <span v-if="loading" class="animate-spin">⟳</span>
            <span>{{ loading ? 'Analyzing...' : 'Run Analysis' }}</span>
          </button>
        </div>
      </div>
    </header>

    <main class="max-w-7xl mx-auto px-4 py-6 sm:px-6 lg:px-8">
      <!-- Error Alert -->
      <div v-if="error" class="mb-6 p-4 bg-red-100 border border-red-300 text-red-700 rounded-lg">
        {{ error }}
        <button @click="error = null" class="ml-2 font-bold">×</button>
      </div>

      <!-- Loading State -->
      <div v-if="loading && !clusters.length" class="flex justify-center items-center h-64">
        <div class="text-center">
          <div class="animate-spin text-4xl mb-2">⟳</div>
          <p class="text-gray-500">Loading dashboard...</p>
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

        <!-- Main Visualization -->
        <div class="bg-white rounded-lg shadow p-6 mb-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">Topic Clusters</h2>
          <p class="text-sm text-gray-500 mb-4">
            Click on a bubble to see posts in that cluster. Size = post count, Color = sentiment.
          </p>
          
          <div v-if="clusters.length" class="flex justify-center">
            <BubbleChart 
              :clusters="clusters" 
              :width="800" 
              :height="500"
              @cluster-click="handleClusterClick"
            />
          </div>
          <div v-else class="text-center py-12 text-gray-500">
            No clusters found. Click "Run Analysis" to analyze posts.
          </div>
        </div>

        <!-- Cluster Cards Grid -->
        <div class="mb-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">Cluster Details</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <ClusterCard 
              v-for="cluster in clusters" 
              :key="cluster.id"
              :cluster="cluster"
              @click="handleClusterClick"
            />
          </div>
        </div>

        <!-- Legend -->
        <div class="bg-white rounded-lg shadow p-4">
          <h3 class="text-sm font-semibold text-gray-700 mb-2">Sentiment Legend</h3>
          <div class="flex gap-6">
            <div class="flex items-center gap-2">
              <div class="w-4 h-4 rounded-full bg-green-500"></div>
              <span class="text-sm text-gray-600">Positive (≥ 0.3)</span>
            </div>
            <div class="flex items-center gap-2">
              <div class="w-4 h-4 rounded-full bg-yellow-500"></div>
              <span class="text-sm text-gray-600">Neutral (-0.3 to 0.3)</span>
            </div>
            <div class="flex items-center gap-2">
              <div class="w-4 h-4 rounded-full bg-red-500"></div>
              <span class="text-sm text-gray-600">Negative (≤ -0.3)</span>
            </div>
          </div>
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
