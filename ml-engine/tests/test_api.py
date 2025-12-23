"""
Tests for Flask API endpoints
"""
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'app'))

import pytest
import json
from api import app


@pytest.fixture
def client():
    """Create test client"""
    app.config['TESTING'] = True
    with app.test_client() as client:
        yield client


class TestHealthEndpoint:
    """Tests for /health endpoint"""
    
    @pytest.mark.unit
    def test_health_returns_200(self, client):
        """Health check should return 200"""
        response = client.get('/health')
        assert response.status_code == 200
    
    @pytest.mark.unit
    def test_health_returns_healthy_status(self, client):
        """Health check should return healthy status"""
        response = client.get('/health')
        data = json.loads(response.data)
        assert data['status'] == 'healthy'
        assert data['service'] == 'ml-engine'


class TestAnalyzeEndpoint:
    """Tests for /api/analyze endpoint"""
    
    @pytest.mark.unit
    def test_missing_posts_returns_400(self, client):
        """Request without posts should return 400"""
        response = client.post('/api/analyze', 
                               json={},
                               content_type='application/json')
        assert response.status_code == 400
        data = json.loads(response.data)
        assert 'error' in data
        assert data['error']['code'] == 'MISSING_FIELD'
    
    @pytest.mark.unit
    def test_empty_posts_returns_empty_result(self, client):
        """Empty posts array should return empty result"""
        response = client.post('/api/analyze',
                               json={'posts': []},
                               content_type='application/json')
        assert response.status_code == 200
        data = json.loads(response.data)
        assert data['clusters'] == []
        assert data['posts'] == []
        assert data['postsAnalyzed'] == 0
    
    @pytest.mark.unit
    def test_valid_request_returns_200(self, client):
        """Valid request should return 200 with results"""
        posts = [
            {"id": "1", "content": "Great truck performance!", "source": "twitter"},
            {"id": "2", "content": "Love the new 579 model", "source": "forums"},
            {"id": "3", "content": "Amazing torque on this beast", "source": "youtube"}
        ]
        response = client.post('/api/analyze',
                               json={'posts': posts},
                               content_type='application/json')
        assert response.status_code == 200
        data = json.loads(response.data)
        assert 'clusters' in data
        assert 'posts' in data
        assert data['postsAnalyzed'] == 3
        assert 'processingTimeMs' in data
    
    @pytest.mark.unit
    def test_posts_have_sentiment(self, client):
        """Returned posts should have sentiment scores"""
        posts = [{"id": "1", "content": "Great truck!", "source": "twitter"}]
        response = client.post('/api/analyze',
                               json={'posts': posts},
                               content_type='application/json')
        data = json.loads(response.data)
        assert 'sentiment' in data['posts'][0]
    
    @pytest.mark.unit
    def test_missing_post_id_returns_400(self, client):
        """Post without id should return 400"""
        posts = [{"content": "Test content"}]
        response = client.post('/api/analyze',
                               json={'posts': posts},
                               content_type='application/json')
        assert response.status_code == 400
        data = json.loads(response.data)
        assert data['error']['code'] == 'MISSING_FIELD'
    
    @pytest.mark.unit
    def test_missing_post_content_returns_400(self, client):
        """Post without content should return 400"""
        posts = [{"id": "1"}]
        response = client.post('/api/analyze',
                               json={'posts': posts},
                               content_type='application/json')
        assert response.status_code == 400
        data = json.loads(response.data)
        assert data['error']['code'] == 'MISSING_FIELD'
    
    @pytest.mark.unit
    def test_too_many_posts_returns_413(self, client):
        """Exceeding max posts should return 413"""
        # Create more posts than the limit (default 500)
        posts = [{"id": str(i), "content": f"Test {i}"} for i in range(501)]
        response = client.post('/api/analyze',
                               json={'posts': posts},
                               content_type='application/json')
        assert response.status_code == 413
        data = json.loads(response.data)
        assert data['error']['code'] == 'PAYLOAD_TOO_LARGE'
    
    @pytest.mark.unit
    def test_content_too_long_returns_413(self, client):
        """Post with content exceeding max length should return 413"""
        posts = [{"id": "1", "content": "x" * 10001}]  # Default max is 10000
        response = client.post('/api/analyze',
                               json={'posts': posts},
                               content_type='application/json')
        assert response.status_code == 413
        data = json.loads(response.data)
        assert data['error']['code'] == 'CONTENT_TOO_LARGE'


class TestSentimentEndpoint:
    """Tests for /api/sentiment endpoint"""
    
    @pytest.mark.unit
    def test_missing_posts_returns_400(self, client):
        """Request without posts should return 400"""
        response = client.post('/api/sentiment',
                               json={},
                               content_type='application/json')
        assert response.status_code == 400
        data = json.loads(response.data)
        assert 'error' in data
    
    @pytest.mark.unit
    def test_empty_posts_returns_empty_result(self, client):
        """Empty posts array should return empty result"""
        response = client.post('/api/sentiment',
                               json={'posts': []},
                               content_type='application/json')
        assert response.status_code == 200
        data = json.loads(response.data)
        assert data['posts'] == []
        assert data['count'] == 0
    
    @pytest.mark.unit
    def test_valid_request_returns_200(self, client):
        """Valid request should return 200 with sentiment"""
        posts = [
            {"id": "1", "content": "Great truck!"},
            {"id": "2", "content": "Terrible experience."}
        ]
        response = client.post('/api/sentiment',
                               json={'posts': posts},
                               content_type='application/json')
        assert response.status_code == 200
        data = json.loads(response.data)
        assert len(data['posts']) == 2
        assert data['count'] == 2
    
    @pytest.mark.unit
    def test_posts_have_sentiment_scores(self, client):
        """Returned posts should have sentiment scores"""
        posts = [{"id": "1", "content": "Amazing performance!"}]
        response = client.post('/api/sentiment',
                               json={'posts': posts},
                               content_type='application/json')
        data = json.loads(response.data)
        sentiment = data['posts'][0]['sentiment']
        assert 'compound' in sentiment
        assert 'positive' in sentiment
        assert 'negative' in sentiment
        assert 'neutral' in sentiment


class TestErrorHandling:
    """Tests for error handling"""
    
    @pytest.mark.unit
    def test_404_returns_json(self, client):
        """404 errors should return JSON"""
        response = client.get('/nonexistent')
        assert response.status_code == 404
        data = json.loads(response.data)
        assert 'error' in data
        assert data['error']['code'] == 'NOT_FOUND'
    
    @pytest.mark.unit
    def test_invalid_json_returns_400(self, client):
        """Invalid JSON should return 400"""
        response = client.post('/api/analyze',
                               data='not valid json',
                               content_type='application/json')
        assert response.status_code == 400
    
    @pytest.mark.unit
    def test_error_envelope_structure(self, client):
        """Error responses should have consistent structure"""
        response = client.post('/api/analyze',
                               json={},
                               content_type='application/json')
        data = json.loads(response.data)
        assert 'error' in data
        assert 'code' in data['error']
        assert 'message' in data['error']
