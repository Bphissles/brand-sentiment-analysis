"""
Integration tests for Flask API
Tests API endpoints and request/response handling
"""
import pytest
import sys
import json
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent / 'app'))

from api import app


@pytest.fixture
def client():
    """Create test client"""
    app.config['TESTING'] = True
    with app.test_client() as client:
        yield client


def test_health_endpoint(client):
    """Test health check endpoint"""
    response = client.get('/health')
    
    assert response.status_code == 200
    data = json.loads(response.data)
    assert data['status'] == 'healthy'
    assert data['service'] == 'ml-engine'


def test_analyze_endpoint_with_valid_data(client):
    """Test analyze endpoint with valid post data"""
    payload = {
        "posts": [
            {"id": "1", "content": "Great electric truck with excellent range"},
            {"id": "2", "content": "Comfortable sleeper cab and ergonomic design"},
            {"id": "3", "content": "Reliable PACCAR engine with good service"},
            {"id": "4", "content": "Waiting for 589 model availability"}
        ]
    }
    
    response = client.post('/api/analyze',
                          data=json.dumps(payload),
                          content_type='application/json')
    
    assert response.status_code == 200
    data = json.loads(response.data)
    
    assert 'clusters' in data
    assert 'posts' in data
    assert 'postsAnalyzed' in data
    assert 'processingTimeMs' in data
    assert data['postsAnalyzed'] == 4


def test_analyze_endpoint_response_structure(client):
    """Test that analyze response has correct structure"""
    payload = {
        "posts": [
            {"id": "1", "content": "Test post about electric vehicles"},
            {"id": "2", "content": "Another test about comfort"}
        ]
    }
    
    response = client.post('/api/analyze',
                          data=json.dumps(payload),
                          content_type='application/json')
    
    data = json.loads(response.data)
    
    # Check clusters structure
    if len(data['clusters']) > 0:
        cluster = data['clusters'][0]
        assert 'id' in cluster
        assert 'taxonomyId' in cluster
        assert 'label' in cluster
        assert 'keywords' in cluster
        assert 'postCount' in cluster
        assert 'postIds' in cluster
        assert 'sentiment' in cluster
        assert 'sentimentLabel' in cluster
    
    # Check posts structure
    if len(data['posts']) > 0:
        post = data['posts'][0]
        assert 'id' in post
        assert 'content' in post
        assert 'sentiment' in post
        assert 'keywords' in post


def test_analyze_endpoint_with_empty_posts(client):
    """Test analyze endpoint with empty posts array"""
    payload = {"posts": []}
    
    response = client.post('/api/analyze',
                          data=json.dumps(payload),
                          content_type='application/json')
    
    assert response.status_code == 200
    data = json.loads(response.data)
    assert data['postsAnalyzed'] == 0
    assert data['clusters'] == []
    assert data['posts'] == []


def test_analyze_endpoint_missing_posts_field(client):
    """Test analyze endpoint with missing posts field"""
    payload = {"data": []}
    
    response = client.post('/api/analyze',
                          data=json.dumps(payload),
                          content_type='application/json')
    
    assert response.status_code == 400
    data = json.loads(response.data)
    assert 'error' in data


def test_analyze_endpoint_invalid_json(client):
    """Test analyze endpoint with invalid JSON"""
    response = client.post('/api/analyze',
                          data='invalid json',
                          content_type='application/json')
    
    assert response.status_code in [400, 415]


def test_sentiment_endpoint_with_valid_data(client):
    """Test sentiment-only endpoint"""
    payload = {
        "posts": [
            {"id": "1", "content": "Absolutely love this truck!"},
            {"id": "2", "content": "Terrible experience, very disappointed"}
        ]
    }
    
    response = client.post('/api/sentiment',
                          data=json.dumps(payload),
                          content_type='application/json')
    
    assert response.status_code == 200
    data = json.loads(response.data)
    
    assert 'posts' in data
    assert 'count' in data
    assert data['count'] == 2
    assert len(data['posts']) == 2


def test_sentiment_endpoint_response_structure(client):
    """Test sentiment endpoint response structure"""
    payload = {
        "posts": [{"id": "1", "content": "Test content"}]
    }
    
    response = client.post('/api/sentiment',
                          data=json.dumps(payload),
                          content_type='application/json')
    
    data = json.loads(response.data)
    post = data['posts'][0]
    
    assert 'sentiment' in post
    assert 'compound' in post['sentiment']
    assert 'positive' in post['sentiment']
    assert 'negative' in post['sentiment']
    assert 'neutral' in post['sentiment']


def test_sentiment_endpoint_missing_posts(client):
    """Test sentiment endpoint with missing posts field"""
    payload = {}
    
    response = client.post('/api/sentiment',
                          data=json.dumps(payload),
                          content_type='application/json')
    
    assert response.status_code == 400
    data = json.loads(response.data)
    assert 'error' in data


def test_analyze_endpoint_preserves_post_fields(client):
    """Test that analyze preserves original post fields"""
    payload = {
        "posts": [
            {
                "id": "123",
                "content": "Test content",
                "source": "twitter",
                "author": "testuser"
            }
        ]
    }
    
    response = client.post('/api/analyze',
                          data=json.dumps(payload),
                          content_type='application/json')
    
    data = json.loads(response.data)
    post = data['posts'][0]
    
    assert post['id'] == "123"
    assert post['source'] == "twitter"
    assert post['author'] == "testuser"


def test_analyze_endpoint_processing_time(client):
    """Test that processing time is reasonable"""
    payload = {
        "posts": [
            {"id": str(i), "content": f"Test post {i}"}
            for i in range(10)
        ]
    }
    
    response = client.post('/api/analyze',
                          data=json.dumps(payload),
                          content_type='application/json')
    
    data = json.loads(response.data)
    
    # Processing time should be reasonable (less than 5 seconds for 10 posts)
    assert data['processingTimeMs'] < 5000
    assert data['processingTimeMs'] > 0
