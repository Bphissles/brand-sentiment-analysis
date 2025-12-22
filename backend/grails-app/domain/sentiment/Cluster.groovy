package sentiment

import java.time.Instant

/**
 * Group of related posts clustered by topic
 * Maps to taxonomy categories defined in config/taxonomy.yaml
 */
class Cluster {

    String taxonomyId          // Maps to taxonomy.yaml cluster id
    String label               // Human-readable label
    String description         // Business context

    // Aggregated data
    String keywords            // Comma-separated top keywords
    Double sentiment           // Average sentiment: -1 to 1
    String sentimentLabel      // positive, negative, neutral
    Integer postCount          // Number of posts in cluster

    // AI-generated insight
    String insight             // Gemini-generated business insight

    // Tracking
    String analysisRunId       // Which analysis run created this
    Instant dateCreated
    Instant lastUpdated

    static constraints = {
        taxonomyId nullable: false, blank: false, maxSize: 100
        label nullable: false, blank: false, maxSize: 255
        description nullable: true, maxSize: 1000

        keywords nullable: true, maxSize: 2000
        sentiment nullable: true
        sentimentLabel nullable: true, inList: ['positive', 'negative', 'neutral']
        postCount nullable: true

        insight nullable: true, maxSize: 5000
        analysisRunId nullable: true, maxSize: 100
    }

    static mapping = {
        keywords type: 'text'
        insight type: 'text'
    }
}
