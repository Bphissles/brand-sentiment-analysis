# Search Strategy Documentation

## Current Implementation: Approach 1 - Broad Search Without Site Restrictions

**Status:** ✅ Implemented

**Strategy:** Remove site-specific restrictions (e.g., `site:twitter.com`) and let Gemini's Google Search grounding find relevant content across the entire web. This maximizes data collection by not limiting to specific domains.

**Benefits:**
- Casts wider net for data collection
- Finds content from unexpected but relevant sources
- Lets Google's search algorithm determine relevance
- Reduces risk of missing valuable discussions on lesser-known platforms

**Implementation:**
- Search queries focus on intent and topic, not domain
- Gemini filters results based on source type context
- Deduplication handles content from multiple sources

---

## Future Approaches (Not Yet Implemented)

### Approach 4: Competitor Comparisons

**Purpose:** Gather sentiment data through competitive analysis discussions.

**Rationale:** 
- Truck buyers often compare brands before purchasing
- Comparison discussions reveal strengths/weaknesses
- Forums and reviews frequently pit brands against each other
- Provides context for Peterbilt's market position

**Proposed Query Examples:**
```groovy
// Direct comparisons
'Peterbilt vs Freightliner owner experience'
'Peterbilt vs Kenworth reliability comparison'
'Peterbilt vs Volvo fuel economy'
'Peterbilt vs International maintenance costs'
'Peterbilt vs Mack driver comfort'

// Indirect comparisons
'best semi truck brand 2024'
'most reliable heavy duty truck'
'which truck brand has best resale value'
'top rated sleeper cab trucks'
```

**Competitors to Include:**
- Freightliner (Daimler) - Market leader
- Kenworth (PACCAR sibling) - Direct competitor
- Volvo - Premium segment
- International (Navistar) - Mid-market
- Mack - Construction/vocational focus
- Western Star - Specialized applications

**Implementation Notes:**
- Add competitor name to Post model or metadata
- Create comparison-specific cluster categories
- Filter to ensure Peterbilt is mentioned in results
- Track competitive sentiment trends over time

**Estimated Impact:** +30-40% more data, richer competitive insights

---

### Approach 5: Role-Based Queries

**Purpose:** Capture diverse perspectives from different stakeholders in the trucking ecosystem.

**Rationale:**
- Different roles have different priorities and pain points
- Owner-operators care about ROI and maintenance costs
- Fleet managers focus on uptime and total cost of ownership
- Drivers prioritize comfort and ease of operation
- Mechanics see reliability and serviceability issues

**Proposed Query Structure by Role:**

#### Owner-Operators
```groovy
'Peterbilt owner operator review'
'Peterbilt ROI return on investment'
'Peterbilt resale value depreciation'
'cost of owning Peterbilt truck'
'Peterbilt maintenance expenses owner operator'
```

#### Fleet Managers
```groovy
'Peterbilt fleet management experience'
'Peterbilt uptime reliability fleet'
'Peterbilt total cost of ownership TCO'
'Peterbilt dealer support fleet'
'Peterbilt warranty service fleet manager'
```

#### Drivers (Company/Lease)
```groovy
'Peterbilt driver comfort review'
'Peterbilt sleeper cab quality'
'Peterbilt ride quality highway'
'Peterbilt ergonomics driver fatigue'
'best Peterbilt model for long haul'
```

#### Mechanics/Technicians
```groovy
'Peterbilt maintenance issues mechanic'
'Peterbilt common problems repair'
'Peterbilt parts availability cost'
'easiest truck to work on Peterbilt'
'Peterbilt diagnostic system review'
```

#### Safety Managers
```groovy
'Peterbilt safety features review'
'Peterbilt collision avoidance system'
'Peterbilt driver assistance technology'
'Peterbilt safety rating crash test'
```

**Implementation Notes:**
- Add `perspective` or `role` field to Post model
- Use Gemini to classify post author role from content
- Create role-specific insight summaries
- Track sentiment by stakeholder type

**Cluster Categories to Add:**
- Cost of Ownership
- Driver Retention/Satisfaction
- Maintenance Complexity
- Safety & Compliance
- Technology & Innovation

**Estimated Impact:** +50-60% more data, multi-dimensional sentiment analysis

---

## Approach Comparison Matrix

| Approach | Data Volume | Implementation Complexity | Business Value | Priority |
|----------|-------------|--------------------------|----------------|----------|
| 1. Broad Search | High | Low | High | ✅ Current |
| 4. Competitor Comparisons | Medium | Medium | High | 🔜 Next |
| 5. Role-Based Queries | High | High | Very High | 🔮 Future |

---

## Implementation Roadmap

### Phase 1: ✅ Broad Search (Current)
- Remove site restrictions
- Increase query diversity
- Monitor data quality

### Phase 2: Competitor Comparisons (Q1 2025)
- Add competitor tracking
- Create comparison clusters
- Build competitive sentiment dashboard

### Phase 3: Role-Based Queries (Q2 2025)
- Extend Post model with role field
- Train Gemini to classify perspectives
- Build stakeholder-specific insights

---

## Notes & Considerations

### Data Quality vs. Quantity
- Broader searches = more data but potential noise
- Gemini's filtering helps maintain relevance
- Deduplication prevents redundant content
- Monitor false positive rate

### Rate Limiting
- More queries = more API calls
- Current: 2-second delay between queries
- May need to adjust based on Gemini quotas
- Consider query batching for efficiency

### Cost Management
- Gemini API costs scale with query volume
- Monitor token usage per search
- Balance data volume with budget constraints
- Consider caching strategies

### Future Enhancements
- Dynamic query generation based on trending topics
- Temporal analysis (seasonal trends, model year cycles)
- Geographic segmentation (regional preferences)
- Language support (Spanish, French for Canadian market)

---

**Last Updated:** December 26, 2024  
**Owner:** Sentiment Analysis Team  
**Status:** Living Document - Update as strategy evolves
