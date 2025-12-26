"""
Flask API for ML Engine
Provides endpoints for clustering and sentiment analysis
"""
import os
import time
import math
import logging
from flask import Flask, jsonify, request
from flasgger import Swagger
from dotenv import load_dotenv

from preprocessing import preprocess_posts
from sentiment import analyze_posts_sentiment, aggregate_cluster_sentiment, classify_sentiment
from clustering import cluster_posts

load_dotenv()

logger = logging.getLogger(__name__)

app = Flask(__name__)

# Swagger/OpenAPI configuration
swagger_config = {
    "headers": [],
    "specs": [
        {
            "endpoint": "apispec",
            "route": "/apispec.json",
            "rule_filter": lambda rule: True,
            "model_filter": lambda tag: True,
        }
    ],
    "static_url_path": "/flasgger_static",
    "swagger_ui": True,
    "specs_route": "/apidocs/"
}

swagger_template = {
    "info": {
        "title": "ML Engine API",
        "description": "Sentiment analysis and clustering API for the Brand Sentiment Analyzer",
        "version": "1.0.0",
        "contact": {
            "name": "API Support"
        }
    },
    "basePath": "/",
    "schemes": ["http", "https"]
}

swagger = Swagger(app, config=swagger_config, template=swagger_template)

# Request size limits (configurable via environment)
MAX_POSTS_PER_REQUEST = int(os.environ.get('MAX_POSTS_PER_REQUEST', '500'))
MAX_CONTENT_LENGTH = int(os.environ.get('MAX_CONTENT_LENGTH', '10000'))


def make_error_response(code: str, message: str, details: str = None, status_code: int = 400):
    """Create a consistent JSON error response envelope"""
    error = {
        'error': {
            'code': code,
            'message': message
        }
    }
    if details:
        error['error']['details'] = details
    return jsonify(error), status_code


def validate_posts(posts: list) -> tuple:
    """
    Validate posts array structure and enforce limits.
    
    Returns:
        Tuple of (is_valid, error_response_or_none)
    """
    if not isinstance(posts, list):
        return False, make_error_response(
            'INVALID_PAYLOAD',
            'posts must be an array',
            status_code=400
        )
    
    if len(posts) > MAX_POSTS_PER_REQUEST:
        return False, make_error_response(
            'PAYLOAD_TOO_LARGE',
            f'Too many posts in request',
            f'Maximum {MAX_POSTS_PER_REQUEST} posts allowed, received {len(posts)}',
            status_code=413
        )
    
    for i, post in enumerate(posts):
        if not isinstance(post, dict):
            return False, make_error_response(
                'INVALID_POST',
                f'Post at index {i} is not an object',
                status_code=400
            )
        
        if 'content' not in post:
            return False, make_error_response(
                'MISSING_FIELD',
                f'Post at index {i} is missing required field: content',
                status_code=400
            )
        
        if not isinstance(post.get('content'), str):
            return False, make_error_response(
                'INVALID_FIELD',
                f'Post at index {i} has invalid content type (expected string)',
                status_code=400
            )
        
        if len(post.get('content', '')) > MAX_CONTENT_LENGTH:
            return False, make_error_response(
                'CONTENT_TOO_LARGE',
                f'Post at index {i} content exceeds maximum length',
                f'Maximum {MAX_CONTENT_LENGTH} characters allowed',
                status_code=413
            )
        
        if 'id' not in post:
            return False, make_error_response(
                'MISSING_FIELD',
                f'Post at index {i} is missing required field: id',
                status_code=400
            )
    
    return True, None


@app.errorhandler(Exception)
def handle_exception(e):
    """Global error handler for unhandled exceptions"""
    logger.exception(f"Unhandled exception: {e}")
    return make_error_response(
        'INTERNAL_ERROR',
        'An unexpected error occurred',
        status_code=500
    )


@app.errorhandler(400)
def handle_bad_request(e):
    """Handle 400 errors"""
    return make_error_response(
        'BAD_REQUEST',
        str(e.description) if hasattr(e, 'description') else 'Bad request',
        status_code=400
    )


@app.errorhandler(404)
def handle_not_found(e):
    """Handle 404 errors"""
    return make_error_response(
        'NOT_FOUND',
        'Resource not found',
        status_code=404
    )


