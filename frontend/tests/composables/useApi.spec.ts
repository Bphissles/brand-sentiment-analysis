import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useApi } from '~/composables/useApi'

// Mock useAuth
vi.mock('~/composables/useAuth', () => ({
  useAuth: () => ({
    getAuthHeader: vi.fn(() => ({ Authorization: 'Bearer mock-token' }))
  })
}))

describe('useApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    global.$fetch = vi.fn()
  })

  it('should fetch dashboard summary', async () => {
    const mockSummary = {
      totalPosts: 50,
      totalClusters: 4,
      averageSentiment: 0.25,
      sentimentDistribution: {
        positive: 2,
        neutral: 1,
        negative: 1
      },
      topClusters: []
    }
    
    global.$fetch = vi.fn().mockResolvedValue(mockSummary)
    
    const { fetchSummary } = useApi()
    const result = await fetchSummary()
    
    expect(result).toEqual(mockSummary)
    expect($fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/clusters/summary',
      expect.objectContaining({
        headers: { Authorization: 'Bearer mock-token' }
      })
    )
  })

  it('should fetch all clusters', async () => {
    const mockClusters = [
      { id: '1', label: 'EV Adoption', postCount: 10 },
      { id: '2', label: 'Driver Comfort', postCount: 15 }
    ]
    
    global.$fetch = vi.fn().mockResolvedValue({ clusters: mockClusters })
    
    const { fetchClusters } = useApi()
    const result = await fetchClusters()
    
    expect(result).toEqual(mockClusters)
    expect($fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/clusters',
      expect.objectContaining({
        headers: { Authorization: 'Bearer mock-token' }
      })
    )
  })

  it('should fetch single cluster with posts', async () => {
    const mockResponse = {
      cluster: { id: '1', label: 'EV Adoption' },
      posts: [{ id: '1', content: 'Test post' }]
    }
    
    global.$fetch = vi.fn().mockResolvedValue(mockResponse)
    
    const { fetchCluster } = useApi()
    const result = await fetchCluster('1')
    
    expect(result).toEqual(mockResponse)
    expect($fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/clusters/1',
      expect.objectContaining({
        headers: { Authorization: 'Bearer mock-token' }
      })
    )
  })

  it('should fetch posts with filters', async () => {
    const mockPosts = [
      { id: '1', source: 'twitter', content: 'Post 1' },
      { id: '2', source: 'twitter', content: 'Post 2' }
    ]
    
    global.$fetch = vi.fn().mockResolvedValue({ data: mockPosts })
    
    const { fetchPosts } = useApi()
    const result = await fetchPosts({ source: 'twitter' })
    
    expect(result).toEqual(mockPosts)
    expect($fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/posts?source=twitter',
      expect.objectContaining({
        headers: { Authorization: 'Bearer mock-token' }
      })
    )
  })

  it('should trigger ML analysis', async () => {
    const mockResponse = {
      run: { id: '1', status: 'completed' },
      clusters: 4,
      postsAnalyzed: 50
    }
    
    global.$fetch = vi.fn().mockResolvedValue(mockResponse)
    
    const { triggerAnalysis } = useApi()
    const result = await triggerAnalysis()
    
    expect(result).toEqual(mockResponse)
    expect($fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/analysis/trigger',
      expect.objectContaining({
        method: 'POST',
        headers: { Authorization: 'Bearer mock-token' }
      })
    )
  })

  it('should load fixtures', async () => {
    const mockResponse = {
      success: true,
      postsLoaded: 50,
      sources: { twitter: 20, youtube: 15, forums: 15 }
    }
    
    global.$fetch = vi.fn().mockResolvedValue(mockResponse)
    
    const { loadFixtures } = useApi()
    const result = await loadFixtures()
    
    expect(result).toEqual(mockResponse)
    expect($fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/analysis/load-fixtures',
      expect.objectContaining({
        method: 'POST',
        headers: { Authorization: 'Bearer mock-token' }
      })
    )
  })

  it('should check API health', async () => {
    const mockHealth = {
      status: 'healthy',
      service: 'grails-backend',
      mlEngineStatus: 'healthy'
    }
    
    global.$fetch = vi.fn().mockResolvedValue(mockHealth)
    
    const { checkHealth } = useApi()
    const result = await checkHealth()
    
    expect(result).toEqual(mockHealth)
    expect($fetch).toHaveBeenCalledWith('http://localhost:8080/api/health')
  })

  it('should clear all data', async () => {
    const mockResponse = {
      success: true,
      postsDeleted: 50,
      clustersDeleted: 4
    }
    
    global.$fetch = vi.fn().mockResolvedValue(mockResponse)
    
    const { clearAllData } = useApi()
    const result = await clearAllData()
    
    expect(result).toEqual(mockResponse)
    expect($fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/analysis/clear',
      expect.objectContaining({
        method: 'DELETE',
        headers: { Authorization: 'Bearer mock-token' }
      })
    )
  })
})
