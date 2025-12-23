package sentiment

import grails.gorm.transactions.Transactional

/**
 * Service for managing AI-generated insights
 * Handles caching and regeneration of insights
 */
@Transactional
class AiInsightService {

    GeminiService geminiService

    /**
     * Get or generate all AI insights for the current analysis
     * Returns cached insights if they exist and are current
     */
    Map getInsights(String source = 'all') {
        def latestRun = AnalysisRun.findByStatus('completed', [sort: 'completedAt', order: 'desc'])
        def analysisRunId = latestRun?.id
        
        // Check for existing insights for this analysis run
        def existingInsights = AiInsight.findAllByAnalysisRunIdAndSource(analysisRunId, source)
        
        if (existingInsights) {
            return [
                trendAnalysis: existingInsights.find { it.type == 'trend_analysis' }?.content,
                recommendations: existingInsights.find { it.type == 'recommendations' }?.content,
                executiveSummary: existingInsights.find { it.type == 'executive_summary' }?.content,
                generatedAt: existingInsights[0]?.dateCreated,
                cached: true
            ]
        }
        
        // No cached insights, return null (frontend will show placeholders)
        return [
            trendAnalysis: null,
            recommendations: null,
            executiveSummary: null,
            cached: false
        ]
    }

    /**
     * Generate and store new AI insights
     * Called after a new analysis run completes
     */
    Map generateInsights(String source = 'all') {
        def latestRun = AnalysisRun.findByStatus('completed', [sort: 'completedAt', order: 'desc'])
        def analysisRunId = latestRun?.id
        
        // Get current data for insight generation
        def clusters = Cluster.list()
        def posts = Post.list()
        
        // Apply source filter if specified
        if (source && source != 'all') {
            posts = posts.findAll { it.source == source }
            def postIds = posts.collect { it.id.toString() }
            clusters = clusters.findAll { cluster ->
                cluster.postIds?.any { it in postIds }
            }
        }
        
        def totalPosts = posts.size()
        def clusterCount = clusters.size()
        
        // Calculate sentiment distribution
        def sentimentDistribution = [
            positive: posts.count { it.sentimentLabel == 'positive' },
            neutral: posts.count { it.sentimentLabel == 'neutral' },
            negative: posts.count { it.sentimentLabel == 'negative' }
        ]
        
        // Calculate average sentiment
        def avgSentiment = posts ? posts.sum { it.sentimentCompound ?: 0 } / posts.size() : 0.0
        
        // Prepare cluster data for Gemini
        def clusterData = clusters.collect { cluster ->
            [
                label: cluster.label,
                postCount: cluster.postCount,
                sentimentLabel: cluster.sentimentLabel,
                sentiment: cluster.sentiment
            ]
        }.sort { -it.postCount }
        
        // Delete old insights for this source
        AiInsight.findAllBySource(source).each { it.delete() }
        
        // Generate new insights via Gemini
        def trendAnalysis = geminiService.generateTrendAnalysis(sentimentDistribution, clusterData, totalPosts)
        def recommendations = geminiService.generateRecommendations(clusterData, sentimentDistribution)
        def executiveSummary = geminiService.generateExecutiveSummary(totalPosts, clusterCount, avgSentiment, clusterData, sentimentDistribution)
        
        // Store insights
        def insights = []
        
        insights << new AiInsight(
            type: 'trend_analysis',
            content: trendAnalysis,
            source: source,
            analysisRunId: analysisRunId,
            postsAnalyzed: totalPosts,
            clustersCount: clusterCount
        ).save(flush: true)
        
        insights << new AiInsight(
            type: 'recommendations',
            content: recommendations,
            source: source,
            analysisRunId: analysisRunId,
            postsAnalyzed: totalPosts,
            clustersCount: clusterCount
        ).save(flush: true)
        
        insights << new AiInsight(
            type: 'executive_summary',
            content: executiveSummary,
            source: source,
            analysisRunId: analysisRunId,
            postsAnalyzed: totalPosts,
            clustersCount: clusterCount
        ).save(flush: true)
        
        return [
            trendAnalysis: trendAnalysis,
            recommendations: recommendations,
            executiveSummary: executiveSummary,
            generatedAt: new Date(),
            cached: false
        ]
    }

    /**
     * Clear all cached insights (called when data changes)
     */
    void clearInsights() {
        AiInsight.executeUpdate("delete from AiInsight")
    }
}
