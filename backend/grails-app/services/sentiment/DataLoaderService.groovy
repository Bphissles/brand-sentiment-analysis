package sentiment

import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import java.time.Instant

/**
 * Service for loading fixture data into the database
 * Used for development and testing
 */
@Transactional
class DataLoaderService {

    /**
     * Load all fixture files from data/fixtures directory
     * 
     * @return Map with counts of loaded data
     */
    Map loadAllFixtures() {
        def sources = ['twitter', 'youtube', 'forums']
        def totalPosts = 0
        def loadedSources = [:]

        sources.each { source ->
            def count = loadFixtureFile(source)
            if (count > 0) {
                totalPosts += count
                loadedSources[source] = count
            }
        }

        return [
            postsLoaded: totalPosts,
            sources: loadedSources
        ]
    }

    /**
     * Count available fixture posts without loading them
     * 
     * @return Map with counts per source and total
     */
    Map countFixtures() {
        def sources = ['twitter', 'youtube', 'forums']
        def totalPosts = 0
        def sourceCounts = [:]

        sources.each { source ->
            def count = countFixtureFile(source)
            sourceCounts[source] = count
            totalPosts += count
        }

        return [
            total: totalPosts,
            sources: sourceCounts
        ]
    }

    /**
     * Count posts in a fixture file without loading
     * 
     * @param source The source type (twitter, youtube, forums)
     * @return Number of posts in fixture file
     */
    int countFixtureFile(String source) {
        def file = findFixtureFile(source)
        if (!file) return 0

        try {
            def jsonSlurper = new JsonSlurper()
            def data = jsonSlurper.parse(file)
            def posts = (data instanceof List) ? data : (data.posts ?: [])
            return posts.size()
        } catch (Exception e) {
            log.warn("Failed to count fixtures for ${source}: ${e.message}")
            return 0
        }
    }

    /**
     * Find fixture file for a given source
     * 
     * @param source The source type
     * @return File object or null if not found
     */
    private File findFixtureFile(String source) {
        def fixtureDir = System.getProperty('user.dir')
        def possiblePaths = [
            "${fixtureDir}/../data/fixtures/${source}.json",
            "${fixtureDir}/data/fixtures/${source}.json",
            "${fixtureDir}/../sentiment-analyzer/data/fixtures/${source}.json"
        ]
        
        for (path in possiblePaths) {
            def f = new File(path)
            if (f.exists()) {
                return f
            }
        }
        
        log.warn("Fixture file not found for ${source}. Tried: ${possiblePaths}")
        return null
    }

    /**
     * Load a single fixture file
     * 
     * @param source The source type (twitter, youtube, forums)
     * @return Number of posts loaded
     */
    int loadFixtureFile(String source) {
        def file = findFixtureFile(source)
        if (!file) {
            return 0
        }

        log.info("Loading fixture file: ${file.absolutePath}")
        def jsonSlurper = new JsonSlurper()
        def data = jsonSlurper.parse(file)
        // Support both array format and object with posts property
        def posts = (data instanceof List) ? data : (data.posts ?: [])
        def count = 0

        posts.eachWithIndex { postData, idx ->
            // Generate externalId if not present
            def externalId = postData.externalId ?: postData.id ?: "${source}-fixture-${idx}-${postData.content?.hashCode()?.abs()}"
            
            // Check if post already exists by externalId
            def existing = Post.findByExternalId(externalId)
            if (existing) {
                log.debug("Post already exists: ${externalId}")
                return
            }

            def post = new Post(
                externalId: externalId,
                source: source,
                content: postData.content,
                author: postData.author,
                authorUrl: postData.authorUrl,
                postUrl: postData.postUrl,
                publishedAt: postData.publishedAt ? Instant.parse(postData.publishedAt) : null,
                fetchedAt: Instant.now()
            )

            if (post.validate()) {
                post.save()
                count++
            } else {
                log.warn("Invalid post data: ${post.errors}")
            }
        }

        log.info("Loaded ${count} posts from ${source}")
        return count
    }

    /**
     * Clear all data from database
     */
    void clearAll() {
        Post.executeUpdate('delete from Post')
        Cluster.executeUpdate('delete from Cluster')
        AnalysisRun.executeUpdate('delete from AnalysisRun')
        log.info("Cleared all data")
    }

    /**
     * Clean corrupted post content that contains embedded JSON metadata
     * This fixes posts where the content field includes raw JSON like:
     * "actual content", "author": "name", "postUrl": "..."
     * 
     * @return Number of posts cleaned
     */
    int cleanCorruptedPostContent() {
        def corruptedPosts = Post.findAll {
            content =~ /",\s*"(author|postUrl|publishedAt|sourceSite)"/
        }
        
        if (!corruptedPosts) {
            // Try alternative pattern matching via HQL
            corruptedPosts = Post.executeQuery(
                "from Post p where p.content like '%postUrl%' or p.content like '%sourceSite%'"
            )
        }
        
        def cleanedCount = 0
        
        corruptedPosts.each { post ->
            def originalContent = post.content
            def cleanedContent = cleanJsonFromContent(originalContent)
            
            if (cleanedContent != originalContent && cleanedContent.length() > 10) {
                post.content = cleanedContent
                post.save(flush: true)
                cleanedCount++
                log.debug("Cleaned post ${post.id}: ${cleanedContent.take(50)}...")
            }
        }
        
        log.info("Cleaned ${cleanedCount} corrupted posts")
        return cleanedCount
    }
    
    /**
     * Extract clean content from a string that may contain embedded JSON
     * Handles various patterns of corrupted content with embedded JSON metadata
     */
    private String cleanJsonFromContent(String content) {
        if (!content) return content
        
        def cleaned = content
        
        // Find the first occurrence of JSON field patterns and truncate there
        // These patterns indicate where the actual content ends and JSON metadata begins
        def cutoffPatterns = [
            '", "author"',
            '","author"',
            '", \n    "author"',
            '",\n    "author"',
            '\", \"author\"',
            '\\", \\"author\\"'
        ]
        
        def minCutoff = cleaned.length()
        cutoffPatterns.each { pattern ->
            def idx = cleaned.indexOf(pattern)
            if (idx > 0 && idx < minCutoff) {
                minCutoff = idx
            }
        }
        
        if (minCutoff < cleaned.length()) {
            cleaned = cleaned.substring(0, minCutoff)
        }
        
        // Also check for postUrl, publishedAt, sourceSite patterns
        def additionalPatterns = [
            '", "postUrl"',
            '", "publishedAt"',
            '", "sourceSite"'
        ]
        
        additionalPatterns.each { pattern ->
            def idx = cleaned.indexOf(pattern)
            if (idx > 0) {
                cleaned = cleaned.substring(0, idx)
            }
        }
        
        // Remove trailing escaped quote if present
        if (cleaned.endsWith('\\"')) {
            cleaned = cleaned[0..-3]
        }
        if (cleaned.endsWith('"')) {
            cleaned = cleaned[0..-2]
        }
        
        return cleaned.trim()
    }
}
