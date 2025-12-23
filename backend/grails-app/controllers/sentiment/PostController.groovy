package sentiment

import grails.converters.JSON
import grails.gorm.transactions.Transactional

/**
 * REST controller for Post operations
 * Handles CRUD and listing of social media posts
 */
class PostController {

    static responseFormats = ['json']
    static allowedMethods = [index: 'GET', show: 'GET', save: 'POST', delete: 'DELETE']

    /**
     * GET /api/posts
     * List all posts with optional filtering
     */
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
    def show(Long id) {
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
    @Transactional
    def delete(Long id) {
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
