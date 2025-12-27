package sentiment

import grails.gorm.transactions.Transactional
import java.time.Instant

/**
 * Service for data ingestion operations
 * Handles transaction boundaries for scraping and import operations
 */
class IngestionService {

    WebScraperService webScraperService

    /**
     * Scrape posts from a source
     * No transaction - network I/O operation
     */
    List<Map> scrapeSource(String source) {
        switch (source) {
            case 'twitter':
                return webScraperService.scrapeTwitter()
            case 'youtube':
                return webScraperService.scrapeYouTube()
            case 'forums':
                return webScraperService.scrapeForums()
            case 'reddit':
                return webScraperService.scrapeReddit()
            case 'news':
                return webScraperService.scrapeNews()
            default:
                return []
        }
    }

    /**
     * Import posts to database in batches
     * Short transactions with batch processing
     */
    @Transactional
    Map importPosts(List<Map> posts) {
        def imported = 0
        def skipped = 0
        def batchSize = 50
        
        posts.eachWithIndex { postData, idx ->
            // Generate externalId if missing (use content hash as fallback)
            def externalId = postData.externalId ?: postData.id ?: 
                "gen_${postData.content?.hashCode()?.abs()}_${System.currentTimeMillis()}"
            
            // Check for duplicates by externalId
            def existingPost = Post.findByExternalId(externalId)
            if (existingPost) {
                skipped++
                return
            }

            // Parse publishedAt - handle string, epoch, Instant, or null
            def publishedAt = parsePublishedAt(postData.publishedAt)

            // Create new post
            def post = new Post(
                content: postData.content,
                author: postData.author,
                source: postData.source,
                postUrl: postData.postUrl,
                publishedAt: publishedAt,
                externalId: externalId
            )
            
            if (post.validate()) {
                post.save()
                imported++
                
                // Flush and clear every N records to manage memory
                if ((idx + 1) % batchSize == 0) {
                    post.discard()
                }
            }
        }
        
        return [imported: imported, skipped: skipped, total: posts.size()]
    }

    /**
     * Scrape and import from a single source
     * Separates network I/O from database operations
     */
    Map scrapeAndImport(String source) {
        // Step 1: Scrape (no transaction - network I/O)
        def posts = scrapeSource(source)
        
        // Step 2: Import (short transaction with batching)
        def result = importPosts(posts)
        
        return [
            source: source,
            posts: posts.size(),
            imported: result.imported,
            skipped: result.skipped
        ]
    }

    /**
     * Scrape and import from all sources
     * Processes each source independently
     */
    Map scrapeAndImportAll() {
        def sources = ['twitter', 'youtube', 'forums', 'reddit', 'news']
        def results = [:]
        def imported = [:]
        def errors = []

        sources.each { source ->
            try {
                def result = scrapeAndImport(source)
                results[source] = result.posts
                imported[source] = result.imported
            } catch (Exception e) {
                errors << [source: source, error: e.message]
            }
        }

        return [
            results: results,
            imported: imported,
            errors: errors
        ]
    }

    /**
     * Parse publishedAt field from various formats
     * Handles: ISO 8601 strings, epoch milliseconds, Instant, or null
     */
    private Instant parsePublishedAt(def value) {
        if (value == null) {
            return null
        }
        if (value instanceof Instant) {
            return value
        }
        if (value instanceof Number) {
            // Assume epoch milliseconds
            return Instant.ofEpochMilli(value.longValue())
        }
        if (value instanceof String) {
            try {
                return Instant.parse(value)
            } catch (Exception e) {
                // Try parsing as epoch string
                try {
                    return Instant.ofEpochMilli(Long.parseLong(value))
                } catch (Exception e2) {
                    return null
                }
            }
        }
        return null
    }
}
