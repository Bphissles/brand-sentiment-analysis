"""
Unit tests for sentiment analysis module
Tests VADER sentiment scoring and classification
"""
import pytest
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent / 'app'))

from sentiment import analyze_posts_sentiment, aggregate_cluster_sentiment, classify_sentiment


def test_analyze_posts_sentiment_adds_sentiment_field():
    """Test that sentiment analysis adds sentiment field to posts"""
    posts = [
        {"id": "1", "content": "This truck is absolutely amazing!"},
        {"id": "2", "content": "Terrible experience, very disappointed"}
    ]
    
    analyzed = analyze_posts_sentiment(posts)
    
    assert len(analyzed) == 2
    assert "sentiment" in analyzed[0]
    assert "sentiment" in analyzed[1]


def test_analyze_posts_sentiment_structure():
    """Test that sentiment object has correct structure"""
    posts = [{"id": "1", "content": "Great truck"}]
    
    analyzed = analyze_posts_sentiment(posts)
    sentiment = analyzed[0]["sentiment"]
    
    assert "compound" in sentiment
    assert "positive" in sentiment
    assert "negative" in sentiment
    assert "neutral" in sentiment


def test_analyze_posts_sentiment_ranges():
    """Test that sentiment scores are in valid ranges"""
    posts = [{"id": "1", "content": "Excellent performance and reliability"}]
    
    analyzed = analyze_posts_sentiment(posts)
    sentiment = analyzed[0]["sentiment"]
    
    # Compound score should be between -1 and 1
    assert -1 <= sentiment["compound"] <= 1
    # Component scores should be between 0 and 1
    assert 0 <= sentiment["positive"] <= 1
    assert 0 <= sentiment["negative"] <= 1
    assert 0 <= sentiment["neutral"] <= 1


def test_analyze_posts_sentiment_positive_text():
    """Test that positive text gets positive sentiment"""
    posts = [{"id": "1", "content": "Absolutely love this truck! Best purchase ever!"}]
    
    analyzed = analyze_posts_sentiment(posts)
    sentiment = analyzed[0]["sentiment"]
    
    assert sentiment["compound"] > 0
    assert sentiment["positive"] > sentiment["negative"]


def test_analyze_posts_sentiment_negative_text():
    """Test that negative text gets negative sentiment"""
    posts = [{"id": "1", "content": "Horrible truck, terrible quality, very disappointed"}]
    
    analyzed = analyze_posts_sentiment(posts)
    sentiment = analyzed[0]["sentiment"]
    
    assert sentiment["compound"] < 0
    assert sentiment["negative"] > sentiment["positive"]


def test_analyze_posts_sentiment_neutral_text():
    """Test that neutral text gets neutral sentiment"""
    posts = [{"id": "1", "content": "The truck has a diesel engine"}]
    
    analyzed = analyze_posts_sentiment(posts)
    sentiment = analyzed[0]["sentiment"]
    
    # Neutral text should have compound close to 0
    assert abs(sentiment["compound"]) < 0.5


def test_classify_sentiment_positive():
    """Test positive sentiment classification"""
    assert classify_sentiment(0.6) == "positive"
    assert classify_sentiment(0.3) == "positive"


def test_classify_sentiment_negative():
    """Test negative sentiment classification"""
    assert classify_sentiment(-0.6) == "negative"
    assert classify_sentiment(-0.3) == "negative"


def test_classify_sentiment_neutral():
    """Test neutral sentiment classification"""
    assert classify_sentiment(0.0) == "neutral"
    assert classify_sentiment(0.1) == "neutral"
    assert classify_sentiment(-0.1) == "neutral"


def test_classify_sentiment_boundaries():
    """Test sentiment classification at boundaries"""
    # Just above neutral threshold
    assert classify_sentiment(0.25) == "neutral"
    # Just below neutral threshold
    assert classify_sentiment(-0.25) == "neutral"
    # At positive threshold
    assert classify_sentiment(0.3) == "positive"
    # At negative threshold
    assert classify_sentiment(-0.3) == "negative"


def test_aggregate_cluster_sentiment_calculates_average():
    """Test that cluster sentiment is averaged correctly"""
    posts = [
        {"sentiment": {"compound": 0.5}},
        {"sentiment": {"compound": 0.3}},
        {"sentiment": {"compound": 0.7}}
    ]
    
    avg = aggregate_cluster_sentiment(posts)
    
    expected = (0.5 + 0.3 + 0.7) / 3
    assert abs(avg - expected) < 0.01


def test_aggregate_cluster_sentiment_empty_list():
    """Test that empty cluster returns 0"""
    posts = []
    avg = aggregate_cluster_sentiment(posts)
    assert avg == 0.0


def test_aggregate_cluster_sentiment_single_post():
    """Test aggregation with single post"""
    posts = [{"sentiment": {"compound": 0.42}}]
    avg = aggregate_cluster_sentiment(posts)
    assert avg == 0.42


def test_aggregate_cluster_sentiment_mixed_sentiments():
    """Test aggregation with mixed positive and negative"""
    posts = [
        {"sentiment": {"compound": 0.8}},
        {"sentiment": {"compound": -0.6}},
        {"sentiment": {"compound": 0.2}}
    ]
    
    avg = aggregate_cluster_sentiment(posts)
    expected = (0.8 - 0.6 + 0.2) / 3
    assert abs(avg - expected) < 0.01


def test_analyze_posts_sentiment_preserves_fields():
    """Test that original post fields are preserved"""
    posts = [
        {
            "id": "1",
            "content": "Great truck",
            "source": "twitter",
            "author": "user1"
        }
    ]
    
    analyzed = analyze_posts_sentiment(posts)
    
    assert analyzed[0]["id"] == "1"
    assert analyzed[0]["source"] == "twitter"
    assert analyzed[0]["author"] == "user1"
    assert analyzed[0]["content"] == "Great truck"
