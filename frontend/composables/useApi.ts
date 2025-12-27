/**
 * Composable for API calls to the Grails backend
 */
import type {
  Cluster,
  Post,
  DashboardSummary,
  ClustersResponse,
  ClusterDetailResponse,
  PostsResponse,
  AnalysisTriggerResponse,
  LoadFixturesResponse,
  HealthResponse,
  IngestionStatusResponse,
  ScrapeResponse,
  ClearDataResponse,
  InsightsResponse
} from '~/types/models'

export const useApi = () => {
  const config = useRuntimeConfig()
  const baseUrl = config.public.apiUrl || 'http://localhost:8080'
  const { getAuthHeader } = useAuth()

  /**
   * Fetch dashboard summary with optional source filter
   */
  const fetchSummary = async (source?: string): Promise<DashboardSummary> => {
    const query = source && source !== 'all' ? `?source=${source}` : ''
    const response = await $fetch<DashboardSummary>(`${baseUrl}/api/clusters/summary${query}`, {
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Fetch all clusters with optional source filter
   */
  const fetchClusters = async (source?: string): Promise<Cluster[]> => {
    const query = source && source !== 'all' ? `?source=${source}` : ''
    const response = await $fetch<ClustersResponse>(`${baseUrl}/api/clusters${query}`, {
      headers: getAuthHeader()
    })
    return response.clusters || []
  }

  /**
   * Fetch a single cluster by ID
   */
  const fetchCluster = async (id: string | number): Promise<ClusterDetailResponse> => {
    const response = await $fetch<ClusterDetailResponse>(`${baseUrl}/api/clusters/${id}`, {
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Fetch all posts with optional filters
   */
  const fetchPosts = async (params?: { source?: string; clusterId?: string; sentiment?: string }): Promise<Post[]> => {
    const query = new URLSearchParams()
    if (params?.source) query.set('source', params.source)
    if (params?.clusterId) query.set('clusterId', params.clusterId)
    if (params?.sentiment) query.set('sentimentLabel', params.sentiment)
    
    const url = query.toString() ? `${baseUrl}/api/posts?${query}` : `${baseUrl}/api/posts`
    const response = await $fetch<PostsResponse>(url, {
      headers: getAuthHeader()
    })
    return response.data || []
  }

  /**
   * Trigger ML analysis
   */
  const triggerAnalysis = async (): Promise<AnalysisTriggerResponse> => {
    const response = await $fetch<AnalysisTriggerResponse>(`${baseUrl}/api/analysis/trigger`, {
      method: 'POST',
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Load fixture data (admin only)
   */
  const loadFixtures = async (): Promise<LoadFixturesResponse> => {
    const response = await $fetch<LoadFixturesResponse>(`${baseUrl}/api/analysis/load-fixtures`, {
      method: 'POST',
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Check API health
   */
  const checkHealth = async (): Promise<HealthResponse> => {
    const response = await $fetch<HealthResponse>(`${baseUrl}/api/health`)
    return response
  }

  /**
   * Get data ingestion status
   */
  const getIngestionStatus = async (): Promise<IngestionStatusResponse> => {
    const response = await $fetch<IngestionStatusResponse>(`${baseUrl}/api/ingestion/status`, {
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Scrape all sources using Gemini (admin only)
   */
  const scrapeAllSources = async (): Promise<ScrapeResponse> => {
    const response = await $fetch<ScrapeResponse>(`${baseUrl}/api/ingestion/scrapeAll`, {
      method: 'POST',
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Scrape a specific source (admin only)
   */
  const scrapeSource = async (source: 'twitter' | 'youtube' | 'reddit' | 'forums' | 'news'): Promise<ScrapeResponse> => {
    const response = await $fetch<ScrapeResponse>(`${baseUrl}/api/ingestion/scrape/${source}`, {
      method: 'POST',
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Clear all data (posts and clusters) - admin only
   */
  const clearAllData = async (): Promise<ClearDataResponse> => {
    const response = await $fetch<ClearDataResponse>(`${baseUrl}/api/analysis/clear`, {
      method: 'DELETE',
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Fetch AI insights (cached)
   */
  const fetchInsights = async (source?: string): Promise<InsightsResponse> => {
    const query = source && source !== 'all' ? `?source=${source}` : ''
    const response = await $fetch<InsightsResponse>(`${baseUrl}/api/insights${query}`, {
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Generate new AI insights
   */
  const generateInsights = async (source?: string): Promise<InsightsResponse> => {
    const query = source && source !== 'all' ? `?source=${source}` : ''
    const response = await $fetch<InsightsResponse>(`${baseUrl}/api/insights/generate${query}`, {
      method: 'POST',
      headers: getAuthHeader()
    })
    return response
  }

  return {
    fetchSummary,
    fetchClusters,
    fetchCluster,
    fetchPosts,
    triggerAnalysis,
    loadFixtures,
    checkHealth,
    getIngestionStatus,
    scrapeAllSources,
    scrapeSource,
    clearAllData,
    fetchInsights,
    generateInsights
  }
}
