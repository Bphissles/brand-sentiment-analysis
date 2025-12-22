"""
Clustering module for topic discovery
Implements K-Means and LDA for grouping posts by theme
"""
from typing import List, Dict, Tuple


# Peterbilt-specific cluster taxonomy for labeling
CLUSTER_TAXONOMY = {
    'ev_adoption': {
        'keywords': ['579ev', 'ev', 'electric', 'battery', 'charging', 'range', 'zero emission', '350kw'],
        'label': 'EV Adoption'
    },
    'driver_comfort': {
        'keywords': ['sleeper', 'interior', 'ergonomic', 'seat', 'comfort', '80 inch', 'platinum', 'cab'],
        'label': 'Driver Comfort'
    },
    'uptime_reliability': {
        'keywords': ['paccar', 'powertrain', 'engine', 'service', 'dealer', 'breakdown', 'repair', 'uptime'],
        'label': 'Uptime & Reliability'
    },
    'model_demand': {
        'keywords': ['589', '579', 'wait', 'order', 'backlog', 'delivery', 'availability', 'new model'],
        'label': 'Model Demand'
    }
}


def cluster_posts(posts: List[dict], n_clusters: int = 4) -> List[Dict]:
    """
    Cluster posts into topic groups using K-Means
    
    Args:
        posts: List of preprocessed posts with 'tokens' field
        n_clusters: Number of clusters to create
        
    Returns:
        List of cluster dictionaries with labels, keywords, and post assignments
    """
    # TODO: Implement in Sprint 2
    # 1. Create TF-IDF matrix from post tokens
    # 2. Run K-Means clustering
    # 3. Extract top keywords per cluster
    # 4. Match to taxonomy for labeling
    # 5. Return cluster assignments
    
    return []


def extract_cluster_keywords(cluster_posts: List[dict], top_n: int = 10) -> List[str]:
    """
    Extract top keywords from a cluster of posts
    
    Args:
        cluster_posts: Posts belonging to a single cluster
        top_n: Number of top keywords to return
        
    Returns:
        List of top keywords
    """
    # TODO: Implement in Sprint 2
    # Count word frequencies across cluster posts
    # Return top N most frequent meaningful words
    
    return []


def match_cluster_to_taxonomy(keywords: List[str]) -> Tuple[str, str]:
    """
    Match cluster keywords to predefined taxonomy
    
    Args:
        keywords: List of cluster keywords
        
    Returns:
        Tuple of (taxonomy_key, human_readable_label)
    """
    best_match = None
    best_score = 0
    
    for key, taxonomy in CLUSTER_TAXONOMY.items():
        # Count keyword overlaps
        overlap = len(set(keywords) & set(taxonomy['keywords']))
        if overlap > best_score:
            best_score = overlap
            best_match = (key, taxonomy['label'])
    
    if best_match:
        return best_match
    
    # Default if no match found
    return ('general', 'General Discussion')
