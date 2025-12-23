/**
 * Composable for API calls to the Grails backend
 */
import type { Cluster, Post, DashboardSummary } from '~/types/models'

export const useApi = () => {
  const config = useRuntimeConfig()
  const baseUrl = config.public.apiUrl || 'http://localhost:8080'
  const { getAuthHeader } = useAuth()

  /**
   * Fetch dashboard summary
   */
  const fetchSummary = async (): Promise<DashboardSummary> => {
    const response = await $fetch<any>(`${baseUrl}/api/clusters/summary`, {
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Fetch all clusters
   */
  const fetchClusters = async (): Promise<Cluster[]> => {
    const response = await $fetch<any>(`${baseUrl}/api/clusters`, {
      headers: getAuthHeader()
    })
    return response.clusters || []
  }

  /**
   * Fetch a single cluster by ID
   */
  const fetchCluster = async (id: string | number): Promise<{ cluster: Cluster; posts: Post[] }> => {
    const response = await $fetch<any>(`${baseUrl}/api/clusters/${id}`, {
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Fetch all posts with optional filters
   */
  const fetchPosts = async (params?: { source?: string; clusterId?: string }): Promise<Post[]> => {
    const query = new URLSearchParams()
    if (params?.source) query.set('source', params.source)
    if (params?.clusterId) query.set('clusterId', params.clusterId)
    
    const url = query.toString() ? `${baseUrl}/api/posts?${query}` : `${baseUrl}/api/posts`
    const response = await $fetch<any>(url, {
      headers: getAuthHeader()
    })
    return response.data || []
  }

  /**
   * Trigger ML analysis
   */
  const triggerAnalysis = async (): Promise<any> => {
    const response = await $fetch<any>(`${baseUrl}/api/analysis/trigger`, {
      method: 'POST',
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Load fixture data (admin only)
   */
  const loadFixtures = async (): Promise<any> => {
    const response = await $fetch<any>(`${baseUrl}/api/analysis/load-fixtures`, {
      method: 'POST',
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Check API health
   */
  const checkHealth = async (): Promise<any> => {
    const response = await $fetch<any>(`${baseUrl}/api/health`)
    return response
  }

  /**
   * Get data ingestion status
   */
  const getIngestionStatus = async (): Promise<any> => {
    const response = await $fetch<any>(`${baseUrl}/api/ingestion/status`, {
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Scrape all sources using Gemini (admin only)
   */
  const scrapeAllSources = async (): Promise<any> => {
    const response = await $fetch<any>(`${baseUrl}/api/ingestion/scrapeAll`, {
      method: 'POST',
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Scrape a specific source (admin only)
   */
  const scrapeSource = async (source: 'twitter' | 'youtube' | 'forums'): Promise<any> => {
    const response = await $fetch<any>(`${baseUrl}/api/ingestion/scrape/${source}`, {
      method: 'POST',
      headers: getAuthHeader()
    })
    return response
  }

  /**
   * Clear all data (posts and clusters) - admin only
   */
  const clearAllData = async (): Promise<any> => {
    const response = await $fetch<any>(`${baseUrl}/api/analysis/clear`, {
      method: 'DELETE',
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
    clearAllData
  }
}
