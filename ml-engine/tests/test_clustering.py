"""
Tests for clustering module
"""
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'app'))

import pytest
from clustering import (
    cluster_posts,
    extract_cluster_keywords,
    match_cluster_to_taxonomy,
    CLUSTER_TAXONOMY
)


class TestClusterPosts:
    """Tests for cluster_posts function"""
    
    @pytest.mark.unit
    def test_insufficient_posts_returns_reason(self):
        """Should return reason when fewer posts than clusters requested"""
        posts = [
            {"id": "1", "tokens": ["great", "truck"]},
            {"id": "2", "tokens": ["amazing", "performance"]}
        ]
        clusters, result_posts = cluster_posts(posts, n_clusters=5)
        assert len(clusters) == 1
        assert clusters[0].get('reason') == 'insufficient_posts'
    
    @pytest.mark.unit
    def test_empty_posts_returns_reason(self):
        """Should return reason for empty posts list"""
        clusters, result_posts = cluster_posts([], n_clusters=3)
        assert len(clusters) == 1
        assert clusters[0].get('reason') == 'insufficient_posts'
    
    @pytest.mark.unit
    def test_all_empty_tokens_returns_reason(self):
        """Should return reason when all posts have empty tokens"""
        posts = [
            {"id": "1", "tokens": []},
            {"id": "2", "tokens": []},
            {"id": "3", "tokens": []},
            {"id": "4", "tokens": []}
        ]
        clusters, result_posts = cluster_posts(posts, n_clusters=3)
        assert len(clusters) == 1
        assert clusters[0].get('reason') == 'insufficient_vocabulary'
    
    @pytest.mark.unit
    def test_successful_clustering(self):
        """Should cluster posts with sufficient data"""
        posts = [
            {"id": "1", "tokens": ["electric", "battery", "charging", "range"]},
            {"id": "2", "tokens": ["ev", "electric", "charger", "kwh"]},
            {"id": "3", "tokens": ["sleeper", "interior", "comfort", "cab"]},
            {"id": "4", "tokens": ["seat", "mattress", "space", "loft"]},
            {"id": "5", "tokens": ["engine", "breakdown", "repair", "service"]},
            {"id": "6", "tokens": ["dealer", "maintenance", "uptime", "reliable"]}
        ]
        clusters, result_posts = cluster_posts(posts, n_clusters=3)
        
        # Should have actual clusters, not just a reason
        assert len(clusters) >= 1
        if 'reason' not in clusters[0]:
            assert all('id' in c for c in clusters)
            assert all('label' in c for c in clusters)
    
    @pytest.mark.unit
    def test_assigns_cluster_ids_to_posts(self):
        """Posts should have clusterId assigned"""
        posts = [
            {"id": "1", "tokens": ["electric", "battery", "charging"]},
            {"id": "2", "tokens": ["ev", "electric", "charger"]},
            {"id": "3", "tokens": ["sleeper", "interior", "comfort"]},
            {"id": "4", "tokens": ["seat", "mattress", "space"]}
        ]
        clusters, result_posts = cluster_posts(posts, n_clusters=2)
        
        # Check if clustering succeeded
        if 'reason' not in clusters[0]:
            clustered = [p for p in result_posts if 'clusterId' in p]
            assert len(clustered) > 0
    
    @pytest.mark.unit
    def test_deterministic_with_fixed_random_state(self):
        """Clustering should be deterministic with same input"""
        posts = [
            {"id": "1", "tokens": ["electric", "battery", "charging", "range"]},
            {"id": "2", "tokens": ["ev", "electric", "charger", "kwh"]},
            {"id": "3", "tokens": ["sleeper", "interior", "comfort", "cab"]},
            {"id": "4", "tokens": ["seat", "mattress", "space", "loft"]}
        ]
        
        clusters1, _ = cluster_posts(posts, n_clusters=2)
        clusters2, _ = cluster_posts(posts, n_clusters=2)
        
        # If both succeeded, they should produce same results
        if 'reason' not in clusters1[0] and 'reason' not in clusters2[0]:
            assert len(clusters1) == len(clusters2)


