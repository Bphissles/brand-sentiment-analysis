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
            posts: posts.collect { post ->
                [
                    id: post.id,
                    externalId: post.externalId,
                    source: post.source,
                    content: post.content,
                    author: post.author,
                    authorUrl: post.authorUrl,
                    postUrl: post.postUrl,
                    publishedAt: post.publishedAt?.toEpochMilli(),
                    fetchedAt: post.fetchedAt?.toEpochMilli(),
                    sentimentCompound: post.sentimentCompound,
                    sentimentPositive: post.sentimentPositive,
                    sentimentNegative: post.sentimentNegative,
                    sentimentNeutral: post.sentimentNeutral,
                    sentimentLabel: post.sentimentLabel,
                    keywords: post.keywords?.split(',')?.toList() ?: [],
                    clusterId: post.clusterId
                ]
            }
        ])
    }

    /**
     * GET /api/clusters/summary
     * Get dashboard summary of all clusters
     */
    def summary() {
        def clusters = Cluster.list()
        def posts = Post.list()

        // Calculate sentiment distribution using cluster data
        def positive = clusters.count { it.sentimentLabel == 'positive' }
        def negative = clusters.count { it.sentimentLabel == 'negative' }
        def neutral = clusters.count { it.sentimentLabel == 'neutral' }

        // Calculate average sentiment using cluster data
        def avgSentiment = clusters ? clusters.sum { it.sentiment ?: 0 } / clusters.size() : 0

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