@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint
    ---
    tags:
      - System
    responses:
      200:
        description: Service health status
        schema:
          type: object
          properties:
            status:
              type: string
              example: healthy
            service:
              type: string
              example: ml-engine
    """
    return jsonify({'status': 'healthy', 'service': 'ml-engine'})


@app.route('/api/analyze', methods=['POST'])
def analyze():
    """Analyze posts for clustering and sentiment
    ---
    tags:
      - Analysis
    consumes:
      - application/json
    produces:
      - application/json
    parameters:
      - in: body
        name: body
        required: true
        schema:
          type: object
          required:
            - posts
          properties:
            posts:
              type: array
              items:
                type: object
                required:
                  - id
                  - content
                properties:
                  id:
                    type: string
                    example: "1"
                  content:
                    type: string
                    example: "Great product, highly recommend!"
                  source:
                    type: string
                    example: "twitter"
                  author:
                    type: string
                    example: "user123"
                  publishedAt:
                    type: string
                    example: "2024-01-15T10:30:00Z"
    responses:
      200:
        description: Analysis results with clusters and sentiment
        schema:
          type: object
          properties:
            clusters:
              type: array
              items:
                type: object
                properties:
                  id:
                    type: string
                  label:
                    type: string
                  postIds:
                    type: array
                    items:
                      type: string
                  sentiment:
                    type: number
                  sentimentLabel:
                    type: string
            posts:
              type: array
              items:
                type: object
                properties:
                  id:
                    type: string
                  content:
                    type: string
                  sentiment:
                    type: number
                  clusterId:
                    type: string
                  keywords:
                    type: array
                    items:
                      type: string
            postsAnalyzed:
              type: integer
            processingTimeMs:
              type: integer
      400:
        description: Invalid request payload
      413:
        description: Payload too large
    """
    start_time = time.time()
    
    data = request.get_json()
    
    if not data or 'posts' not in data:
        return make_error_response(
            'MISSING_FIELD',
            'Missing posts in request body',
            status_code=400
        )
    
    posts = data['posts']
    
    # Validate posts structure and limits
    is_valid, error_response = validate_posts(posts)
    if not is_valid:
        return error_response
    
    if len(posts) == 0:
        return jsonify({
            'clusters': [],
            'posts': [],
            'postsAnalyzed': 0,
            'processingTimeMs': 0
        })
    
    # Step 1: Preprocess posts (clean text, tokenize)
    preprocessed = preprocess_posts(posts)
    
    # Step 2: Analyze sentiment for each post
    with_sentiment = analyze_posts_sentiment(preprocessed)
    
    # Step 3: Cluster posts by topic
    # Dynamic cluster count based on data size
    # Formula: sqrt(n/2), clamped between 3 and 10
    n_clusters = max(3, min(10, int(math.sqrt(len(posts) / 2))))
    n_clusters = min(n_clusters, len(posts))  # Don't create more clusters than posts
    clusters, clustered_posts = cluster_posts(with_sentiment, n_clusters=n_clusters)
    
    # Step 4: Calculate aggregate sentiment per cluster
    for cluster in clusters:
        cluster_post_ids = set(cluster.get('postIds', []))
        cluster_posts_list = [p for p in clustered_posts if p.get('id') in cluster_post_ids]
        
        avg_sentiment = aggregate_cluster_sentiment(cluster_posts_list)
        cluster['sentiment'] = round(avg_sentiment, 3)
        cluster['sentimentLabel'] = classify_sentiment(avg_sentiment)
    
    # Calculate processing time
    processing_time_ms = int((time.time() - start_time) * 1000)
    
    # Prepare response (clean up internal fields from posts)
    response_posts = []
    for post in clustered_posts:
        response_post = {
            'id': post.get('id'),
            'source': post.get('source'),
            'content': post.get('content'),
            'author': post.get('author'),
            'publishedAt': post.get('publishedAt'),
            'sentiment': post.get('sentiment'),
            'clusterId': post.get('clusterId'),
            'keywords': post.get('tokens', [])[:10]  # Top 10 tokens as keywords
        }
        response_posts.append(response_post)
    
    return jsonify({
        'clusters': clusters,
        'posts': response_posts,
        'postsAnalyzed': len(posts),
        'processingTimeMs': processing_time_ms
    })


@app.route('/api/sentiment', methods=['POST'])
def sentiment_only():
    """Analyze sentiment for posts without clustering
    ---
    tags:
      - Analysis
    consumes:
      - application/json
    produces:
      - application/json
    parameters:
      - in: body
        name: body
        required: true
        schema:
          type: object
          required:
            - posts
          properties:
            posts:
              type: array
              items:
                type: object
                required:
                  - id
                  - content
                properties:
                  id:
                    type: string
                    example: "1"
                  content:
                    type: string
                    example: "This is a great product!"
    responses:
      200:
        description: Sentiment analysis results
        schema:
          type: object
          properties:
            posts:
              type: array
              items:
                type: object
                properties:
                  id:
                    type: string
                  content:
                    type: string
                  sentiment:
                    type: number
            count:
              type: integer
      400:
        description: Invalid request payload
      413:
        description: Payload too large
    """
    data = request.get_json()
    
    if not data or 'posts' not in data:
        return make_error_response(
            'MISSING_FIELD',
            'Missing posts in request body',
            status_code=400
        )
    
    posts = data['posts']
    
    # Validate posts structure and limits
    is_valid, error_response = validate_posts(posts)
    if not is_valid:
        return error_response
    
    if len(posts) == 0:
        return jsonify({
            'posts': [],
            'count': 0
        })
    
    results = analyze_posts_sentiment(posts)
    
    return jsonify({
        'posts': results,
        'count': len(results)
    })


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    debug = os.environ.get('FLASK_DEBUG', '0') == '1'
    app.run(host='0.0.0.0', port=port, debug=debug)
