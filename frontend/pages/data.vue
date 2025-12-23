<script setup lang="ts">
const { user } = useAuth()
const { 
  getIngestionStatus, 
  scrapeAllSources, 
  scrapeSource, 
  loadFixtures, 
  clearAllData,
  triggerAnalysis,
  generateInsights
} = useApi()

// Check if user is admin
const isAdmin = computed(() => user.value?.role === 'admin')

const status = ref<any>(null)
const loading = ref(false)
const scraping = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

// Fetch status on mount
const fetchStatus = async () => {
  try {
    status.value = await getIngestionStatus()
  } catch (e: any) {
    console.error('Failed to fetch status:', e)
  }
}

onMounted(fetchStatus)

// Scrape all sources
const handleScrapeAll = async () => {
  if (scraping.value) return
  
  scraping.value = true
  error.value = null
  successMessage.value = null
  
  try {
    const result = await scrapeAllSources()
    successMessage.value = `Scraped ${result.totalImported} posts from all sources`
    await fetchStatus()
  } catch (e: any) {
    error.value = e.data?.error || e.message || 'Scraping failed'
  } finally {
    scraping.value = false
  }
}

// Scrape single source
const handleScrapeSource = async (source: 'twitter' | 'youtube' | 'forums') => {
  if (scraping.value) return
  
  scraping.value = true
  error.value = null
  successMessage.value = null
  
  try {
    const result = await scrapeSource(source)
    successMessage.value = `Scraped ${result.imported} posts from ${source}`
    await fetchStatus()
  } catch (e: any) {
    error.value = e.data?.error || e.message || 'Scraping failed'
  } finally {
    scraping.value = false
  }
}

// Load mock fixtures
const handleLoadFixtures = async () => {
  loading.value = true
  error.value = null
  successMessage.value = null
  
  try {
    const result = await loadFixtures()
    successMessage.value = `Loaded ${result.postsLoaded} fixture posts`
    await fetchStatus()
  } catch (e: any) {
    error.value = e.data?.error || e.message || 'Failed to load fixtures'
  } finally {
    loading.value = false
  }
}

// Clear all data
const handleClearData = async () => {
  if (!confirm('Are you sure you want to delete all posts and clusters?')) return
  
  loading.value = true
  error.value = null
  successMessage.value = null
  
  try {
    const result = await clearAllData()
    successMessage.value = `Cleared ${result.postsDeleted} posts and ${result.clustersDeleted} clusters`
    await fetchStatus()
  } catch (e: any) {
    error.value = e.data?.error || e.message || 'Failed to clear data'
  } finally {
    loading.value = false
  }
}

