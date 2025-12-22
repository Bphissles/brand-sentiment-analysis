"""
Flask API for ML Engine
Provides endpoints for clustering and sentiment analysis
"""
import os
from flask import Flask, jsonify, request
from dotenv import load_dotenv

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
        "clusters": [
            {
                "id": 0,
                "label": "EV Adoption",
                "keywords": ["579EV", "charging", "battery"],
                "sentiment": 0.45,
                "post_ids": ["1", "3", "5"]
            },
            ...
        ]
    }
    """
    data = request.get_json()
    
    if not data or 'posts' not in data:
        return jsonify({'error': 'Missing posts in request body'}), 400
    
    posts = data['posts']
    
    if len(posts) == 0:
        return jsonify({'clusters': []})
    
    # TODO: Implement actual clustering in Sprint 2
    # For now, return a placeholder response
    return jsonify({
        'clusters': [],
        'message': 'ML analysis not yet implemented'
    })


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    debug = os.environ.get('FLASK_DEBUG', '0') == '1'
    app.run(host='0.0.0.0', port=port, debug=debug)
