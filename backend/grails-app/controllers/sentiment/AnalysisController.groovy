package sentiment

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import java.time.Instant

/**
 * REST controller for Analysis operations
 * Handles triggering ML analysis and managing analysis runs
 */
class AnalysisController {

    static responseFormats = ['json']
    static allowedMethods = [index: 'GET', show: 'GET', trigger: 'POST', loadFixtures: 'POST']

    MlEngineService mlEngineService
    DataLoaderService dataLoaderService

    /**
     * GET /api/analysis
     * List all analysis runs
     */
    def index() {
        def runs = AnalysisRun.list(sort: 'dateCreated', order: 'desc', max: 20)
        respond([runs: runs])
    }

    /**
     * GET /api/analysis/{id}
     * Get a single analysis run
     */
    def show(Long id) {
        def run = AnalysisRun.get(id)
        if (!run) {
            render status: 404, text: [error: 'Analysis run not found'] as JSON
            return
        }
        respond run
    }

    /**
     * POST /api/analysis/trigger
     * Trigger a new ML analysis on all posts
     */
    @Transactional
    def trigger() {
        // Create analysis run record
        def run = new AnalysisRun(
            status: 'processing',
            startedAt: Instant.now()
        )
        run.save(flush: true)

        try {
            // Get all posts
            def posts = Post.list()
            
            if (posts.isEmpty()) {
                run.status = 'completed'
                run.postsAnalyzed = 0
                run.clustersCreated = 0
                run.completedAt = Instant.now()
                run.durationMs = 0
                run.save(flush: true)
                
                respond([
                    run: run,
                    message: 'No posts to analyze'
                ])
                return
            }

            // Call ML Engine first before clearing existing data
            def result = mlEngineService.analyzePostsForClusters(posts)

            if (!result.success) {
                run.status = 'failed'
                run.error = result.error
                run.completedAt = Instant.now()
                run.save(flush: true)
                
                render status: 500, text: [error: result.error, run: run] as JSON
                return
            }

            // Only clear previous clusters after successful ML analysis
            Cluster.executeUpdate('delete from Cluster')
            Post.executeUpdate('update Post set clusterId = null')

            // Save clusters from ML response
            def clustersCreated = 0
            result.clusters.each { clusterData ->
                def cluster = new Cluster(
                    taxonomyId: clusterData.taxonomyId,
                    label: clusterData.label,
                    description: clusterData.description,
                    keywords: clusterData.keywords?.join(','),
                    sentiment: clusterData.sentiment,
                    sentimentLabel: clusterData.sentimentLabel,
                    postCount: clusterData.postCount,
                    analysisRunId: run.id.toString()
                )
                cluster.save(flush: true)
                clustersCreated++

                // Update posts with cluster assignment
                clusterData.postIds?.each { postId ->
                    def post = Post.get(postId as Long)
                    if (post) {
                        post.clusterId = cluster.id.toString()
                        post.save()
                    }
                }
            }

            // Update posts with sentiment from ML response
            result.posts?.each { postData ->
                log.info("Processing post sentiment for ID: ${postData.id}")
                // Try both string and Long ID conversion
                def post = null
                try {
                    post = Post.get(postData.id as Long)
                } catch (Exception e) {
                    log.warn("Could not convert ${postData.id} to Long, trying externalId match")
                    post = Post.findByExternalId(postData.id.toString())
                }
                
                if (post) {
                    log.info("Found post ${postData.id} in database")
                    if (postData.sentiment) {
                        log.info("Updating sentiment for post ${postData.id}: ${postData.sentiment}")
                        post.sentimentCompound = postData.sentiment.compound
                        post.sentimentPositive = postData.sentiment.positive
                        post.sentimentNegative = postData.sentiment.negative
                        post.sentimentNeutral = postData.sentiment.neutral
                        
                        // Generate sentiment label from compound score
                        def compound = postData.sentiment.compound as Double
                        if (compound >= 0.05) {
                            post.sentimentLabel = 'positive'
                        } else if (compound <= -0.05) {
                            post.sentimentLabel = 'negative'
                        } else {
                            post.sentimentLabel = 'neutral'
                        }
                        
                        post.keywords = postData.keywords?.take(10)?.join(',')
                        post.save()
                        log.info("Saved sentiment for post ${postData.id}")
                    } else {
                        log.warn("No sentiment data for post ${postData.id}")
                    }
                } else {
                    log.warn("Post not found in database: ${postData.id}")
                }
            }

            // Update run record
            run.status = 'completed'
            run.postsAnalyzed = result.postsAnalyzed
            run.clustersCreated = clustersCreated
            run.completedAt = Instant.now()
            run.durationMs = result.processingTimeMs
            run.save(flush: true)

            respond([
                run: run,
                clusters: result.clusters?.size() ?: 0,
                postsAnalyzed: result.postsAnalyzed
            ])

        } catch (Exception e) {
            log.error("Analysis failed", e)
            run.status = 'failed'
            run.error = e.message
            run.completedAt = Instant.now()
            run.save(flush: true)
            
            render status: 500, text: [error: e.message, run: run] as JSON
        }
    }

    /**
     * POST /api/analysis/load-fixtures
     * Load mock data from fixtures into database
     */
    @Transactional
    def loadFixtures() {
        try {
            def result = dataLoaderService.loadAllFixtures()
            respond([
                success: true,
                postsLoaded: result.postsLoaded,
                sources: result.sources
            ])
        } catch (Exception e) {
            log.error("Failed to load fixtures", e)
            render status: 500, text: [error: e.message] as JSON
        }
    }

    /**
     * DELETE /api/analysis/clear
     * Clear all posts and clusters (for testing)
     */
    @Transactional
    def clear() {
        def postCount = Post.count()
        def clusterCount = Cluster.count()
        
        Post.executeUpdate('delete from Post')
        Cluster.executeUpdate('delete from Cluster')
        
        respond([
            success: true,
            postsDeleted: postCount,
            clustersDeleted: clusterCount
        ])
    }
}
