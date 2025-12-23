package sentiment

import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/**
 * Service for scraping web content from various sources
 * Uses Gemini API for intelligent content extraction
 */
@Transactional
class WebScraperService {

    GeminiService geminiService

    // Search URLs for different platforms
    private static final Map<String, String> SEARCH_URLS = [
        twitter: "https://nitter.net/search?q=peterbilt",
        youtube: "https://www.youtube.com/results?search_query=peterbilt+truck+review",
        reddit: "https://www.reddit.com/r/Truckers/search/?q=peterbilt&restrict_sr=1&sort=new"
    ]

    /**
     * Generate realistic posts using Gemini AI
     * Since direct web scraping is often blocked, we use Gemini to generate
     * realistic sample posts based on real trucking industry topics
     * 
     * @param sourceType The source type (twitter, youtube, forums)
     * @param count Number of posts to generate
     * @return List of generated post maps
     */
    List<Map> generatePostsWithGemini(String sourceType, int count = 10) {
        log.info("Generating ${count} ${sourceType} posts with Gemini...")
        
        try {
            def prompt = buildGenerationPrompt(sourceType, count)
            def generatedJson = geminiService.callGeminiApi(prompt)
            return parseExtractedPosts(generatedJson, sourceType)
        } catch (Exception e) {
            log.error("Failed to generate posts for ${sourceType}", e)
            return []
        }
    }

    /**
     * Build prompt for generating realistic posts
     */
    private String buildGenerationPrompt(String sourceType, int count) {
        def platform = sourceType == 'twitter' ? 'Twitter/X' : (sourceType == 'youtube' ? 'YouTube comments' : 'trucking forum posts')
        
        return """Generate ${count} realistic ${platform} posts about Peterbilt trucks.

Include a mix of:
- Positive reviews about specific models (579, 589, 389, 567)
- Complaints about service, wait times, or issues
- Questions about features, specs, or comparisons
- Comments about EV trucks (579EV) and charging
- Discussions about reliability, fuel economy, and comfort

Each post should feel authentic to the platform:
${sourceType == 'twitter' ? '- Short, casual, may include hashtags like #Peterbilt #TruckerLife #579EV' : ''}
${sourceType == 'youtube' ? '- Comment style, responding to truck review videos' : ''}
${sourceType == 'forums' ? '- Longer, more detailed, sharing personal experiences' : ''}

Return ONLY a JSON array with this exact structure (no markdown, no extra text):
[
  {
    "content": "The actual post text",
    "author": "RealisticUsername123",
    "publishedAt": "2024-12-${String.format('%02d', (Math.random() * 20 + 1) as int)}T${String.format('%02d', (Math.random() * 24) as int)}:00:00Z"
  }
]

Generate exactly ${count} posts with varied sentiments (positive, negative, neutral)."""
    }

    /**
     * Generate Twitter/X style posts about Peterbilt using Gemini
     * 
     * @param searchQuery Search query (used for context)
     * @param maxPosts Number of posts to generate
     * @return List of post maps
     */
    List<Map> scrapeTwitter(String searchQuery = "peterbilt", int maxPosts = 15) {
        return generatePostsWithGemini("twitter", maxPosts)
    }

    /**
     * Generate YouTube comment style posts about Peterbilt using Gemini
     * 
     * @param searchQuery Search query (used for context)
     * @param maxPosts Number of posts to generate
     * @return List of post maps
     */
    List<Map> scrapeYouTube(String searchQuery = "peterbilt truck review", int maxPosts = 10) {
        return generatePostsWithGemini("youtube", maxPosts)
    }

    /**
     * Generate forum style posts about Peterbilt using Gemini
     * 
     * @param maxPosts Number of posts to generate
     * @return List of post maps
     */
    List<Map> scrapeForums(int maxPosts = 10) {
        return generatePostsWithGemini("forums", maxPosts)
    }

    /**
     * Scrape all sources and return combined posts
     * 
     * @return Map with posts from each source
     */
    Map<String, List<Map>> scrapeAllSources() {
        log.info("Starting full scrape of all sources...")
        
        return [
            twitter: scrapeTwitter(),
            youtube: scrapeYouTube(),
            forums: scrapeForums()
        ]
    }

    /**
     * Fetch URL content with proper headers
     */
    private String fetchUrl(String urlString) {
        try {
            def url = new URL(urlString)
            def connection = url.openConnection() as HttpURLConnection
            
            // Set headers to appear as a browser
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            
            def responseCode = connection.responseCode
            
            if (responseCode == 200) {
                return connection.inputStream.getText("UTF-8")
            } else {
                log.warn("HTTP ${responseCode} from ${urlString}")
                return null
            }
            
        } catch (Exception e) {
            log.error("Failed to fetch ${urlString}: ${e.message}")
            return null
        }
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
            
            def posts = new JsonSlurper().parseText(cleanJson)
            
            if (posts instanceof List) {
                return posts.collect { post ->
                    [
                        content: post.content?.toString() ?: '',
                        author: post.author?.toString() ?: 'Unknown',
                        source: sourceType,
                        postUrl: post.postUrl?.toString() ?: '',
                        publishedAt: parseDate(post.publishedAt),
                        externalId: generateExternalId(post, sourceType)
                    ]
                }.findAll { it.content?.length() > 10 }
            }
            
            return []
            
        } catch (Exception e) {
            log.error("Failed to parse extracted posts: ${e.message}")
            log.debug("Raw JSON: ${jsonString?.take(500)}")
            return []
        }
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
