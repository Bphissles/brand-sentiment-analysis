package sentiment

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * REST controller for Cluster operations
 * Handles listing and retrieval of topic clusters
 */
@Tag(name = "Clusters", description = "Topic cluster management")
class ClusterController {

    static responseFormats = ['json']
    static allowedMethods = [index: 'GET', show: 'GET', summary: 'GET']

    /**
     * GET /api/clusters
     * List all clusters with optional filtering
     */
    @Operation(
        summary = "List clusters",
        description = "List all clusters with optional filtering by analysis run, sentiment, or source"
    )
    @ApiResponse(responseCode = "200", description = "List of clusters with sample posts")
    def index() {
        def analysisRunId = params.analysisRunId
        def sentimentLabel = params.sentimentLabel
        def source = params.source

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

        // If source filter is specified, filter clusters by posts from that source
        if (source) {
            def clusterIdsWithSource = Post.findAllBySource(source)*.clusterId.unique()
            clusters = clusters.findAll { clusterIdsWithSource.contains(it.id.toString()) }
        }

        // Enrich with post sample for each cluster
        def enrichedClusters = clusters.collect { cluster ->
            def postCriteria = [max: 3]
            def samplePosts = source 
                ? Post.findAllByClusterIdAndSource(cluster.id.toString(), source, postCriteria)
                : Post.findAllByClusterId(cluster.id.toString(), postCriteria)
            
            // Recalculate post count for this source if filtered
            def postCount = source 
                ? Post.countByClusterIdAndSource(cluster.id.toString(), source)
                : cluster.postCount
            
            [
                id: cluster.id,
                taxonomyId: cluster.taxonomyId,
                label: cluster.label,
                description: cluster.description,
                keywords: cluster.keywords?.split(',')?.toList() ?: [],
                sentiment: cluster.sentiment,
                sentimentLabel: cluster.sentimentLabel,
                postCount: postCount,
                insight: cluster.insight,
                samplePosts: samplePosts.collect { p ->
                    [id: p.id, content: p.content?.take(200), author: p.author]
                }
            ]
        }.findAll { it.postCount > 0 }  // Only include clusters with posts for this source

        respond([clusters: enrichedClusters, total: enrichedClusters.size()])
    }

    /**
     * GET /api/clusters/{id}
     * Get a single cluster with its posts
     */
    @Operation(summary = "Get cluster", description = "Get a single cluster with all its posts")
    @ApiResponse(responseCode = "200", description = "Cluster details with posts")
    @ApiResponse(responseCode = "404", description = "Cluster not found")
    def show(@Parameter(description = "Cluster ID") Long id) {
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
    @Operation(
        summary = "Dashboard summary",
        description = "Get dashboard summary with sentiment distribution and top clusters"
    )
    @ApiResponse(responseCode = "200", description = "Dashboard summary data")
    def summary() {
        def source = params.source
        
        def clusters = Cluster.list()
        def posts = source ? Post.findAllBySource(source) : Post.list()

        // If source filter, only include clusters that have posts from that source
        if (source) {
            def clusterIdsWithSource = posts*.clusterId.unique()
            clusters = clusters.findAll { clusterIdsWithSource.contains(it.id.toString()) }
        }

        // Calculate sentiment distribution from posts (more accurate when filtered)
        def positive = posts.count { it.sentimentLabel == 'positive' }
        def negative = posts.count { it.sentimentLabel == 'negative' }
        def neutral = posts.count { it.sentimentLabel == 'neutral' }

        // Calculate average sentiment from posts
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
                def clusterPostCount = source 
                    ? Post.countByClusterIdAndSource(c.id.toString(), source)
                    : c.postCount
                [
                    id: c.id,
                    label: c.label,
                    sentiment: c.sentiment,
                    sentimentLabel: c.sentimentLabel,
                    postCount: clusterPostCount
                ]
            }.findAll { it.postCount > 0 }
        ])
    }
}
