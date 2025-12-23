"""
Tests for sentiment analysis module
"""
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'app'))

import pytest
from unittest.mock import patch, MagicMock
from sentiment import (
    analyze_sentiment, 
    _analyze_sentiment_vader,
    analyze_posts_sentiment,
    aggregate_cluster_sentiment,
    classify_sentiment
)


class TestAnalyzeSentimentVader:
    """Tests for VADER sentiment analysis (fallback method)"""
    
    @pytest.mark.unit
    def test_positive_text(self):
        """Positive text should have positive compound score"""
        result = _analyze_sentiment_vader("This truck is amazing and fantastic!")
        assert result['compound'] > 0
        assert result['positive'] > result['negative']
    
    @pytest.mark.unit
    def test_negative_text(self):
        """Negative text should have negative compound score"""
        result = _analyze_sentiment_vader("This truck is terrible and broken down.")
        assert result['compound'] < 0
        assert result['negative'] > result['positive']
    
    @pytest.mark.unit
    def test_neutral_text(self):
        """Neutral text should have near-zero compound score"""
        result = _analyze_sentiment_vader("The truck is blue.")
        assert abs(result['compound']) < 0.5
    
    @pytest.mark.unit
    def test_custom_lexicon_positive(self):
        """Custom positive terms should be recognized"""
        result = _analyze_sentiment_vader("This truck has insane torque!")
        assert result['compound'] > 0
    
    @pytest.mark.unit
    def test_custom_lexicon_negative(self):
        """Custom negative terms should be recognized"""
        result = _analyze_sentiment_vader("The truck had a breakdown and derate.")
        assert result['compound'] < 0
    
    @pytest.mark.unit
    def test_returns_all_fields(self):
        """Result should contain all required fields"""
        result = _analyze_sentiment_vader("Test text")
        assert 'compound' in result
        assert 'positive' in result
        assert 'negative' in result
        assert 'neutral' in result


class TestAnalyzeSentiment:
    """Tests for main analyze_sentiment function"""
    
    @pytest.mark.unit
    def test_empty_text_returns_neutral(self):
        """Empty text should return neutral sentiment"""
        result = analyze_sentiment("")
        assert result['compound'] == 0.0
        assert result['neutral'] == 1.0
    
    @pytest.mark.unit
    def test_whitespace_only_returns_neutral(self):
        """Whitespace-only text should return neutral sentiment"""
        result = analyze_sentiment("   ")
        assert result['compound'] == 0.0
        assert result['neutral'] == 1.0
    
    @pytest.mark.unit
    def test_none_text_returns_neutral(self):
        """None text should return neutral sentiment"""
        result = analyze_sentiment(None)
        assert result['compound'] == 0.0
        assert result['neutral'] == 1.0
    
    @pytest.mark.unit
    def test_falls_back_to_vader_when_gemini_unavailable(self):
        """Should use VADER when Gemini is not available"""
        with patch('sentiment.GEMINI_IMPORTED', False):
            result = analyze_sentiment("Great truck!")
            assert result is not None
            assert 'compound' in result


class TestAnalyzePostsSentiment:
    """Tests for batch sentiment analysis"""
    
    @pytest.mark.unit
    def test_adds_sentiment_to_posts(self):
        """Each post should have sentiment field added"""
        posts = [
            {"id": "1", "content": "Great truck!"},
            {"id": "2", "content": "Terrible experience."}
        ]
        result = analyze_posts_sentiment(posts)
        assert all('sentiment' in post for post in result)
    
    @pytest.mark.unit
    def test_preserves_original_fields(self):
        """Original post fields should be preserved"""
        posts = [{"id": "1", "content": "Test", "source": "twitter"}]
        result = analyze_posts_sentiment(posts)
        assert result[0]["id"] == "1"
        assert result[0]["source"] == "twitter"
    
    @pytest.mark.unit
    def test_handles_empty_content(self):
        """Posts with empty content should get neutral sentiment"""
        posts = [{"id": "1", "content": ""}]
        result = analyze_posts_sentiment(posts)
        assert result[0]['sentiment']['compound'] == 0.0


class TestAggregateClusterSentiment:
    """Tests for cluster sentiment aggregation"""
    
    @pytest.mark.unit
    def test_calculates_average(self):
        """Should return average of compound scores"""
        posts = [
            {"sentiment": {"compound": 0.5}},
            {"sentiment": {"compound": 0.3}},
            {"sentiment": {"compound": 0.2}}
        ]
        result = aggregate_cluster_sentiment(posts)
        assert abs(result - 0.333) < 0.01
    
    @pytest.mark.unit
    def test_empty_posts_returns_zero(self):
        """Empty posts list should return 0"""
        assert aggregate_cluster_sentiment([]) == 0.0
    
    @pytest.mark.unit
    def test_handles_missing_sentiment(self):
        """Posts without sentiment should be treated as 0"""
        posts = [{"id": "1"}, {"sentiment": {"compound": 0.5}}]
        result = aggregate_cluster_sentiment(posts)
        assert result == 0.25


class TestClassifySentiment:
    """Tests for sentiment classification"""
    
    @pytest.mark.unit
    def test_positive_classification(self):
        """Scores >= 0.3 should be positive"""
        assert classify_sentiment(0.3) == "positive"
        assert classify_sentiment(0.8) == "positive"
        assert classify_sentiment(1.0) == "positive"
    
    @pytest.mark.unit
    def test_negative_classification(self):
        """Scores <= -0.3 should be negative"""
        assert classify_sentiment(-0.3) == "negative"
        assert classify_sentiment(-0.8) == "negative"
        assert classify_sentiment(-1.0) == "negative"
    
    @pytest.mark.unit
    def test_neutral_classification(self):
        """Scores between -0.3 and 0.3 should be neutral"""
        assert classify_sentiment(0.0) == "neutral"
        assert classify_sentiment(0.29) == "neutral"
        assert classify_sentiment(-0.29) == "neutral"
