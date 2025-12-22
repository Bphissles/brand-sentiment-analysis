package sentiment

import grails.converters.JSON
import grails.gorm.transactions.Transactional

/**
 * REST controller for Cluster operations
 * Handles listing and retrieval of topic clusters
 */
class ClusterController {

    static responseFormats = ['json']
    static allowedMethods = [index: 'GET', show: 'GET']

    /**
     * GET /api/clusters
     * List all clusters with optional filtering
     */
    def index() {
        def analysisRunId = params.analysisRunId
        def sentimentLabel = params.sentimentLabel

        def criteria = Cluster.createCriteria()
        def clusters = criteria.list {
            if (analysisRunId) {
                eq('analysisRunId', analysisRunId)
            }
            if (sentimentLabel) {
                eq('sentimentLabel', sentimentLabel)
            }
            order('postCount', 'desc')
        }

        // Enrich with post sample for each cluster
        def enrichedClusters = clusters.collect { cluster ->
            def samplePosts = Post.findAllByClusterId(cluster.id.toString(), [max: 3])
            [
                id: cluster.id,
                taxonomyId: cluster.taxonomyId,
                label: cluster.label,
                description: cluster.description,
                keywords: cluster.keywords?.split(',')?.toList() ?: [],
                sentiment: cluster.sentiment,
                sentimentLabel: cluster.sentimentLabel,
                postCount: cluster.postCount,
                insight: cluster.insight,
                samplePosts: samplePosts.collect { p ->
                    [id: p.id, content: p.content?.take(200), author: p.author]
                }
            ]
        }

        respond([clusters: enrichedClusters, total: clusters.size()])
    }

    /**
     * GET /api/clusters/{id}
     * Get a single cluster with its posts
     */
    def show(Long id) {
        def cluster = Cluster.get(id)
        if (!cluster) {
            render status: 404, text: [error: 'Cluster not found'] as JSON
            return
        }

        // Get all posts in this cluster
        def posts = Post.findAllByClusterId(cluster.id.toString())

        respond([
            cluster: [
                id: cluster.id,
                taxonomyId: cluster.taxonomyId,
                label: cluster.label,
                description: cluster.description,
                keywords: cluster.keywords?.split(',')?.toList() ?: [],
                sentiment: cluster.sentiment,
                sentimentLabel: cluster.sentimentLabel,
                postCount: cluster.postCount,
                insight: cluster.insight
            ],
            posts: posts
        ])
    }

    /**
     * GET /api/clusters/summary
     * Get dashboard summary of all clusters
     */
    def summary() {
        def clusters = Cluster.list()
        def posts = Post.list()

        // Calculate sentiment distribution
        def positive = posts.count { it.sentimentLabel == 'positive' }
        def negative = posts.count { it.sentimentLabel == 'negative' }
        def neutral = posts.count { it.sentimentLabel == 'neutral' }

        // Calculate average sentiment
        def avgSentiment = posts ? posts.sum { it.sentimentCompound ?: 0 } / posts.size() : 0

        respond([
            totalPosts: posts.size(),
            totalClusters: clusters.size(),
            averageSentiment: avgSentiment?.round(3),
            sentimentDistribution: [
                positive: positive,
                neutral: neutral,
                negative: negative
            ],
            topClusters: clusters.take(4).collect { c ->
                [
                    id: c.id,
                    label: c.label,
                    sentiment: c.sentiment,
                    sentimentLabel: c.sentimentLabel,
                    postCount: c.postCount
                ]
            }
        ])
    }
}
