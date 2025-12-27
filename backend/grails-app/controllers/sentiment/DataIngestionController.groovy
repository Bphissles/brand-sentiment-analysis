package sentiment

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * Controller for data ingestion operations
 * Handles web scraping and post import from various sources
 */
@Tag(name = "Data Ingestion", description = "Web scraping and data import")
class DataIngestionController {

    static responseFormats = ['json']
    static allowedMethods = [
        scrapeAll: 'POST',
        scrapeSource: 'POST',
        manualImport: 'POST',
        status: 'GET'
    ]

    IngestionService ingestionService

    // Track scraping status
    private static Map scrapingStatus = [
        running: false,
        lastRun: null,
        lastResult: null
    ]

    /**
     * GET /api/ingestion/status
     * Get the current status of data ingestion
     */
    @Operation(summary = "Ingestion status", description = "Get the current status of data ingestion")
    @ApiResponse(responseCode = "200", description = "Ingestion status")
    def status() {
        def postCount = Post.count()
        def sourceBreakdown = Post.executeQuery(
            "SELECT p.source, COUNT(p) FROM Post p GROUP BY p.source"
        ).collectEntries { [it[0], it[1]] }

        respond([
            status: scrapingStatus.running ? 'running' : 'idle',
            lastRun: scrapingStatus.lastRun,
            lastResult: scrapingStatus.lastResult,
            totalPosts: postCount,
            sourceBreakdown: sourceBreakdown,
            geminiConfigured: isGeminiConfigured()
        ])
    }

    /**
     * POST /api/ingestion/scrape-all
     * Scrape all configured sources and import posts
     */
    @Operation(summary = "Scrape all sources", description = "Scrape all configured sources and import posts")
    @ApiResponse(responseCode = "200", description = "Scraping completed")
    @ApiResponse(responseCode = "400", description = "Gemini API not configured")
    @ApiResponse(responseCode = "409", description = "Scraping already in progress")
    def scrapeAll() {
        if (scrapingStatus.running) {
            render status: 409, text: [error: 'Scraping already in progress'] as JSON
            return
        }

        if (!isGeminiConfigured()) {
            render status: 400, text: [
                error: 'Gemini API key not configured',
                message: 'Set GEMINI_API_KEY environment variable to enable web scraping'
            ] as JSON
            return
        }

        scrapingStatus.running = true
        scrapingStatus.lastRun = new Date()

        try {
            // Use IngestionService for proper transaction boundaries
            def result = ingestionService.scrapeAndImportAll()
            def results = result.results
            def imported = result.imported
            def errors = result.errors

            scrapingStatus.lastResult = [
                success: true,
                imported: imported,
                total: imported.values().sum(),
                errors: errors
            ]

            respond([
                success: true,
                imported: imported,
                totalImported: imported.values().sum(),
                errors: errors
            ])

        } catch (Exception e) {
            log.error("Scraping failed", e)
            scrapingStatus.lastResult = [success: false, error: e.message]
            render status: 500, text: [error: e.message] as JSON
            
        } finally {
            scrapingStatus.running = false
        }
    }

    /**
     * POST /api/ingestion/scrape/{source}
     * Scrape a specific source
     */
    @Operation(summary = "Scrape source", description = "Scrape a specific source (twitter, youtube, reddit, forums, news)")
    @ApiResponse(responseCode = "200", description = "Scraping completed")
    @ApiResponse(responseCode = "400", description = "Invalid source or Gemini not configured")
    def scrapeSource(@Parameter(description = "Source name") String source) {
        if (!['twitter', 'youtube', 'reddit', 'forums', 'news'].contains(source)) {
            render status: 400, text: [error: "Invalid source: ${source}"] as JSON
            return
        }

        if (!isGeminiConfigured()) {
            render status: 400, text: [
                error: 'Gemini API key not configured'
            ] as JSON
            return
        }

        try {
            // Use IngestionService for proper transaction boundaries
            def result = ingestionService.scrapeAndImport(source)

            respond([
                success: true,
                source: source,
                scraped: result.posts,
                imported: result.imported
            ])

        } catch (Exception e) {
            log.error("Failed to scrape ${source}", e)
            render status: 500, text: [error: e.message] as JSON
        }
    }

    /**
     * POST /api/ingestion/import
     * Import posts from JSON body (manual import)
     */
    @Operation(summary = "Manual import", description = "Import posts from JSON body")
    @ApiResponse(responseCode = "200", description = "Import successful")
    @ApiResponse(responseCode = "400", description = "Missing posts array")
    @ApiResponse(responseCode = "500", description = "Import failed")
    def manualImport() {
        def json = request.JSON
        
        if (!json?.posts) {
            render status: 400, text: [error: 'Missing posts array in request body'] as JSON
            return
        }

        try {
            // Use IngestionService for proper transaction boundaries
            def result = ingestionService.importPosts(json.posts as List)
            respond([
                success: true, 
                imported: result.imported,
                skipped: result.skipped,
                total: result.total
            ])
        } catch (Exception e) {
            log.error("Import failed", e)
            render status: 500, text: [error: e.message] as JSON
        }
    }

    /**
     * Check if Gemini API is configured
     */
    private boolean isGeminiConfigured() {
        def apiKey = grailsApplication.config.getProperty('gemini.apiKey', String)
        return apiKey && apiKey != 'your_gemini_api_key_here'
    }
}
