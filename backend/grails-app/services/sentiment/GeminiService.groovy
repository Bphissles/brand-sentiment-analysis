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

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    /**
     * Generate a business insight for a cluster of posts
     * 
     * @param clusterLabel The cluster category label
     * @param keywords Top keywords in the cluster
     * @param samplePosts Sample post content for context
     * @param sentiment Average sentiment score
     * @return Generated insight text
     */
    String generateClusterInsight(String clusterLabel, List<String> keywords, List<String> samplePosts, Double sentiment) {
        def prompt = buildInsightPrompt(clusterLabel, keywords, samplePosts, sentiment)
        return callGeminiApi(prompt)
    }

    /**
     * Extract structured data from raw web content
     * Used for parsing scraped pages into post format
     * 
     * @param rawContent Raw HTML or text content
     * @param sourceType The source type (twitter, youtube, forums)
     * @return Extracted post data as JSON string
     */
    String extractPostData(String rawContent, String sourceType) {
        def prompt = buildExtractionPrompt(rawContent, sourceType)
        return callGeminiApi(prompt)
    }

    /**
     * Build prompt for cluster insight generation
     */
    private String buildInsightPrompt(String clusterLabel, List<String> keywords, List<String> samplePosts, Double sentiment) {
        def sentimentLabel = sentiment >= 0.3 ? "positive" : (sentiment <= -0.3 ? "negative" : "mixed")
        
        return """You are a marketing analyst for Peterbilt Trucks. Analyze the following customer feedback cluster and provide a business insight.

CLUSTER: ${clusterLabel}
TOP KEYWORDS: ${keywords.join(", ")}
OVERALL SENTIMENT: ${sentimentLabel} (score: ${sentiment})

SAMPLE CUSTOMER FEEDBACK:
${samplePosts.take(5).collect { "- \"${it.take(200)}...\"" }.join("\n")}

Provide a 2-3 sentence business insight that:
1. Summarizes what customers are saying about this topic
2. Explains why this matters for Peterbilt's marketing and sales teams
3. Suggests a potential action or opportunity

Keep the tone professional and actionable. Focus on business implications."""
    }

    /**
     * Build prompt for web content extraction
     */
    private String buildExtractionPrompt(String rawContent, String sourceType) {
        return """Extract social media post data from the following ${sourceType} content.

RAW CONTENT:
${rawContent.take(5000)}

Return a JSON array of posts with this structure:
[
  {
    "content": "The post text",
    "author": "Username or handle",
    "publishedAt": "ISO date if available",
    "postUrl": "URL to original post if available"
  }
]

Only include posts that mention Peterbilt trucks, trucking, or related topics.
Return valid JSON only, no additional text."""
    }

    /**
     * Call the Gemini API
     * 
     * @param prompt The prompt to send
     * @return The generated text response
     */
    private String callGeminiApi(String prompt) {
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
