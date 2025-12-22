package sentiment

import java.time.Instant

/**
 * Raw social media/forum post
 * Stores content fetched from public sources for sentiment analysis
 */
class Post {

    String externalId          // Original platform ID
    String source              // twitter, youtube, forums
    String content             // Raw text content
    String author              // Username or handle
    String authorUrl           // Link to author profile
    String postUrl             // Link to original post
    Instant publishedAt        // When originally posted
    Instant fetchedAt          // When we retrieved it

    // ML-generated fields (populated after analysis)
    Double sentimentCompound   // Overall score: -1 to 1
    Double sentimentPositive   // Positive component: 0 to 1
    Double sentimentNegative   // Negative component: 0 to 1
    Double sentimentNeutral    // Neutral component: 0 to 1
    String sentimentLabel      // positive, negative, neutral

    String clusterId           // FK to Cluster
    String keywords            // Comma-separated keywords

    Instant dateCreated
    Instant lastUpdated

    static constraints = {
        externalId nullable: false, blank: false, maxSize: 255
        source nullable: false, inList: ['twitter', 'youtube', 'forums']
        content nullable: false, blank: false, maxSize: 10000
        author nullable: true, maxSize: 255
        authorUrl nullable: true, maxSize: 500
        postUrl nullable: true, maxSize: 500
        publishedAt nullable: true
        fetchedAt nullable: true

        sentimentCompound nullable: true
        sentimentPositive nullable: true
        sentimentNegative nullable: true
        sentimentNeutral nullable: true
        sentimentLabel nullable: true, inList: ['positive', 'negative', 'neutral']

        clusterId nullable: true
        keywords nullable: true, maxSize: 1000
    }

    static mapping = {
        content type: 'text'
        keywords type: 'text'
    }
}
