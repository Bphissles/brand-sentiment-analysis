package sentiment

import java.time.Instant

/**
 * Stores AI-generated insights that are cached until new analysis is run
 * Types: trend_analysis, recommendations, executive_summary
 */
class AiInsight {

    String type                // trend_analysis, recommendations, executive_summary
    String content             // The generated text content
    String source              // Optional: filter by source (all, twitter, youtube, forums)
    
    // Link to the analysis run that generated this insight
    Long analysisRunId
    
    // Metadata
    Integer postsAnalyzed      // Number of posts when this was generated
    Integer clustersCount      // Number of clusters when this was generated
    
    Instant dateCreated
    Instant lastUpdated

    static constraints = {
        type nullable: false, inList: ['trend_analysis', 'recommendations', 'executive_summary']
        content nullable: false, maxSize: 10000
        source nullable: true
        analysisRunId nullable: true
        postsAnalyzed nullable: true
        clustersCount nullable: true
    }

    static mapping = {
        content type: 'text'
    }
}
