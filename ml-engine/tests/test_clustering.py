"""
Unit tests for clustering module
Tests K-Means clustering and taxonomy matching
"""
import pytest
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent / 'app'))

from clustering import cluster_posts, match_cluster_to_taxonomy, CLUSTER_TAXONOMY


def test_cluster_posts_returns_clusters_and_posts():
    """Test that clustering returns both clusters and updated posts"""
    posts = [
        {"id": "1", "content": "579EV electric truck", "tokens": ["579ev", "electric", "truck"]},
        {"id": "2", "content": "PACCAR engine reliable", "tokens": ["paccar", "engine", "reliable"]},
        {"id": "3", "content": "sleeper cab comfort", "tokens": ["sleeper", "cab", "comfort"]},
        {"id": "4", "content": "589 model availability", "tokens": ["589", "model", "availability"]}
    ]
    
    clusters, updated_posts = cluster_posts(posts, n_clusters=3)
    
    assert isinstance(clusters, list)
    assert isinstance(updated_posts, list)
    assert len(updated_posts) == len(posts)


def test_cluster_posts_assigns_cluster_ids():
    """Test that posts are assigned cluster IDs"""
    posts = [
        {"id": str(i), "content": f"Post {i}", "tokens": [f"word{i}", "test"]}
        for i in range(10)
    ]
    
    clusters, updated_posts = cluster_posts(posts, n_clusters=3)
    
    # Check that posts have cluster IDs
    for post in updated_posts:
        if "clusterId" in post:
            assert post["clusterId"] is not None


def test_cluster_posts_structure():
    """Test that cluster objects have correct structure"""
    posts = [
        {"id": "1", "tokens": ["electric", "battery", "charging"]},
        {"id": "2", "tokens": ["sleeper", "comfort", "interior"]},
        {"id": "3", "tokens": ["engine", "reliable", "service"]},
        {"id": "4", "tokens": ["model", "order", "availability"]}
    ]
    
    clusters, _ = cluster_posts(posts, n_clusters=2)
    
    if len(clusters) > 0:
        cluster = clusters[0]
        assert "id" in cluster
        assert "taxonomyId" in cluster
        assert "label" in cluster
        assert "description" in cluster
        assert "keywords" in cluster
        assert "postCount" in cluster
        assert "postIds" in cluster


def test_cluster_posts_handles_small_dataset():
    """Test clustering with fewer posts than clusters"""
    posts = [
        {"id": "1", "tokens": ["test"]},
        {"id": "2", "tokens": ["example"]}
    ]
    
    clusters, updated_posts = cluster_posts(posts, n_clusters=5)
    
    # Should handle gracefully
    assert isinstance(clusters, list)
    assert isinstance(updated_posts, list)


def test_cluster_posts_handles_empty_list():
    """Test that empty post list is handled"""
    posts = []
    clusters, updated_posts = cluster_posts(posts, n_clusters=3)
    
    assert clusters == []
    assert updated_posts == []


def test_cluster_posts_keywords_extracted():
    """Test that keywords are extracted for each cluster"""
    posts = [
        {"id": str(i), "tokens": ["electric", "battery", "ev", "charging"]}
        for i in range(5)
    ]
    
    clusters, _ = cluster_posts(posts, n_clusters=1)
    
    if len(clusters) > 0:
        assert len(clusters[0]["keywords"]) > 0
        # Should contain some of the common words
        keywords_str = " ".join(clusters[0]["keywords"]).lower()
        assert any(word in keywords_str for word in ["electric", "battery", "ev", "charging"])


def test_match_cluster_to_taxonomy_ev_adoption():
    """Test taxonomy matching for EV-related keywords"""
    keywords = ["579ev", "electric", "battery", "charging", "range"]
    
    taxonomy_id, label = match_cluster_to_taxonomy(keywords)
    
    assert taxonomy_id == "ev_adoption"
    assert label == "EV Adoption"


def test_match_cluster_to_taxonomy_driver_comfort():
    """Test taxonomy matching for comfort-related keywords"""
    keywords = ["sleeper", "interior", "comfort", "cab", "ergonomic"]
    
    taxonomy_id, label = match_cluster_to_taxonomy(keywords)
    
    assert taxonomy_id == "driver_comfort"
    assert label == "Driver Comfort"


def test_match_cluster_to_taxonomy_uptime_reliability():
    """Test taxonomy matching for reliability keywords"""
    keywords = ["engine", "service", "reliable", "maintenance", "dealer"]
    
    taxonomy_id, label = match_cluster_to_taxonomy(keywords)
    
    assert taxonomy_id == "uptime_reliability"
    assert label == "Uptime & Reliability"


def test_match_cluster_to_taxonomy_model_demand():
    """Test taxonomy matching for model demand keywords"""
    keywords = ["589", "579", "order", "availability", "wait"]
    
    taxonomy_id, label = match_cluster_to_taxonomy(keywords)
    
    assert taxonomy_id == "model_demand"
    assert label == "Model Demand"


def test_match_cluster_to_taxonomy_no_match():
    """Test taxonomy matching with unrelated keywords"""
    keywords = ["random", "unrelated", "words", "here"]
    
    taxonomy_id, label = match_cluster_to_taxonomy(keywords)
    
    # Should return default
    assert taxonomy_id == "general"
    assert label == "General Discussion"


def test_match_cluster_to_taxonomy_empty_keywords():
    """Test taxonomy matching with empty keywords"""
    keywords = []
    
    taxonomy_id, label = match_cluster_to_taxonomy(keywords)
    
    assert taxonomy_id == "general"
    assert label == "General Discussion"


def test_cluster_taxonomy_structure():
    """Test that CLUSTER_TAXONOMY has correct structure"""
    assert "ev_adoption" in CLUSTER_TAXONOMY
    assert "driver_comfort" in CLUSTER_TAXONOMY
    assert "uptime_reliability" in CLUSTER_TAXONOMY
    assert "model_demand" in CLUSTER_TAXONOMY
    
    for key, taxonomy in CLUSTER_TAXONOMY.items():
        assert "keywords" in taxonomy
        assert "label" in taxonomy
        assert "description" in taxonomy
        assert isinstance(taxonomy["keywords"], list)
        assert len(taxonomy["keywords"]) > 0


def test_cluster_posts_post_count_accuracy():
    """Test that postCount matches actual number of posts"""
    posts = [
        {"id": str(i), "tokens": ["test", "word", f"unique{i % 2}"]}
        for i in range(10)
    ]
    
    clusters, updated_posts = cluster_posts(posts, n_clusters=2)
    
    # Count posts per cluster
    for cluster in clusters:
        post_ids = set(cluster["postIds"])
        assert cluster["postCount"] == len(post_ids)
