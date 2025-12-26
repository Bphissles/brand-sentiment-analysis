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
    },
    'technology_features': {
        'keywords': ['smartlinq', 'infotainment', 'navigation', 'led', 'headlight', 'dashboard',
                     'apu', 'tech', 'technology', 'system', 'display', 'diagnostics', 'alert'],
        'label': 'Technology & Features',
        'description': 'Truck technology, telematics, and modern features'
    },
    'brand_comparison': {
        'keywords': ['kenworth', 'freightliner', 'volvo', 'mack', 'international', 'switch',
                     'compare', 'comparison', 'versus', 'better', 'competition', 'brand'],
        'label': 'Brand Comparison',
        'description': 'Comparisons with competing truck brands and switching decisions'
    },
    'pricing_value': {
        'keywords': ['price', 'cost', 'expensive', 'cheap', 'value', 'worth', 'money',
                     'rebate', 'hvip', 'incentive', 'roi', 'investment', 'budget', 'afford'],
        'label': 'Pricing & Value',
        'description': 'Truck pricing, incentives, and value for money discussions'
    },
    'build_quality': {
        'keywords': ['quality', 'build', 'premium', 'materials', 'craftsmanship', 'weld',
                     'finish', 'paint', 'chrome', 'durable', 'solid', 'construction'],
        'label': 'Build Quality',
        'description': 'Manufacturing quality, materials, and craftsmanship'
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
        return [{'reason': 'insufficient_posts', 'message': f'Need at least {n_clusters} posts for clustering'}], posts
    
    # Build document strings from tokens
    documents = [' '.join(p.get('tokens', [])) for p in posts]
    
    # Filter out empty documents
    valid_indices = [i for i, doc in enumerate(documents) if doc.strip()]
    if len(valid_indices) < n_clusters:
        return [{'reason': 'insufficient_vocabulary', 'message': 'Not enough non-empty tokenized documents for clustering'}], posts
    
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
        return [{'reason': 'insufficient_vocabulary', 'message': 'Not enough unique terms for TF-IDF vectorization'}], posts
    
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
    used_taxonomy_ids = set()  # Track used taxonomy labels to prevent duplicates
    
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
        
        # Match to taxonomy, excluding already-used labels
        taxonomy_id, label = match_cluster_to_taxonomy(keywords, exclude=used_taxonomy_ids)
        used_taxonomy_ids.add(taxonomy_id)
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


def match_cluster_to_taxonomy(keywords: List[str], exclude: set = None) -> Tuple[str, str]:
    """
    Match cluster keywords to predefined taxonomy using weighted scoring
    
    Args:
        keywords: List of cluster keywords
        exclude: Set of taxonomy_ids to exclude (already used)
        
    Returns:
        Tuple of (taxonomy_key, human_readable_label)
    """
    if exclude is None:
        exclude = set()
    
    best_match = None
    best_score = 0
    
    # Normalize keywords for matching
    keywords_lower = [k.lower() for k in keywords]
    keywords_set = set(keywords_lower)
    
    for key, taxonomy in CLUSTER_TAXONOMY.items():
        # Skip already-used taxonomy labels
        if key in exclude:
            continue
            
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
    
    # Default if no strong match found - make unique by adding cluster number
    general_key = 'general'
    counter = 1
    while general_key in exclude:
        general_key = f'general_{counter}'
        counter += 1
    
    # Make label unique too if this isn't the first general cluster
    label = 'General Discussion' if general_key == 'general' else f'General Discussion {counter}'
    return (general_key, label)
