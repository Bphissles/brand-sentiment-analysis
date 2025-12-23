package sentiment

import grails.converters.JSON

/**
 * REST controller for AI Insights
 * Handles fetching and regenerating AI-generated insights
 */
class AiInsightController {

    static responseFormats = ['json']
    static allowedMethods = [index: 'GET', generate: 'POST']

    AiInsightService aiInsightService

    /**
     * GET /api/insights
     * Get cached AI insights for the current analysis
     */
    def index() {
        def source = params.source ?: 'all'
        def insights = aiInsightService.getInsights(source)
        respond insights
    }

    /**
     * POST /api/insights/generate
     * Generate new AI insights (clears cache and regenerates)
     */
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
