package sentiment

import grails.converters.JSON
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * REST controller for AI Insights
 * Handles fetching and regenerating AI-generated insights
 */
@Tag(name = "Insights", description = "AI-generated insights")
class AiInsightController {

    static responseFormats = ['json']
    static allowedMethods = [index: 'GET', generate: 'POST']

    AiInsightService aiInsightService

    /**
     * GET /api/insights
     * Get cached AI insights for the current analysis
     */
    @Operation(summary = "Get insights", description = "Get cached AI insights for the current analysis")
    @ApiResponse(responseCode = "200", description = "AI insights")
    def index() {
        def source = params.source ?: 'all'
        def insights = aiInsightService.getInsights(source)
        respond insights
    }

    /**
     * POST /api/insights/generate
     * Generate new AI insights (clears cache and regenerates)
     */
    @Operation(summary = "Generate insights", description = "Generate new AI insights (clears cache)")
    @ApiResponse(responseCode = "200", description = "Generated insights")
    @ApiResponse(responseCode = "500", description = "Failed to generate insights")
    def generate() {
        def source = params.source ?: 'all'
        
        try {
            def insights = aiInsightService.generateInsights(source)
            respond insights
        } catch (Exception e) {
            log.error("Failed to generate insights", e)
            render status: 500, text: [error: "Failed to generate insights: ${e.message}"] as JSON
        }
    }
}
