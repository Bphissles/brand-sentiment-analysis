package sentiment

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification
import java.time.Instant

/**
 * Unit tests for PostController
 * Tests CRUD operations and filtering
 */
class PostControllerSpec extends Specification implements ControllerUnitTest<PostController> {

    def setup() {
        mockDomain(Post)
    }

    void "test index returns all posts"() {
        given: "some posts exist"
        new Post(
            externalId: "1",
            source: "twitter",
            content: "Test post",
            author: "user1",
            publishedAt: Instant.now(),
            fetchedAt: Instant.now()
        ).save()

        when: "index is called"
        controller.index()

        then: "posts are returned"
        response.status == 200
        response.json.data.size() == 1
        response.json.total == 1
    }

    void "test index with source filter"() {
        given: "posts from different sources"
        new Post(
            externalId: "1",
            source: "twitter",
            content: "Twitter post",
            author: "user1",
            publishedAt: Instant.now(),
            fetchedAt: Instant.now()
        ).save()
        
        new Post(
            externalId: "2",
            source: "youtube",
            content: "YouTube comment",
            author: "user2",
            publishedAt: Instant.now(),
            fetchedAt: Instant.now()
        ).save()

        when: "filtered by source"
        params.source = "twitter"
        controller.index()

        then: "only twitter posts returned"
        response.status == 200
        response.json.data.size() == 1
        response.json.total == 1
        response.json.data[0].source == "twitter"
    }

    void "test show with valid id"() {
        given: "a post exists"
        def post = new Post(
            externalId: "1",
            source: "twitter",
            content: "Test post",
            author: "user1",
            publishedAt: Instant.now(),
            fetchedAt: Instant.now()
        ).save()

        when: "show is called"
        controller.show(post.id)

        then: "post is returned"
        response.status == 200
        response.json.content == "Test post"
    }

    void "test show with invalid id"() {
        when: "show is called with non-existent id"
        controller.show(999L)

        then: "404 is returned"
        response.status == 404
        response.json.error == "Post not found"
    }

    void "test save with valid data"() {
        given: "valid post data"
        request.json = [
            externalId: "123",
            source: "twitter",
            content: "New post",
            author: "testuser",
            publishedAt: Instant.now().toString()
        ]

        when: "save is called"
        controller.save()

        then: "post is created"
        response.status == 201
        Post.count() == 1
    }

    void "test delete with valid id"() {
        given: "a post exists"
        def post = new Post(
            externalId: "1",
            source: "twitter",
            content: "Test post",
            author: "user1",
            publishedAt: Instant.now(),
            fetchedAt: Instant.now()
        ).save()

        when: "delete is called"
        controller.delete(post.id)

        then: "post is deleted"
        response.status == 204
        Post.count() == 0
    }

    void "test sources returns source counts"() {
        given: "posts from different sources"
        new Post(
            externalId: "1",
            source: "twitter",
            content: "Post 1",
            author: "user1",
            publishedAt: Instant.now(),
            fetchedAt: Instant.now()
        ).save()
        
        new Post(
            externalId: "2",
            source: "twitter",
            content: "Post 2",
            author: "user2",
            publishedAt: Instant.now(),
            fetchedAt: Instant.now()
        ).save()
        
        new Post(
            externalId: "3",
            source: "youtube",
            content: "Post 3",
            author: "user3",
            publishedAt: Instant.now(),
            fetchedAt: Instant.now()
        ).save()

        when: "sources is called"
        controller.sources()

        then: "source counts are returned"
        response.status == 200
        response.json.sources.size() == 2
        response.json.sources.find { it.source == 'twitter' }.count == 2
        response.json.sources.find { it.source == 'youtube' }.count == 1
    }
}
