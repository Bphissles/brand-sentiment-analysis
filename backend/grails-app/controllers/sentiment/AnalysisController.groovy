package sentiment

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.Instant

/**
 * REST controller for Analysis operations
 * Handles triggering ML analysis and managing analysis runs
 */
@Tag(name = "Analysis", description = "ML analysis and processing")
class AnalysisController {

    static responseFormats = ['json']
    static allowedMethods = [index: 'GET', show: 'GET', trigger: 'POST', loadFixtures: 'POST', fixtureCount: 'GET']

    AnalysisService analysisService
    DataLoaderService dataLoaderService

    /**
     * GET /api/analysis
     * List all analysis runs
     */
    @Operation(summary = "List analysis runs", description = "List all analysis runs")
    @ApiResponse(responseCode = "200", description = "List of analysis runs")
    def index() {
        def runs = AnalysisRun.list(sort: 'dateCreated', order: 'desc', max: 20)
        respond([runs: runs])
    }

    /**
     * GET /api/analysis/{id}
     * Get a single analysis run
     */
    @Operation(summary = "Get analysis run", description = "Get a single analysis run by ID")
    @ApiResponse(responseCode = "200", description = "Analysis run details")
    @ApiResponse(responseCode = "404", description = "Analysis run not found")
    def show(@Parameter(description = "Analysis run ID") Long id) {
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
    @Operation(
        summary = "Trigger analysis",
        description = "Trigger a new ML analysis on all posts for clustering and sentiment"
    )
    @ApiResponse(responseCode = "200", description = "Analysis completed successfully")
    @ApiResponse(responseCode = "500", description = "Analysis failed")
    def trigger() {
        // Step 1: Create analysis run (short transaction)
        def run = analysisService.createAnalysisRun()

        try {
            // Step 2: Get all posts (read-only, no transaction needed)
            def posts = Post.list()
            
            if (posts.isEmpty()) {
                analysisService.completeEmptyAnalysisRun(run)
                respond([
                    run: run,
                    message: 'No posts to analyze'
                ])
                return
            }

            // Step 3: Call ML Engine (no transaction - external HTTP call)
            def result = analysisService.callMlEngine(posts)

            if (!result.success) {
                analysisService.failAnalysisRun(run, result.error)
                render status: 500, text: [error: result.error, run: run] as JSON
                return
            }

            // Step 4: Save results to database (short transaction)
            def saveResults = analysisService.saveAnalysisResults(run, result)

            // Step 5: Mark run as completed (short transaction)
            analysisService.completeAnalysisRun(run, saveResults)

            respond([
                run: run,
                clusters: result.clusters?.size() ?: 0,
                postsAnalyzed: result.postsAnalyzed
            ])

        } catch (Exception e) {
            log.error("Analysis failed", e)
            analysisService.failAnalysisRun(run, e.message)
            render status: 500, text: [error: e.message, run: run] as JSON
        }
    }

    /**
     * GET /api/analysis/fixture-count
     * Get count of available fixture posts without loading them
     */
    @Operation(summary = "Count fixtures", description = "Get count of available fixture posts")
    @ApiResponse(responseCode = "200", description = "Fixture counts returned")
    def fixtureCount() {
        try {
            def result = dataLoaderService.countFixtures()
            respond([
                total: result.total,
                sources: result.sources
            ])
        } catch (Exception e) {
            log.error("Failed to count fixtures", e)
            render status: 500, text: [error: e.message] as JSON
        }
    }

    /**
     * POST /api/analysis/load-fixtures
     * Load mock data from fixtures into database
     */
    @Operation(summary = "Load fixtures", description = "Load sample data from fixtures into database")
    @ApiResponse(responseCode = "200", description = "Fixtures loaded successfully")
    @ApiResponse(responseCode = "500", description = "Failed to load fixtures")
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
    @Operation(summary = "Clear data", description = "Clear all posts and clusters (for testing)")
    @ApiResponse(responseCode = "200", description = "Data cleared successfully")
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
