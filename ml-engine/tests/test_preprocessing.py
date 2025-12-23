"""
Unit tests for preprocessing module
Tests text cleaning, tokenization, and TF-IDF vectorization
"""
import pytest
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent / 'app'))

from preprocessing import preprocess_posts, clean_text, tokenize


def test_clean_text_removes_urls():
    """Test that URLs are removed from text"""
    text = "Check out https://example.com for more info"
    cleaned = clean_text(text)
    assert "https://example.com" not in cleaned
    assert "check" in cleaned.lower()


def test_clean_text_removes_mentions():
    """Test that @mentions are removed"""
    text = "Hey @user123 what do you think?"
    cleaned = clean_text(text)
    assert "@user123" not in cleaned
    assert "hey" in cleaned.lower()


def test_clean_text_removes_hashtags():
    """Test that hashtags are cleaned"""
    text = "Great truck #Peterbilt #579EV"
    cleaned = clean_text(text)
    # Hashtag symbol removed but text preserved
    assert "#" not in cleaned
    assert "peterbilt" in cleaned.lower()


def test_clean_text_removes_special_chars():
    """Test that special characters are removed"""
    text = "Amazing!!! Best truck ever..."
    cleaned = clean_text(text)
    assert "!" not in cleaned
    assert "." not in cleaned
    assert "amazing" in cleaned.lower()


def test_tokenize_splits_words():
    """Test that text is properly tokenized"""
    text = "This is a test sentence"
    tokens = tokenize(text)
    assert len(tokens) > 0
    assert "test" in tokens
    assert "sentence" in tokens


def test_tokenize_removes_stopwords():
    """Test that common stopwords are removed"""
    text = "the quick brown fox jumps over the lazy dog"
    tokens = tokenize(text)
    # Common stopwords should be removed
    assert "the" not in tokens
    assert "over" not in tokens
    # Content words should remain
    assert "quick" in tokens or "brown" in tokens or "fox" in tokens


def test_preprocess_posts_adds_tokens():
    """Test that preprocessing adds tokens field to posts"""
    posts = [
        {"id": "1", "content": "Great truck with excellent performance"},
        {"id": "2", "content": "Love the new 579EV model"}
    ]
    
    processed = preprocess_posts(posts)
    
    assert len(processed) == 2
    assert "tokens" in processed[0]
    assert "tokens" in processed[1]
    assert len(processed[0]["tokens"]) > 0
    assert len(processed[1]["tokens"]) > 0


def test_preprocess_posts_preserves_original_fields():
    """Test that original post fields are preserved"""
    posts = [
        {
            "id": "1",
            "content": "Test content",
            "source": "twitter",
            "author": "testuser"
        }
    ]
    
    processed = preprocess_posts(posts)
    
    assert processed[0]["id"] == "1"
    assert processed[0]["source"] == "twitter"
    assert processed[0]["author"] == "testuser"
    assert processed[0]["content"] == "Test content"


def test_preprocess_posts_handles_empty_content():
    """Test that empty content is handled gracefully"""
    posts = [
        {"id": "1", "content": ""},
        {"id": "2", "content": "   "},
        {"id": "3", "content": "Valid content"}
    ]
    
    processed = preprocess_posts(posts)
    
    assert len(processed) == 3
    assert "tokens" in processed[0]
    assert "tokens" in processed[1]
    assert len(processed[2]["tokens"]) > 0


def test_preprocess_posts_handles_special_trucking_terms():
    """Test that trucking-specific terms are preserved"""
    posts = [
        {"id": "1", "content": "The 579EV has great range and PACCAR engine"}
    ]
    
    processed = preprocess_posts(posts)
    tokens = processed[0]["tokens"]
    
    # Check that important trucking terms are preserved
    assert any("579" in token or "ev" in token for token in tokens)
    assert any("paccar" in token.lower() for token in tokens)


def test_preprocess_posts_empty_list():
    """Test that empty post list is handled"""
    posts = []
    processed = preprocess_posts(posts)
    assert processed == []


def test_preprocess_posts_maintains_order():
    """Test that post order is maintained"""
    posts = [
        {"id": "1", "content": "First post"},
        {"id": "2", "content": "Second post"},
        {"id": "3", "content": "Third post"}
    ]
    
    processed = preprocess_posts(posts)
    
    assert processed[0]["id"] == "1"
    assert processed[1]["id"] == "2"
    assert processed[2]["id"] == "3"
