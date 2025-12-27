package sentiment

import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/**
 * Service for scraping web content from various sources
 * Uses Gemini API with Google Search grounding for real web content extraction
 * Note: No @Transactional at class level - this service is primarily network/CPU bound
 * and does not require long-lived DB transactions during web scraping operations
 */
class WebScraperService {

    GeminiService geminiService

    // Default search queries for different platforms - more queries = more data
    // Approach 1: Broad search without site restrictions - let Gemini find relevant content
    private static final Map<String, List<String>> SEARCH_QUERIES = [
        twitter: [
            'Peterbilt truck owner opinion 2024',
            'Peterbilt driver review experience',
            'Peterbilt 579 589 problems complaints',
            'best Peterbilt truck model',
            'Peterbilt vs Kenworth driver opinion',
            'love my Peterbilt truck',
            'regret buying Peterbilt',
            'Peterbilt reliability issues',
            'Peterbilt 579EV electric truck review',
            'Peterbilt fuel economy real world'
        ],
        youtube: [
            'Peterbilt truck review owner experience',
            'Peterbilt 579 walkaround tour',
            'Peterbilt driver opinion honest review',
            'Peterbilt vs Kenworth comparison',
            'Peterbilt problems issues',
            'Peterbilt 579EV electric semi',
            'Peterbilt sleeper cab tour',
            'Peterbilt maintenance costs',
            'best semi truck Peterbilt',
            'Peterbilt long haul review'
        ],
        reddit: [
            'Peterbilt owner operator review',
            'Peterbilt 579 589 experience',
            'Peterbilt problems complaints',
            'Peterbilt vs Kenworth reliability',
            'Peterbilt dealer service experience',
            'Peterbilt truck worth it',
            'Peterbilt maintenance issues',
            'Peterbilt driver comfort',
            'Peterbilt resale value',
            'Peterbilt fuel economy discussion'
        ],
        forums: [
            'Peterbilt owner review forum',
            'Peterbilt 579 589 experience',
            'Peterbilt reliability issues discussion',
            'Peterbilt dealer service feedback',
            'Peterbilt truck problems',
            'Peterbilt maintenance cost forum',
            'Peterbilt vs competitors',
            'Peterbilt driver satisfaction',
            'Peterbilt long term ownership',
            'Peterbilt trucking forum discussion'
        ],
        news: [
            'Peterbilt truck news 2024',
            'Peterbilt electric truck 579EV',
            'Peterbilt autonomous technology',
            'Peterbilt dealer network',
            'Peterbilt industry awards',
            'Peterbilt new model announcement',
            'Peterbilt fleet management',
            'Peterbilt sustainability initiatives',
            'Peterbilt market share trucking',
            'Peterbilt innovation technology'
        ]
    ]

    /**
     * Search for real posts using Gemini with Google Search grounding
     * This finds ACTUAL content from the web, not synthetic data
     * 
     * @param sourceType The source type (twitter, youtube, forums)
     * @param maxPosts Approximate number of posts to find
     * @return List of real post maps from the web
     */
    List<Map> searchForRealPosts(String sourceType, int maxPosts = 30) {
        log.info("Searching web for real ${sourceType} posts about Peterbilt...")
        
        def allPosts = []
        def queries = SEARCH_QUERIES[sourceType] ?: ['Peterbilt truck']
        
        // Run multiple search queries to get diverse results
        queries.eachWithIndex { query, idx ->
            try {
                // Add delay between queries to avoid rate limiting (429 errors)
                if (idx > 0) {
                    log.info("Waiting 2 seconds before next query to avoid rate limits...")
                    Thread.sleep(2000)
                }
                
                log.info("Searching: ${query}")
                def searchResult = geminiService.searchWebForPosts(query, sourceType, 100)
                def posts = parseExtractedPosts(searchResult, sourceType)
                allPosts.addAll(posts)
                log.info("Found ${posts.size()} posts for query: ${query}")
            } catch (Exception e) {
                log.error("Search failed for query '${query}': ${e.message}")
            }
        }
        
        // Deduplicate by content hash
        def uniquePosts = allPosts.unique { post ->
            post.content?.take(100)?.hashCode()
        }
        
        log.info("Total unique ${sourceType} posts found: ${uniquePosts.size()}")
        return uniquePosts.take(maxPosts)
    }

