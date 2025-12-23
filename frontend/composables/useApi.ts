/**
 * Composable for API calls to the Grails backend
 */
import type { Cluster, Post, DashboardSummary } from '~/types/models'

export const useApi = () => {
  const config = useRuntimeConfig()
  const baseUrl = config.public.apiUrl || 'http://localhost:8080'

  /**
   * Fetch dashboard summary
   */
  const fetchSummary = async (): Promise<DashboardSummary> => {
    const response = await $fetch<any>(`${baseUrl}/api/clusters/summary`)
    return response
  }

  /**
   * Fetch all clusters
   */
  const fetchClusters = async (): Promise<Cluster[]> => {
    const response = await $fetch<any>(`${baseUrl}/api/clusters`)
    return response.clusters || []
  }

  /**
   * Fetch a single cluster by ID
   */
  const fetchCluster = async (id: string | number): Promise<{ cluster: Cluster; posts: Post[] }> => {
    const response = await $fetch<any>(`${baseUrl}/api/clusters/${id}`)
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
    const response = await $fetch<any>(url)
    return response.data || []
  }

  /**
   * Trigger ML analysis
   */
  const triggerAnalysis = async (): Promise<any> => {
    const response = await $fetch<any>(`${baseUrl}/api/analysis/trigger`, {
      method: 'POST'
    })
    return response
  }

  /**
   * Load fixture data
   */
  const loadFixtures = async (): Promise<any> => {
    const response = await $fetch<any>(`${baseUrl}/api/analysis/load-fixtures`, {
      method: 'POST'
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

  return {
    fetchSummary,
    fetchClusters,
    fetchCluster,
    fetchPosts,
    triggerAnalysis,
    loadFixtures,
    checkHealth
  }
}
