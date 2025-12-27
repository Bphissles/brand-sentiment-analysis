package sentiment

import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/**
 * Service for interacting with Google Gemini API
 * Handles web content extraction and insight generation
 */
@Transactional
class GeminiService {

    // Injected from application.yml
    def grailsApplication

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    /**
     * Search the web using Gemini with Google Search grounding
     * Returns real content from the web, not generated/synthetic data
     * 
     * @param searchQuery The search query
     * @param sourceType The source type for context (twitter, youtube, forums)
     * @param maxResults Approximate number of posts to find
     * @return JSON string with extracted posts from real web sources
     */
    String searchWebForPosts(String searchQuery, String sourceType, int maxResults = 500) {
        def prompt = buildSearchPrompt(searchQuery, sourceType, maxResults)
        return callGeminiApiWithSearch(prompt)
    }

    /**
     * Build prompt for web search extraction
     */
    private String buildSearchPrompt(String searchQuery, String sourceType, int maxResults) {
        def platformContext
        switch(sourceType) {
            case 'twitter':
                platformContext = 'Twitter/X posts, tweets'
                break
            case 'youtube':
                platformContext = 'YouTube video comments and descriptions'
                break
            case 'reddit':
                platformContext = 'Reddit posts and comments from r/Truckers, r/trucking, r/Peterbilt, and other trucking-related subreddits'
                break
            case 'forums':
                platformContext = 'trucking forum posts from sites like TruckersReport, TheTruckersReport.com, TruckingTruth, and other trucking community forums'
                break
            case 'news':
                platformContext = 'news articles and press releases from trucking industry sources like FleetOwner, Transport Topics, Commercial Carrier Journal, Trucking Info, and general news outlets'
                break
            default:
                platformContext = 'social media posts'
        }
        
        return """Search the web for recent ${platformContext} about: ${searchQuery}

Find REAL posts from REAL users discussing this topic. Focus on:
- Actual user opinions and experiences  
- Recent discussions (within the last few months)
- Varied sentiments (positive, negative, neutral)

CRITICAL JSON FORMATTING RULES:
1. Return ONLY a valid JSON array - no markdown, no explanation
2. Use straight double quotes for JSON structure: "
3. Inside content strings, replace any quotes with single quotes or remove them
4. Do not use curly/smart quotes anywhere

Example format:
[{"content":"User said this truck is great and they love it","author":"username","publishedAt":"2024-12-20","postUrl":"https://example.com","sourceSite":"reddit.com"}]

Find up to ${maxResults} real posts. Only return actual content found on the web."""
    }

    /**
     * Generate AI trend analysis based on sentiment distribution and clusters
     */
    String generateTrendAnalysis(Map sentimentDistribution, List<Map> clusters, Integer totalPosts) {
        def prompt = """You are a marketing analyst for Peterbilt Trucks. Analyze the following sentiment data and provide a brief trend analysis.

SENTIMENT DISTRIBUTION:
- Positive posts: ${sentimentDistribution.positive ?: 0}
- Neutral posts: ${sentimentDistribution.neutral ?: 0}
- Negative posts: ${sentimentDistribution.negative ?: 0}
- Total posts analyzed: ${totalPosts}

TOP DISCUSSION TOPICS:
${clusters.take(4).collect { "- ${it.label}: ${it.postCount} posts, sentiment: ${it.sentimentLabel}" }.join("\n")}

Provide a 2-3 sentence trend analysis that:
1. Summarizes the overall sentiment trend
2. Highlights which topic has the strongest engagement
Keep it concise and professional. No bullet points, just flowing text."""

        return callGeminiApi(prompt)
    }

    /**
     * Generate AI recommendations based on clusters and sentiment
     */
    String generateRecommendations(List<Map> clusters, Map sentimentDistribution) {
        def negativeClusters = clusters.findAll { it.sentimentLabel == 'negative' }
        def positiveClusters = clusters.findAll { it.sentimentLabel == 'positive' }
        
        def prompt = """You are a marketing strategist for Peterbilt Trucks. Based on customer feedback analysis, provide actionable recommendations.

CLUSTERS WITH NEGATIVE SENTIMENT:
${negativeClusters.take(3).collect { "- ${it.label}: ${it.postCount} posts" }.join("\n") ?: "None"}

CLUSTERS WITH POSITIVE SENTIMENT:
${positiveClusters.take(3).collect { "- ${it.label}: ${it.postCount} posts" }.join("\n") ?: "None"}

OVERALL DISTRIBUTION:
- Positive: ${sentimentDistribution.positive ?: 0}
- Negative: ${sentimentDistribution.negative ?: 0}

Provide 2-3 sentences of strategic recommendations that:
1. Address any concerns in negative clusters
2. Suggest how to amplify positive discussions
Keep it concise and actionable. No bullet points."""

        return callGeminiApi(prompt)
    }

