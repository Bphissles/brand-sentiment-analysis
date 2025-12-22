"""
Sentiment analysis module
Uses NLTK VADER for sentiment scoring
"""
from typing import List, Dict


def analyze_sentiment(text: str) -> Dict:
    """
    Analyze sentiment of a single text
    
    Args:
        text: Text content to analyze
        
    Returns:
        Dictionary with sentiment scores:
        {
            'compound': float (-1 to 1),
            'positive': float (0 to 1),
            'negative': float (0 to 1),
            'neutral': float (0 to 1)
        }
    """
    # TODO: Implement with NLTK VADER in Sprint 2
    # from nltk.sentiment.vader import SentimentIntensityAnalyzer
    # sia = SentimentIntensityAnalyzer()
    # return sia.polarity_scores(text)
    
    return {
        'compound': 0.0,
        'positive': 0.0,
        'negative': 0.0,
        'neutral': 1.0
    }


def analyze_posts_sentiment(posts: List[dict]) -> List[dict]:
    """
    Add sentiment scores to a list of posts
    
    Args:
        posts: List of post dictionaries with 'content' field
        
    Returns:
        Posts with added 'sentiment' field
    """
    results = []
    
    for post in posts:
        content = post.get('content', '')
        sentiment = analyze_sentiment(content)
        
        results.append({
            **post,
            'sentiment': sentiment
        })
    
    return results


def aggregate_cluster_sentiment(posts: List[dict]) -> float:
    """
    Calculate aggregate sentiment for a cluster of posts
    
    Args:
        posts: List of posts with 'sentiment' field
        
    Returns:
        Average compound sentiment score (-1 to 1)
    """
    if not posts:
        return 0.0
    
    total = sum(p.get('sentiment', {}).get('compound', 0.0) for p in posts)
    return total / len(posts)


def classify_sentiment(score: float) -> str:
    """
    Classify sentiment score into category
    
    Args:
        score: Compound sentiment score (-1 to 1)
        
    Returns:
        Category string: 'positive', 'negative', or 'neutral'
    """
    if score >= 0.3:
        return 'positive'
    elif score <= -0.3:
        return 'negative'
    else:
        return 'neutral'
