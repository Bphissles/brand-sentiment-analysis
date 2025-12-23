package sentiment

import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/**
 * Service for communicating with the Python ML Engine
 * Handles clustering and sentiment analysis requests
 */
@Transactional
class MlEngineService {

    // Injected from application.yml
    def grailsApplication

    /**
     * Send posts to ML engine for clustering and sentiment analysis
     * 
     * @param posts List of Post domain objects to analyze
     * @return Analysis results with clusters
     */
    Map analyzePostsForClusters(List<Post> posts) {
        def mlEngineUrl = grailsApplication.config.getProperty('mlEngine.url', String, 'http://localhost:5000')
        
        def postData = posts.collect { post ->
            [
                id: post.id.toString(),
                content: post.content,
                source: post.source,
                author: post.author,
                publishedAt: post.publishedAt?.toString()
            ]
        }

        try {
            def url = new URL("${mlEngineUrl}/api/analyze")
            def connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 120000  // ML processing can take time

            def requestBody = JsonOutput.toJson([posts: postData])

            connection.outputStream.withWriter("UTF-8") { writer ->
                writer.write(requestBody)
            }

            def responseCode = connection.responseCode
            
            if (responseCode == 200) {
                def response = new JsonSlurper().parseText(connection.inputStream.text)
                return [
                    success: true,
                    clusters: response.clusters ?: [],
                    posts: response.posts ?: [],
                    postsAnalyzed: response.postsAnalyzed ?: posts.size(),
                    processingTimeMs: response.processingTimeMs ?: 0
                ]
            } else {
                def errorBody = connection.errorStream?.text ?: "No error details"
                log.error("ML Engine error: ${responseCode} - ${errorBody}")
                return [
                    success: false,
                    error: "ML Engine returned ${responseCode}: ${errorBody}"
                ]
            }
            
        } catch (ConnectException e) {
            log.error("Cannot connect to ML Engine at ${mlEngineUrl}", e)
            return [
                success: false,
                error: "Cannot connect to ML Engine. Is it running?"
            ]
        } catch (Exception e) {
            log.error("Failed to call ML Engine", e)
            return [
                success: false,
                error: e.message
            ]
        }
    }

    /**
     * Check if ML Engine is healthy
     * 
     * @return true if ML Engine is responding
     */
    boolean isHealthy() {
        def mlEngineUrl = grailsApplication.config.getProperty('mlEngine.url', String, 'http://localhost:5000')
        
        try {
            def url = new URL("${mlEngineUrl}/health")
            def connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            return connection.responseCode == 200
        } catch (Exception e) {
            log.warn("ML Engine health check failed: ${e.message}")
            return false
        }
    }
}