    /**
     * Search Twitter/X for real posts about Peterbilt
     * Uses Gemini Google Search grounding to find actual tweets
     * 
     * @param searchQuery Additional search terms
     * @param maxPosts Maximum posts to return
     * @return List of real post maps
     */
    List<Map> scrapeTwitter(String searchQuery = "peterbilt", int maxPosts = 200) {
        return searchForRealPosts("twitter", maxPosts)
    }

    /**
     * Search YouTube for real comments about Peterbilt
     * Uses Gemini Google Search grounding to find actual comments
     * 
     * @param searchQuery Additional search terms
     * @param maxPosts Maximum posts to return
     * @return List of real post maps
     */
    List<Map> scrapeYouTube(String searchQuery = "peterbilt truck review", int maxPosts = 200) {
        return searchForRealPosts("youtube", maxPosts)
    }

    /**
     * Search Reddit for real posts about Peterbilt
     * Uses Gemini Google Search grounding to find actual Reddit discussions
     * 
     * @param maxPosts Maximum posts to return
     * @return List of real post maps
     */
    List<Map> scrapeReddit(int maxPosts = 200) {
        return searchForRealPosts("reddit", maxPosts)
    }

    /**
     * Search forums for real posts about Peterbilt
     * Uses Gemini Google Search grounding to find actual forum discussions
     * 
     * @param maxPosts Maximum posts to return
     * @return List of real post maps
     */
    List<Map> scrapeForums(int maxPosts = 200) {
        return searchForRealPosts("forums", maxPosts)
    }

    /**
     * Search news sources for articles about Peterbilt
     * Uses Gemini Google Search grounding to find actual news articles
     * 
     * @param maxPosts Maximum posts to return
     * @return List of real post maps
     */
    List<Map> scrapeNews(int maxPosts = 200) {
        return searchForRealPosts("news", maxPosts)
    }

    /**
     * Scrape all sources and return combined posts
     * 
     * @return Map with posts from each source
     */
    Map<String, List<Map>> scrapeAllSources() {
        log.info("Starting web search across all sources...")
        
        return [
            twitter: scrapeTwitter(),
            youtube: scrapeYouTube(),
            forums: scrapeForums()
        ]
    }

