package sentiment

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * REST controller for Post operations
 * Handles CRUD and listing of social media posts
 */
@Tag(name = "Posts", description = "Social media post management")
class PostController {

    static responseFormats = ['json']
    static allowedMethods = [index: 'GET', show: 'GET', save: 'POST', delete: 'DELETE']

    /**
     * GET /api/posts
     * List all posts with optional filtering
     */
    @Operation(
        summary = "List posts",
        description = "List all posts with optional filtering by source, cluster, or sentiment"
    )
    @ApiResponse(responseCode = "200", description = "Paginated list of posts")
    def index() {
        def source = params.source
        def clusterId = params.clusterId
        def sentimentLabel = params.sentimentLabel
        def max = params.int('max', 100)
        def offset = params.int('offset', 0)

        def criteria = Post.createCriteria()
        def posts = criteria.list(max: max, offset: offset) {
            if (source) {
                eq('source', source)
            }
            if (clusterId) {
                eq('clusterId', clusterId)
            }
            if (sentimentLabel) {
                eq('sentimentLabel', sentimentLabel)
            }
            order('publishedAt', 'desc')
        }

        // Calculate total based on the same filters
        def total = Post.createCriteria().count {
            if (source) {
                eq('source', source)
            }
            if (clusterId) {
                eq('clusterId', clusterId)
            }
            if (sentimentLabel) {
                eq('sentimentLabel', sentimentLabel)
            }
        }

        respond([
            data: posts,
            total: total,
            page: (offset / max) + 1,
            pageSize: max,
            hasMore: (offset + max) < total
        ])
    }

    /**
     * GET /api/posts/{id}
     * Get a single post by ID
     */
    @Operation(summary = "Get post", description = "Get a single post by ID")
    @ApiResponse(responseCode = "200", description = "Post details")
    @ApiResponse(responseCode = "404", description = "Post not found")
    def show(@Parameter(description = "Post ID") Long id) {
        def post = Post.get(id)
        if (!post) {
            render status: 404, text: [error: 'Post not found'] as JSON
            return
        }
        respond post
    }

    /**
     * POST /api/posts
     * Create a new post
     */
    @Operation(summary = "Create post", description = "Create a new social media post")
    @ApiResponse(responseCode = "201", description = "Post created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @Transactional
    def save() {
        def data = request.JSON

        def post = new Post(
            externalId: data.externalId,
            source: data.source,
            content: data.content,
            author: data.author,
            authorUrl: data.authorUrl,
            postUrl: data.postUrl,
            publishedAt: data.publishedAt ? java.time.Instant.parse(data.publishedAt) : null,
            fetchedAt: java.time.Instant.now()
        )

        if (!post.validate()) {
            render status: 400, text: [errors: post.errors.allErrors.collect { it.defaultMessage }] as JSON
            return
        }

        post.save(flush: true)
        respond post, status: 201
    }

    /**
     * DELETE /api/posts/{id}
     * Delete a post
     */
    @Operation(summary = "Delete post", description = "Delete a post by ID")
    @ApiResponse(responseCode = "204", description = "Post deleted")
    @ApiResponse(responseCode = "404", description = "Post not found")
    @Transactional
    def delete(@Parameter(description = "Post ID") Long id) {
        def post = Post.get(id)
        if (!post) {
            render status: 404, text: [error: 'Post not found'] as JSON
            return
        }

        post.delete(flush: true)
        render status: 204
    }

    /**
     * GET /api/posts/sources
     * Get available source types with counts
     */
    @Operation(summary = "List sources", description = "Get available source types with post counts")
    @ApiResponse(responseCode = "200", description = "List of sources with counts")
    def sources() {
        def results = Post.executeQuery(
            'select p.source, count(p) from Post p group by p.source'
        )

        def sources = results.collect { row ->
            [source: row[0], count: row[1]]
        }

        respond([sources: sources])
    }
}
