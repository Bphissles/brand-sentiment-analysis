"""
Clustering module for topic discovery
Implements K-Means with TF-IDF for grouping posts by theme
"""
from typing import List, Dict, Tuple
from collections import Counter
import uuid
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.cluster import KMeans

# Peterbilt-specific cluster taxonomy for labeling
CLUSTER_TAXONOMY = {
    'ev_adoption': {
        'keywords': ['579ev', 'ev', 'electric', 'battery', 'charging', 'range', 
                     'zero', 'emission', '350kw', 'charger', 'kwh', 'bev'],
        'label': 'EV Adoption',
        'description': 'Electric vehicle adoption, charging infrastructure, and range concerns'
    },
    'driver_comfort': {
        'keywords': ['sleeper', 'interior', 'ergonomic', 'seat', 'comfort', '80', 
                     'inch', 'platinum', 'cab', 'mattress', 'space', 'loft', 'ultraloft'],
        'label': 'Driver Comfort',
        'description': 'Cab interior, sleeper features, and driver ergonomics'
    },
    'uptime_reliability': {
        'keywords': ['engine', 'service', 'dealer', 'breakdown', 'repair', 'uptime',
                     'reliable', 'reliability', 'miles', 'maintenance', 'derate', 'shop'],
        'label': 'Uptime & Reliability',
        'description': 'Mechanical reliability, service quality, and dealer experience'
    },
    'model_demand': {
        'keywords': ['589', '579', '567', '389', 'wait', 'order', 'backlog', 'delivery', 
                     'availability', 'new', 'model', 'spec', 'ordered', 'months'],
        'label': 'Model Demand',
        'description': 'Model availability, wait times, and ordering experience'
    }
}


def cluster_posts(posts: List[dict], n_clusters: int = 4) -> Tuple[List[Dict], List[dict]]:
    """
    Cluster posts into topic groups using K-Means with TF-IDF
    
    Args:
        posts: List of preprocessed posts with 'tokens' field
        n_clusters: Number of clusters to create
        
    Returns:
        Tuple of (clusters list, posts with cluster assignments)
    """
    if not posts or len(posts) < n_clusters:
        # Not enough posts to cluster meaningfully
        return [], posts
    
    # Build document strings from tokens
    documents = [' '.join(p.get('tokens', [])) for p in posts]
    
    # Filter out empty documents
    valid_indices = [i for i, doc in enumerate(documents) if doc.strip()]
    if len(valid_indices) < n_clusters:
        return [], posts
    
    valid_documents = [documents[i] for i in valid_indices]
    
    # Create TF-IDF matrix
    vectorizer = TfidfVectorizer(
        max_features=500,
        min_df=1,
        max_df=0.95,
        ngram_range=(1, 2)
    )
    
    try:
        tfidf_matrix = vectorizer.fit_transform(valid_documents)
    except ValueError:
        # Not enough vocabulary
        return [], posts
    
    # Run K-Means clustering
    kmeans = KMeans(
        n_clusters=n_clusters,
        random_state=42,
        n_init=10,
        max_iter=300
    )
    cluster_labels = kmeans.fit_predict(tfidf_matrix)
    
    # Get feature names for keyword extraction
    feature_names = vectorizer.get_feature_names_out()
    
    # Assign cluster labels back to posts
    updated_posts = posts.copy()
    for idx, valid_idx in enumerate(valid_indices):
        updated_posts[valid_idx] = {
            **updated_posts[valid_idx],
            'clusterIdx': int(cluster_labels[idx])
        }
    
    # Build cluster objects
    clusters = []
    for cluster_idx in range(n_clusters):
        # Get posts in this cluster
        cluster_post_indices = [valid_indices[i] for i, label in enumerate(cluster_labels) if label == cluster_idx]
        cluster_posts_list = [updated_posts[i] for i in cluster_post_indices]
        
        if not cluster_posts_list:
            continue
        
        # Extract keywords from cluster centroid
        centroid = kmeans.cluster_centers_[cluster_idx]
        top_indices = centroid.argsort()[-15:][::-1]
        keywords = [feature_names[i] for i in top_indices]
        
        # Match to taxonomy
        taxonomy_id, label = match_cluster_to_taxonomy(keywords)
        description = CLUSTER_TAXONOMY.get(taxonomy_id, {}).get('description', '')
        
        # Create cluster object (using camelCase for consistency with backend)
        cluster = {
            'id': str(uuid.uuid4()),
            'taxonomyId': taxonomy_id,
            'label': label,
            'description': description,
            'keywords': keywords[:10],
            'postCount': len(cluster_posts_list),
            'postIds': [p.get('id') for p in cluster_posts_list]
        }
        
        clusters.append(cluster)
        
        # Update posts with cluster ID
        for post_idx in cluster_post_indices:
            updated_posts[post_idx]['clusterId'] = cluster['id']
    
    return clusters, updated_posts


def extract_cluster_keywords(cluster_posts: List[dict], top_n: int = 10) -> List[str]:
    """
    Extract top keywords from a cluster of posts using term frequency
    
    Args:
        cluster_posts: Posts belonging to a single cluster
        top_n: Number of top keywords to return
        
    Returns:
        List of top keywords
    """
    if not cluster_posts:
        return []
    
    # Collect all tokens
    all_tokens = []
    for post in cluster_posts:
        all_tokens.extend(post.get('tokens', []))
    
    # Count frequencies
    token_counts = Counter(all_tokens)
    
    # Return top N
    return [token for token, count in token_counts.most_common(top_n)]


def match_cluster_to_taxonomy(keywords: List[str]) -> Tuple[str, str]:
    """
    Match cluster keywords to predefined taxonomy using weighted scoring
    
    Args:
        keywords: List of cluster keywords
        
    Returns:
        Tuple of (taxonomy_key, human_readable_label)
    """
    best_match = None
    best_score = 0
    
    # Normalize keywords for matching
    keywords_lower = [k.lower() for k in keywords]
    keywords_set = set(keywords_lower)
    
    for key, taxonomy in CLUSTER_TAXONOMY.items():
        taxonomy_keywords = set(k.lower() for k in taxonomy['keywords'])
        
        # Score based on overlap, weighted by position in keyword list
        score = 0
        for i, kw in enumerate(keywords_lower):
            # Check for exact match or partial match
            for tax_kw in taxonomy_keywords:
                if tax_kw in kw or kw in tax_kw:
                    # Higher weight for earlier keywords (more important)
                    score += (len(keywords) - i) / len(keywords)
                    break
        
        if score > best_score:
            best_score = score
            best_match = (key, taxonomy['label'])
    
    if best_match and best_score > 0.5:
        return best_match
    
    # Default if no strong match found
    return ('general', 'General Discussion')
