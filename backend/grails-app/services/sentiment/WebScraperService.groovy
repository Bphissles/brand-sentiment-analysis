package sentiment

import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/**
 * Service for scraping web content from various sources
 * Uses Gemini API with Google Search grounding for real web content extraction
 */
@Transactional
class WebScraperService {

    GeminiService geminiService

    // Default search queries for different platforms
    private static final Map<String, List<String>> SEARCH_QUERIES = [
        twitter: [
            'Peterbilt truck site:twitter.com OR site:x.com',
            'Peterbilt 579 OR 589 OR 389 trucker',
            '#Peterbilt truck driver review'
        ],
        youtube: [
            'Peterbilt truck review comments',
            'Peterbilt 579EV electric truck',
            'Peterbilt vs Kenworth trucker opinion'
        ],
        forums: [
            'Peterbilt site:reddit.com/r/Truckers',
            'Peterbilt site:thetruckersreport.com',
            'Peterbilt truck owner experience forum'
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
    List<Map> searchForRealPosts(String sourceType, int maxPosts = 15) {
        log.info("Searching web for real ${sourceType} posts about Peterbilt...")
        
        def allPosts = []
        def queries = SEARCH_QUERIES[sourceType] ?: ['Peterbilt truck']
        
        // Run multiple search queries to get diverse results
        queries.each { query ->
            try {
                log.info("Searching: ${query}")
                def searchResult = geminiService.searchWebForPosts(query, sourceType, maxPosts)
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
    List<Map> scrapeTwitter(String searchQuery = "peterbilt", int maxPosts = 15) {
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
    List<Map> scrapeYouTube(String searchQuery = "peterbilt truck review", int maxPosts = 10) {
        return searchForRealPosts("youtube", maxPosts)
    }

    /**
     * Search forums for real posts about Peterbilt
     * Uses Gemini Google Search grounding to find actual forum discussions
     * 
     * @param maxPosts Maximum posts to return
     * @return List of real post maps
     */
    List<Map> scrapeForums(int maxPosts = 10) {
        return searchForRealPosts("forums", maxPosts)
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
     */
    private List<Map> parseExtractedPosts(String jsonString, String sourceType) {
        if (!jsonString || jsonString.startsWith("[Error") || jsonString.startsWith("[Gemini")) {
            log.warn("Invalid extraction response: ${jsonString?.take(100)}")
            return []
        }

        try {
            // Clean up the JSON string (Gemini sometimes adds markdown)
            def cleanJson = jsonString
                .replaceAll(/```json\s*/, '')
                .replaceAll(/```\s*/, '')
                .trim()
            
            // Find the JSON array in the response
            def startIdx = cleanJson.indexOf('[')
            def endIdx = cleanJson.lastIndexOf(']')
            if (startIdx >= 0 && endIdx > startIdx) {
                cleanJson = cleanJson.substring(startIdx, endIdx + 1)
            }
            
            // Normalize quotes - Gemini sometimes returns curly/smart quotes
            cleanJson = cleanJson
                .replace('\u201c', '"')   // Left double quote "
                .replace('\u201d', '"')   // Right double quote "
                .replace('\u2018', "'")   // Left single quote '
                .replace('\u2019', "'")   // Right single quote '
                .replace('\u201e', '"')   // Low double quote „
                .replace('\u201f', '"')   // Double high-reversed-9 ‟
                .replace('\u0022', '"')   // Standard quote
            
            // Fix unescaped quotes inside JSON string values
            // This regex finds content between "content":" and the next ", and escapes internal quotes
            cleanJson = fixInternalQuotes(cleanJson)
            
            // Try to parse, with fallback for common issues
            def posts
            try {
                posts = new JsonSlurper().parseText(cleanJson)
            } catch (Exception parseError) {
                // Try fixing common JSON issues
                log.warn("Initial parse failed, attempting to fix JSON: ${parseError.message}")
                
                // Sometimes Gemini uses single quotes - convert to double
                def fixedJson = cleanJson.replaceAll(/(?<!\\)'/, '"')
                
                // Try again with LAX parser
                def slurper = new JsonSlurper().setType(groovy.json.JsonParserType.LAX)
                posts = slurper.parseText(fixedJson)
            }
            
            if (posts instanceof List) {
                return posts.collect { post ->
                    [
                        content: post.content?.toString() ?: '',
                        author: post.author?.toString() ?: 'Anonymous',
                        source: sourceType,
                        postUrl: post.postUrl?.toString() ?: post.url?.toString() ?: '',
                        sourceSite: post.sourceSite?.toString() ?: '',
                        publishedAt: parseDate(post.publishedAt),
                        externalId: generateExternalId(post, sourceType)
                    ]
                }.findAll { it.content?.length() > 10 }
            }
            
            return []
            
        } catch (Exception e) {
            log.error("Failed to parse extracted posts: ${e.message}")
            log.warn("Raw response (first 1000 chars): ${jsonString?.take(1000)}")
            return []
        }
    }

    /**
     * Fix unescaped quotes inside JSON string values
     * Gemini sometimes returns quotes inside content that break JSON parsing
     */
    private String fixInternalQuotes(String json) {
        // Simple approach: replace problematic patterns
        // Pattern: ": "...text with "quotes" inside..." becomes properly escaped
        def result = new StringBuilder()
        def inString = false
        def escaped = false
        def fieldStart = false
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i)
            
            if (escaped) {
                result.append(c)
                escaped = false
                continue
            }
            
            if (c == '\\' as char) {
                escaped = true
                result.append(c)
                continue
            }
            
            if (c == '"' as char) {
                // Check if this quote is a JSON structural quote or content quote
                if (!inString) {
                    inString = true
                    result.append(c)
                } else {
                    // Look ahead to see if this ends the string
                    def nextNonSpace = findNextNonSpace(json, i + 1)
                    if (nextNonSpace == ',' as char || nextNonSpace == '}' as char || 
                        nextNonSpace == ']' as char || nextNonSpace == ':' as char) {
                        // This is a closing quote
                        inString = false
                        result.append(c)
                    } else {
                        // This is an internal quote - escape it
                        result.append("\\'")
                    }
                }
            } else {
                result.append(c)
            }
        }
        
        return result.toString()
    }
    
    private char findNextNonSpace(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i)
            if (c != ' ' as char && c != '\n' as char && c != '\r' as char && c != '\t' as char) {
                return c
            }
        }
        return '\0' as char
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