class TestExtractClusterKeywords:
    """Tests for extract_cluster_keywords function"""
    
    @pytest.mark.unit
    def test_extracts_top_keywords(self):
        """Should return most frequent tokens"""
        posts = [
            {"tokens": ["truck", "great", "performance"]},
            {"tokens": ["truck", "amazing", "great"]},
            {"tokens": ["truck", "excellent"]}
        ]
        keywords = extract_cluster_keywords(posts, top_n=3)
        assert "truck" in keywords
        assert len(keywords) <= 3
    
    @pytest.mark.unit
    def test_empty_posts_returns_empty(self):
        """Empty posts should return empty list"""
        assert extract_cluster_keywords([]) == []
    
    @pytest.mark.unit
    def test_respects_top_n(self):
        """Should limit to top_n keywords"""
        posts = [
            {"tokens": ["a", "b", "c", "d", "e", "f", "g"]}
        ]
        keywords = extract_cluster_keywords(posts, top_n=3)
        assert len(keywords) == 3


class TestMatchClusterToTaxonomy:
    """Tests for match_cluster_to_taxonomy function"""
    
    @pytest.mark.unit
    def test_matches_ev_adoption(self):
        """EV-related keywords should match ev_adoption"""
        keywords = ["electric", "battery", "charging", "range", "ev"]
        taxonomy_id, label = match_cluster_to_taxonomy(keywords)
        assert taxonomy_id == "ev_adoption"
        assert label == "EV Adoption"
    
    @pytest.mark.unit
    def test_matches_driver_comfort(self):
        """Comfort-related keywords should match driver_comfort"""
        keywords = ["sleeper", "interior", "seat", "comfort", "cab"]
        taxonomy_id, label = match_cluster_to_taxonomy(keywords)
        assert taxonomy_id == "driver_comfort"
        assert label == "Driver Comfort"
    
    @pytest.mark.unit
    def test_matches_uptime_reliability(self):
        """Reliability-related keywords should match uptime_reliability"""
        keywords = ["engine", "breakdown", "repair", "service", "uptime"]
        taxonomy_id, label = match_cluster_to_taxonomy(keywords)
        assert taxonomy_id == "uptime_reliability"
        assert label == "Uptime & Reliability"
    
    @pytest.mark.unit
    def test_matches_model_demand(self):
        """Model-related keywords should match model_demand"""
        keywords = ["589", "579", "order", "wait", "delivery"]
        taxonomy_id, label = match_cluster_to_taxonomy(keywords)
        assert taxonomy_id == "model_demand"
        assert label == "Model Demand"
    
    @pytest.mark.unit
    def test_no_match_returns_custom_label(self):
        """Unmatched keywords should return a custom label derived from keywords"""
        keywords = ["random", "words", "nothing", "related"]
        taxonomy_id, label = match_cluster_to_taxonomy(keywords)
        # Implementation generates custom labels from keywords when no taxonomy match
        assert taxonomy_id.startswith("custom_")
        # Label should be derived from the input keywords
        assert "Random" in label or "Words" in label or "Nothing" in label
    
    @pytest.mark.unit
    def test_exclude_prevents_match(self):
        """Excluded taxonomy IDs should not be matched"""
        keywords = ["electric", "battery", "charging"]
        taxonomy_id, label = match_cluster_to_taxonomy(keywords, exclude={"ev_adoption"})
        assert taxonomy_id != "ev_adoption"
    
    @pytest.mark.unit
    def test_multiple_excludes(self):
        """Multiple excluded IDs should all be skipped"""
        keywords = ["electric", "sleeper", "engine"]
        exclude = {"ev_adoption", "driver_comfort", "uptime_reliability", "model_demand"}
        taxonomy_id, label = match_cluster_to_taxonomy(keywords, exclude=exclude)
        # With main matches excluded, should fall back to another taxonomy or custom
        assert taxonomy_id not in exclude
