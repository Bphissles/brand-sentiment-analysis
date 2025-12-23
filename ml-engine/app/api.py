"""
Flask API for ML Engine
Provides endpoints for clustering and sentiment analysis
"""
import os
import time
from flask import Flask, jsonify, request
from dotenv import load_dotenv

from preprocessing import preprocess_posts
from sentiment import analyze_posts_sentiment, aggregate_cluster_sentiment, classify_sentiment
from clustering import cluster_posts

load_dotenv()

app = Flask(__name__)


@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({'status': 'healthy', 'service': 'ml-engine'})


@app.route('/api/analyze', methods=['POST'])
def analyze():
    """
    Analyze posts for clustering and sentiment
    
    Expected payload:
    {
        "posts": [
            {"id": "1", "content": "...", "source": "twitter"},
            ...
        ]
    }
    
    Returns:
    {
        "clusters": [...],
        "posts": [...],
        "postsAnalyzed": int,
        "processingTimeMs": int
    }
    """
    start_time = time.time()
    
    data = request.get_json()
    
    if not data or 'posts' not in data:
        return jsonify({'error': 'Missing posts in request body'}), 400
    
    posts = data['posts']
    
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
    n_clusters = min(4, len(posts))  # Don't create more clusters than posts
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
    """
    Analyze sentiment for posts without clustering
    
    Expected payload:
    {
        "posts": [{"id": "1", "content": "..."}]
    }
    """
    data = request.get_json()
    
    if not data or 'posts' not in data:
        return jsonify({'error': 'Missing posts in request body'}), 400
    
    posts = data['posts']
    results = analyze_posts_sentiment(posts)
    
    return jsonify({
        'posts': results,
        'count': len(results)
    })


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    debug = os.environ.get('FLASK_DEBUG', '0') == '1'
    app.run(host='0.0.0.0', port=port, debug=debug)
