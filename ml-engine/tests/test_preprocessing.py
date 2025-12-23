"""
Tests for preprocessing module
"""
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'app'))

import pytest
from preprocessing import clean_text, tokenize, remove_stop_words, preprocess_posts


class TestCleanText:
    """Tests for clean_text function"""
    
    @pytest.mark.unit
    def test_removes_urls(self):
        """URLs should be stripped from text"""
        text = "Check out https://example.com for more info"
        result = clean_text(text)
        assert "https" not in result
        assert "example" not in result
    
    @pytest.mark.unit
    def test_removes_mentions(self):
        """@mentions should be removed"""
        text = "Hey @truckerJoe what do you think?"
        result = clean_text(text)
        assert "@truckerJoe" not in result
        assert "truckerJoe" not in result
    
    @pytest.mark.unit
    def test_keeps_hashtag_text(self):
        """Hashtag symbol removed but text kept"""
        text = "Love the #Peterbilt579"
        result = clean_text(text)
        assert "#" not in result
        assert "peterbilt579" in result
    
    @pytest.mark.unit
    def test_lowercases_text(self):
        """Text should be lowercased"""
        text = "GREAT Truck Performance"
        result = clean_text(text)
        assert result == "great truck performance"
    
    @pytest.mark.unit
    def test_removes_special_characters(self):
        """Special characters should be replaced with spaces"""
        text = "Best truck!!! Love it..."
        result = clean_text(text)
        assert "!" not in result
        assert "." not in result
    
    @pytest.mark.unit
    def test_normalizes_whitespace(self):
        """Multiple spaces should be collapsed"""
        text = "Too   much    space"
        result = clean_text(text)
        assert "  " not in result
    
    @pytest.mark.unit
    def test_empty_input(self):
        """Empty string should return empty string"""
        assert clean_text("") == ""
        assert clean_text(None) == ""
    
    @pytest.mark.unit
    def test_idempotent(self):
        """Cleaning already-cleaned text should produce same result"""
        text = "this is already clean text"
        first_pass = clean_text(text)
        second_pass = clean_text(first_pass)
        assert first_pass == second_pass


class TestTokenize:
    """Tests for tokenize function"""
    
    @pytest.mark.unit
    def test_splits_on_whitespace(self):
        """Text should be split into tokens"""
        result = tokenize("hello world test")
        assert result == ["hello", "world", "test"]
    
    @pytest.mark.unit
    def test_empty_input(self):
        """Empty string should return empty list"""
        assert tokenize("") == []
        assert tokenize(None) == []


class TestRemoveStopWords:
    """Tests for remove_stop_words function"""
    
    @pytest.mark.unit
    def test_removes_english_stopwords(self):
        """Common English stopwords should be removed"""
        tokens = ["the", "truck", "is", "great"]
        result = remove_stop_words(tokens)
        assert "the" not in result
        assert "is" not in result
        assert "great" in result
    
    @pytest.mark.unit
    def test_removes_domain_stopwords(self):
        """Domain-specific stopwords should be removed"""
        tokens = ["peterbilt", "truck", "amazing", "performance"]
        result = remove_stop_words(tokens)
        assert "peterbilt" not in result
        assert "truck" not in result
        assert "amazing" in result
        assert "performance" in result
    
    @pytest.mark.unit
    def test_removes_short_tokens(self):
        """Tokens with 2 or fewer characters should be removed"""
        tokens = ["a", "is", "the", "great", "ev"]
        result = remove_stop_words(tokens)
        assert "a" not in result
        assert "ev" not in result
    
    @pytest.mark.unit
    def test_custom_stopwords(self):
        """Custom stopwords should also be removed"""
        tokens = ["custom", "word", "test"]
        result = remove_stop_words(tokens, custom_stop_words={"custom"})
        assert "custom" not in result
        assert "word" in result


class TestPreprocessPosts:
    """Tests for preprocess_posts function"""
    
    @pytest.mark.unit
    def test_adds_cleaned_content(self):
        """Posts should have cleaned_content field added"""
        posts = [{"id": "1", "content": "Check out https://example.com!"}]
        result = preprocess_posts(posts)
        assert "cleaned_content" in result[0]
        assert "https" not in result[0]["cleaned_content"]
    
    @pytest.mark.unit
    def test_adds_tokens(self):
        """Posts should have tokens field added"""
        posts = [{"id": "1", "content": "Great truck performance"}]
        result = preprocess_posts(posts)
        assert "tokens" in result[0]
        assert isinstance(result[0]["tokens"], list)
    
    @pytest.mark.unit
    def test_preserves_original_fields(self):
        """Original post fields should be preserved"""
        posts = [{"id": "1", "content": "Test", "source": "twitter", "author": "user1"}]
        result = preprocess_posts(posts)
        assert result[0]["id"] == "1"
        assert result[0]["source"] == "twitter"
        assert result[0]["author"] == "user1"
    
    @pytest.mark.unit
    def test_handles_missing_content(self):
        """Posts without content should be handled gracefully"""
        posts = [{"id": "1"}]
        result = preprocess_posts(posts)
        assert result[0]["cleaned_content"] == ""
        assert result[0]["tokens"] == []
