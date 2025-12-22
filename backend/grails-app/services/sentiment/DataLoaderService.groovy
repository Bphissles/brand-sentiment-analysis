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
        def loadedSources = []

        sources.each { source ->
            def count = loadFixtureFile(source)
            if (count > 0) {
                totalPosts += count
                loadedSources << [source: source, count: count]
            }
        }

        return [
            postsLoaded: totalPosts,
            sources: loadedSources
        ]
    }

    /**
     * Load a single fixture file
     * 
     * @param source The source type (twitter, youtube, forums)
     * @return Number of posts loaded
     */
    int loadFixtureFile(String source) {
        def fixtureDir = System.getProperty('user.dir')
        def filePath = "${fixtureDir}/../data/fixtures/${source}.json"
        def file = new File(filePath)

        if (!file.exists()) {
            log.warn("Fixture file not found: ${filePath}")
            return 0
        }

        def jsonSlurper = new JsonSlurper()
        def data = jsonSlurper.parse(file)
        def posts = data.posts ?: []
        def count = 0

        posts.each { postData ->
            // Check if post already exists by externalId
            def existing = Post.findByExternalId(postData.externalId ?: postData.id)
            if (existing) {
                log.debug("Post already exists: ${postData.externalId}")
                return
            }

            def post = new Post(
                externalId: postData.externalId ?: postData.id,
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
}