    /**
     * Parse Gemini's extracted JSON into post maps
     * Uses field-by-field extraction to handle malformed JSON from Gemini
     */
    private List<Map> parseExtractedPosts(String jsonString, String sourceType) {
        if (!jsonString || jsonString.startsWith("[Error") || jsonString.startsWith("[Gemini")) {
            log.warn("Invalid extraction response: ${jsonString?.take(100)}")
            return []
        }

        def posts = []
        
        try {
            // Clean up the response
            def text = jsonString
                .replaceAll(/```json\s*/, '')
                .replaceAll(/```\s*/, '')
                .trim()
            
            // Normalize curly/smart quotes to straight quotes
            text = text
                .replace('\u201c', '"')   // Left double quote "
                .replace('\u201d', '"')   // Right double quote "
                .replace('\u2018', "'")   // Left single quote '
                .replace('\u2019', "'")   // Right single quote '
                .replace('\u201e', '"')   // Low double quote „
                .replace('\u201f', '"')   // Double high-reversed-9 ‟
            
            // Strategy: Find each "content" field and extract the post around it
            // This is more robust than trying to parse the whole JSON
            def contentStarts = []
            def searchIdx = 0
            while (true) {
                def idx = text.indexOf('"content"', searchIdx)
                if (idx < 0) break
                contentStarts << idx
                searchIdx = idx + 1
            }
            
            log.info("Found ${contentStarts.size()} potential posts in response")
            
            contentStarts.each { contentIdx ->
                try {
                    def post = [:]
                    
                    // Find the opening brace before this content field
                    def braceIdx = text.lastIndexOf('{', contentIdx)
                    if (braceIdx < 0) return
                    
                    // Extract content value - find ": " after "content", then extract until we hit ", "author" or similar
                    def colonIdx = text.indexOf(':', contentIdx)
                    if (colonIdx < 0) return
                    
                    def valueStart = text.indexOf('"', colonIdx + 1)
                    if (valueStart < 0) return
                    valueStart++ // Move past the opening quote
                    
                    // Find the end of content - look for ", "author" or ", "publishedAt" etc
                    def valueEnd = findFieldEnd(text, valueStart)
                    if (valueEnd > valueStart) {
                        post.content = text.substring(valueStart, valueEnd)
                            .replace('\\"', '"')
                            .replace("\\'", "'")
                    }
                    
                    // Extract other fields from nearby text (within ~500 chars)
                    def searchEnd = Math.min(text.length(), contentIdx + 1500)
                    def nearbyText = text.substring(braceIdx, searchEnd)
                    
                    post.author = extractFieldValue(nearbyText, 'author') ?: 'Anonymous'
                    post.publishedAt = extractFieldValue(nearbyText, 'publishedAt')
                    post.postUrl = extractFieldValue(nearbyText, 'postUrl')
                    post.sourceSite = extractFieldValue(nearbyText, 'sourceSite')
                    
                    // Only add if we got meaningful content
                    if (post.content && post.content.length() > 10) {
                        posts << [
                            content: post.content,
                            author: post.author,
                            source: sourceType,
                            postUrl: post.postUrl ?: '',
                            sourceSite: post.sourceSite ?: '',
                            publishedAt: parseDate(post.publishedAt),
                            externalId: generateExternalId(post, sourceType)
                        ]
                    }
                } catch (Exception e) {
                    log.debug("Skipping malformed post: ${e.message}")
                }
            }
            
            log.info("Successfully extracted ${posts.size()} posts from ${sourceType}")
            return posts
            
        } catch (Exception e) {
            log.error("Failed to parse extracted posts: ${e.message}")
            return posts
        }
    }
    
    /**
     * Find the end of a JSON string value by looking for closing patterns
     */
    private int findFieldEnd(String text, int start) {
        // Look for patterns that indicate end of this field value
        // ", "fieldName" or "} or "]
        def patterns = ['", "', '","', '"}', '"]']
        def minEnd = text.length()
        
        patterns.each { pattern ->
            def idx = text.indexOf(pattern, start)
            if (idx > 0 && idx < minEnd) {
                minEnd = idx
            }
        }
        
        return minEnd
    }
    
    /**
     * Extract a field value from JSON-like text
     */
    private String extractFieldValue(String text, String fieldName) {
        def pattern = "\"${fieldName}\"\\s*:\\s*\"([^\"]*)\""
        def matcher = (text =~ pattern)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    /**
     * Parse date string to timestamp
     */
    private Long parseDate(String dateStr) {
        if (!dateStr) return System.currentTimeMillis()
        
        try {
            // Try ISO format first
            return Date.parse("yyyy-MM-dd'T'HH:mm:ss", dateStr).time
        } catch (Exception e) {
            try {
                return Date.parse("yyyy-MM-dd", dateStr).time
            } catch (Exception e2) {
                return System.currentTimeMillis()
            }
        }
    }

    /**
     * Generate a unique external ID for deduplication
     */
    private String generateExternalId(Map post, String sourceType) {
        def content = post.content?.toString() ?: ''
        def author = post.author?.toString() ?: ''
        return "${sourceType}-${(content + author).hashCode().abs()}"
    }
}
