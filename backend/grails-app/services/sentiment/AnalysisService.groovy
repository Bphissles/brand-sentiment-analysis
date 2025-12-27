package sentiment

import grails.gorm.transactions.Transactional
import java.time.Instant

/**
 * Service for ML analysis orchestration
 * Handles transaction boundaries for analysis operations
 */
class AnalysisService {

    MlEngineService mlEngineService

    /**
     * Create a new analysis run record
     * Short transaction - just creates the record
     */
    @Transactional
    AnalysisRun createAnalysisRun() {
        def run = new AnalysisRun(
            status: 'processing',
            startedAt: Instant.now()
        )
        run.save(flush: true, failOnError: true)
        return run
    }

    /**
     * Call ML Engine to analyze posts
     * No transaction - external HTTP call
     */
    Map callMlEngine(List<Post> posts) {
        return mlEngineService.analyzePostsForClusters(posts)
    }

    /**
     * Save ML analysis results to database
     * Short transaction - just saves clusters and updates posts
     */
    @Transactional
    Map saveAnalysisResults(AnalysisRun run, Map mlResult) {
        // Clear previous clusters
        Cluster.executeUpdate('delete from Cluster')
        Post.executeUpdate('update Post set clusterId = null')

        // Save clusters from ML response
        def clustersCreated = 0
        mlResult.clusters.each { clusterData ->
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
            cluster.save(flush: true, failOnError: true)
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
        mlResult.posts?.each { postData ->
            def post = null
            try {
                post = Post.get(postData.id as Long)
            } catch (Exception e) {
                post = Post.findByExternalId(postData.id.toString())
            }
            
            if (post && postData.sentiment) {
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
            }
        }

        return [
            clustersCreated: clustersCreated,
            postsAnalyzed: mlResult.postsAnalyzed,
            processingTimeMs: mlResult.processingTimeMs
        ]
    }

    /**
     * Mark analysis run as completed
     * Short transaction - just updates the record
     */
    @Transactional
    void completeAnalysisRun(AnalysisRun run, Map results) {
        run.status = 'completed'
        run.postsAnalyzed = results.postsAnalyzed
        run.clustersCreated = results.clustersCreated
        run.completedAt = Instant.now()
        run.durationMs = results.processingTimeMs
        run.save(flush: true, failOnError: true)
    }

    /**
     * Mark analysis run as failed
     * Short transaction - just updates the record
     */
    @Transactional
    void failAnalysisRun(AnalysisRun run, String error) {
        run.status = 'failed'
        run.error = error
        run.completedAt = Instant.now()
        run.save(flush: true, failOnError: true)
    }

    /**
     * Mark analysis run as completed with no posts
     * Short transaction - just updates the record
     */
    @Transactional
    void completeEmptyAnalysisRun(AnalysisRun run) {
        run.status = 'completed'
        run.postsAnalyzed = 0
        run.clustersCreated = 0
        run.completedAt = Instant.now()
        run.durationMs = 0
        run.save(flush: true, failOnError: true)
    }
}