    /**
     * Generate executive summary for stakeholder reporting
     */
    String generateExecutiveSummary(Integer totalPosts, Integer clusterCount, Double avgSentiment, List<Map> clusters, Map sentimentDistribution) {
        def sentimentLabel = avgSentiment >= 0.1 ? "positive" : (avgSentiment <= -0.1 ? "negative" : "neutral")
        def topTopics = clusters.take(4).collect { it.label }.join(", ")
        
        def prompt = """You are preparing an executive summary for Peterbilt Trucks leadership. Create a professional narrative summary.

ANALYSIS OVERVIEW:
- Total posts analyzed: ${totalPosts}
- Topic clusters identified: ${clusterCount}
- Overall brand sentiment: ${sentimentLabel} (score: ${String.format("%.2f", avgSentiment)})
- Key topics: ${topTopics}

SENTIMENT BREAKDOWN:
- Positive: ${sentimentDistribution.positive ?: 0} posts
- Neutral: ${sentimentDistribution.neutral ?: 0} posts  
- Negative: ${sentimentDistribution.negative ?: 0} posts

TOP CLUSTERS:
${clusters.take(4).collect { "- ${it.label}: ${it.postCount} posts, ${it.sentimentLabel} sentiment" }.join("\n")}

Write a 3-4 sentence executive summary suitable for stakeholder presentations. Include:
1. High-level findings
2. Key themes driving conversation
3. Overall brand health assessment
Professional tone, no bullet points, flowing narrative."""

        return callGeminiApi(prompt)
    }

    /**
     * Call the Gemini API with Google Search grounding enabled
     * This allows Gemini to search the web for real content
     * 
     * @param prompt The prompt to send
     * @return The generated text response with real web data
     */
    String callGeminiApiWithSearch(String prompt) {
        def apiKey = grailsApplication.config.getProperty('gemini.apiKey', String)
        
        if (!apiKey || apiKey == 'your_gemini_api_key_here') {
            log.warn("Gemini API key not configured")
            return "[Gemini API not configured]"
        }

        try {
            // Use gemini-2.0-flash for search grounding
            def url = new URL("${GEMINI_API_URL}?key=${apiKey}")
            def connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 120000  // Longer timeout for search

            // Include google_search tool for grounding
            def requestBody = JsonOutput.toJson([
                contents: [
                    [
                        parts: [
                            [text: prompt]
                        ]
                    ]
                ],
                tools: [
                    [
                        google_search: [:]  // Enable Google Search grounding
                    ]
                ],
                generationConfig: [
                    temperature: 0.3,  // Lower temperature for factual extraction
                    maxOutputTokens: 4096
                ]
            ])

            connection.outputStream.withWriter("UTF-8") { writer ->
                writer.write(requestBody)
            }

            def responseCode = connection.responseCode
            
            if (responseCode == 200) {
                def response = new JsonSlurper().parseText(connection.inputStream.text)
                
                // Log grounding metadata for debugging
                def groundingMetadata = response?.candidates?.getAt(0)?.groundingMetadata
                if (groundingMetadata) {
                    log.info("Search queries used: ${groundingMetadata.webSearchQueries}")
                    log.info("Sources found: ${groundingMetadata.groundingChunks?.size() ?: 0}")
                }
                
                return response?.candidates?.getAt(0)?.content?.parts?.getAt(0)?.text ?: ""
            } else {
                def errorBody = connection.errorStream?.text ?: "No error details"
                log.error("Gemini API error: ${responseCode} - ${errorBody}")
                return "[Error calling Gemini API: ${responseCode}]"
            }
            
        } catch (Exception e) {
            log.error("Failed to call Gemini API with search", e)
            return "[Error: ${e.message}]"
        }
    }

    /**
     * Call the Gemini API (without search grounding)
     * 
     * @param prompt The prompt to send
     * @return The generated text response
     */
    String callGeminiApi(String prompt) {
        def apiKey = grailsApplication.config.getProperty('gemini.apiKey', String)
        
        if (!apiKey || apiKey == 'your_gemini_api_key_here') {
            log.warn("Gemini API key not configured, returning placeholder response")
            return "[Gemini API not configured - placeholder insight]"
        }

        try {
            def url = new URL("${GEMINI_API_URL}?key=${apiKey}")
            def connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            def requestBody = JsonOutput.toJson([
                contents: [
                    [
                        parts: [
                            [text: prompt]
                        ]
                    ]
                ],
                generationConfig: [
                    temperature: 0.7,
                    maxOutputTokens: 1024
                ]
            ])

            connection.outputStream.withWriter("UTF-8") { writer ->
                writer.write(requestBody)
            }

            def responseCode = connection.responseCode
            
            if (responseCode == 200) {
                def response = new JsonSlurper().parseText(connection.inputStream.text)
                return response?.candidates?.getAt(0)?.content?.parts?.getAt(0)?.text ?: ""
            } else {
                def errorBody = connection.errorStream?.text ?: "No error details"
                log.error("Gemini API error: ${responseCode} - ${errorBody}")
                return "[Error calling Gemini API: ${responseCode}]"
            }
            
        } catch (Exception e) {
            log.error("Failed to call Gemini API", e)
            return "[Error: ${e.message}]"
        }
    }
}
