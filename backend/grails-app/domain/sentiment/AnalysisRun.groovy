package sentiment

import java.time.Instant

/**
 * Tracks a complete analysis job
 * Records status, counts, and timing for each ML analysis run
 */
class AnalysisRun {

    String status              // pending, processing, completed, failed

    // Counts
    Integer postsAnalyzed = 0
    Integer clustersCreated = 0
    Integer insightsGenerated = 0

    // Timing
    Instant startedAt
    Instant completedAt
    Long durationMs

    // Error tracking
    String error

    Instant dateCreated
    Instant lastUpdated

    static constraints = {
        status nullable: false, inList: ['pending', 'processing', 'completed', 'failed']
        postsAnalyzed nullable: true
        clustersCreated nullable: true
        insightsGenerated nullable: true
        startedAt nullable: true
        completedAt nullable: true
        durationMs nullable: true
        error nullable: true, maxSize: 5000
    }

    static mapping = {
        error type: 'text'
    }
}