// Run analysis after data load
const handleRunAnalysis = async () => {
  loading.value = true
  error.value = null
  
  try {
    const result = await triggerAnalysis()
    successMessage.value = `Analysis complete: ${result.run.clustersCreated} clusters created. Generating AI insights...`
    
    // Auto-generate AI insights after analysis completes
    try {
      await generateInsights()
      successMessage.value = `Analysis complete: ${result.run.clustersCreated} clusters created with AI insights generated.`
    } catch (insightError) {
      console.error('Failed to generate insights:', insightError)
      successMessage.value = `Analysis complete: ${result.run.clustersCreated} clusters created. (AI insights generation failed)`
    }
  } catch (e: any) {
    error.value = e.data?.error || e.message || 'Analysis failed'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-slate-50 dark:bg-slate-900 transition-colors">
    <!-- Header -->
    <header class="bg-gradient-to-r from-slate-900 to-slate-800 shadow-lg">
      <div class="max-w-7xl mx-auto px-4 py-5 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center">
          <div class="flex items-center gap-4">
            <NuxtLink to="/" class="w-10 h-10 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-lg flex items-center justify-center hover:scale-105 transition-transform">
              <span class="text-white font-bold text-lg">V</span>
            </NuxtLink>
            <div>
              <h1 class="text-xl font-semibold text-white tracking-tight">Data Management</h1>
              <p class="text-sm text-slate-400">Configure data sources and ingestion</p>
            </div>
          </div>
          <div class="flex items-center gap-3">
            <ThemeToggle />
            <NuxtLink 
              to="/"
              class="px-4 py-2 bg-slate-700 text-slate-200 text-sm font-medium rounded-lg hover:bg-slate-600 transition-colors"
            >
              Back to Dashboard
            </NuxtLink>
          </div>
        </div>
      </div>
    </header>

    <main class="max-w-7xl mx-auto px-4 py-6 sm:px-6 lg:px-8">
      <!-- Alerts -->
      <div v-if="error" class="mb-6 p-4 bg-red-100 dark:bg-red-900/30 border border-red-300 dark:border-red-800 text-red-700 dark:text-red-400 rounded-lg flex justify-between items-center">
        <span>{{ error }}</span>
        <button @click="error = null" class="font-bold text-xl">&times;</button>
      </div>
      
      <div v-if="successMessage" class="mb-6 p-4 bg-emerald-100 dark:bg-emerald-900/30 border border-emerald-300 dark:border-emerald-800 text-emerald-700 dark:text-emerald-400 rounded-lg flex justify-between items-center">
        <span>{{ successMessage }}</span>
        <button @click="successMessage = null" class="font-bold text-xl">&times;</button>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Current Status -->
        <div class="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-200 dark:border-slate-700 p-6">
          <h2 class="text-lg font-semibold text-slate-800 dark:text-slate-100 mb-4">Current Status</h2>
          
          <div v-if="status" class="space-y-4">
            <div class="flex justify-between items-center py-2 border-b border-slate-100 dark:border-slate-700">
              <span class="text-slate-600 dark:text-slate-400">Total Posts</span>
              <span class="font-semibold text-slate-800 dark:text-slate-200">{{ status.totalPosts }}</span>
            </div>
            
            <div class="py-2 border-b border-slate-100 dark:border-slate-700">
              <span class="text-slate-600 dark:text-slate-400 block mb-2">Posts by Source</span>
              <div class="flex gap-3">
                <span class="px-2 py-1 bg-sky-100 dark:bg-sky-900/30 text-sky-700 dark:text-sky-400 rounded text-sm">
                  Twitter: {{ status.sourceBreakdown?.twitter || 0 }}
                </span>
                <span class="px-2 py-1 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400 rounded text-sm">
                  YouTube: {{ status.sourceBreakdown?.youtube || 0 }}
                </span>
                <span class="px-2 py-1 bg-violet-100 dark:bg-violet-900/30 text-violet-700 dark:text-violet-400 rounded text-sm">
                  Forums: {{ status.sourceBreakdown?.forums || 0 }}
                </span>
              </div>
            </div>
            
            <div class="flex justify-between items-center py-2 border-b border-slate-100 dark:border-slate-700">
              <span class="text-slate-600 dark:text-slate-400">Gemini API</span>
              <span 
                class="px-2 py-1 rounded text-sm font-medium"
                :class="status.geminiConfigured ? 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400' : 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400'"
              >
                {{ status.geminiConfigured ? 'Configured' : 'Not Configured' }}
              </span>
            </div>
            
            <div class="flex justify-between items-center py-2">
              <span class="text-slate-600 dark:text-slate-400">Scraping Status</span>
              <span 
                class="px-2 py-1 rounded text-sm font-medium"
                :class="status.status === 'running' ? 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400' : 'bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-400'"
              >
                {{ status.status === 'running' ? 'Running...' : 'Idle' }}
              </span>
            </div>
          </div>
          
          <div v-else class="text-center py-8 text-slate-400">
            Loading status...
          </div>
        </div>

        <!-- Gemini Web Scraping (Admin Only) -->
        <div v-if="isAdmin" class="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-200 dark:border-slate-700 p-6">
          <h2 class="text-lg font-semibold text-slate-800 dark:text-slate-100 mb-2">Gemini Web Scraping</h2>
          <p class="text-sm text-slate-500 dark:text-slate-400 mb-4">
            Use Google Gemini AI to scrape and extract posts from social media and forums.
          </p>
          
          <div v-if="!status?.geminiConfigured" class="p-4 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg mb-4">
            <p class="text-sm text-amber-700 dark:text-amber-400">
              <strong>Gemini API key required.</strong> Set the <code class="bg-amber-100 dark:bg-amber-900/50 px-1 rounded">GEMINI_API_KEY</code> environment variable to enable web scraping.
            </p>
          </div>
          
          <div class="space-y-3">
            <button
              @click="handleScrapeAll"
              :disabled="scraping || !status?.geminiConfigured"
              class="w-full px-4 py-3 bg-gradient-to-r from-cyan-500 to-blue-500 text-white font-medium rounded-lg hover:from-cyan-600 hover:to-blue-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 transition-all"
            >
              <svg v-if="scraping" class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span>{{ scraping ? 'Scraping...' : 'Scrape All Sources' }}</span>
            </button>
            
            <div class="grid grid-cols-3 gap-2">
              <button
                @click="handleScrapeSource('twitter')"
                :disabled="scraping || !status?.geminiConfigured"
                class="px-3 py-2 bg-sky-500 text-white text-sm font-medium rounded-lg hover:bg-sky-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Twitter/X
              </button>
              <button
                @click="handleScrapeSource('youtube')"
                :disabled="scraping || !status?.geminiConfigured"
                class="px-3 py-2 bg-red-500 text-white text-sm font-medium rounded-lg hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                YouTube
              </button>
              <button
                @click="handleScrapeSource('forums')"
                :disabled="scraping || !status?.geminiConfigured"
                class="px-3 py-2 bg-violet-500 text-white text-sm font-medium rounded-lg hover:bg-violet-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Forums
              </button>
            </div>
          </div>
        </div>

        <!-- Mock Data (Admin Only) -->
        <div v-if="isAdmin" class="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-200 dark:border-slate-700 p-6">
          <h2 class="text-lg font-semibold text-slate-800 dark:text-slate-100 mb-2">Mock Data</h2>
          <p class="text-sm text-slate-500 dark:text-slate-400 mb-4">
            Load pre-defined fixture data for testing and demonstration purposes.
          </p>
          
          <button
            @click="handleLoadFixtures"
            :disabled="loading"
            class="w-full px-4 py-3 bg-slate-600 text-white font-medium rounded-lg hover:bg-slate-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 transition-colors"
          >
            <svg v-if="loading" class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <span>Load Fixture Data (50 posts)</span>
          </button>
        </div>

        <!-- Data Actions -->
        <div class="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-200 dark:border-slate-700 p-6">
          <h2 class="text-lg font-semibold text-slate-800 dark:text-slate-100 mb-2">Data Actions</h2>
          <p class="text-sm text-slate-500 dark:text-slate-400 mb-4">
            Run analysis on loaded data or clear all data to start fresh.
          </p>
          
          <div class="space-y-3">
            <button
              @click="handleRunAnalysis"
              :disabled="loading || !status?.totalPosts"
              class="w-full px-4 py-3 bg-emerald-500 text-white font-medium rounded-lg hover:bg-emerald-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 transition-colors"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
              <span>Run ML Analysis</span>
            </button>
            
            <!-- Clear Data (Admin Only) -->
            <button
              v-if="isAdmin"
              @click="handleClearData"
              :disabled="loading"
              class="w-full px-4 py-3 bg-rose-500 text-white font-medium rounded-lg hover:bg-rose-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 transition-colors"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              <span>Clear All Data</span>
            </button>
            
            <!-- Viewer notice -->
            <p v-if="!isAdmin" class="text-sm text-slate-500 dark:text-slate-400 text-center py-2">
              <span class="inline-flex items-center gap-1">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
                Admin access required for destructive actions
              </span>
            </p>
          </div>
        </div>
      </div>

      <!-- Instructions -->
      <div class="mt-6 bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-200 dark:border-slate-700 p-6">
        <h2 class="text-lg font-semibold text-slate-800 dark:text-slate-100 mb-4">Setup Instructions</h2>
        
        <div class="prose prose-slate dark:prose-invert max-w-none text-sm">
          <ol class="space-y-3 text-slate-600 dark:text-slate-400">
            <li>
              <strong class="text-slate-800 dark:text-slate-200">Get a Gemini API Key:</strong>
              Visit <a href="https://makersuite.google.com/app/apikey" target="_blank" class="text-cyan-600 dark:text-cyan-400 hover:underline">Google AI Studio</a> to create a free API key.
            </li>
            <li>
              <strong class="text-slate-800 dark:text-slate-200">Set Environment Variable:</strong>
              Add <code class="bg-slate-100 dark:bg-slate-700 px-1 rounded">GEMINI_API_KEY=your_key_here</code> to your <code class="bg-slate-100 dark:bg-slate-700 px-1 rounded">.env</code> file.
            </li>
            <li>
              <strong class="text-slate-800 dark:text-slate-200">Restart Backend:</strong>
              Restart the Grails backend to load the new environment variable.
            </li>
            <li>
              <strong class="text-slate-800 dark:text-slate-200">Scrape Data:</strong>
              Click "Scrape All Sources" to fetch real posts from Twitter, YouTube, and forums.
            </li>
            <li>
              <strong class="text-slate-800 dark:text-slate-200">Run Analysis:</strong>
              After scraping, click "Run ML Analysis" to cluster posts and generate insights.
            </li>
          </ol>
        </div>
      </div>
    </main>
  </div>
</template>
