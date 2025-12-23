"""
Sentiment analysis module
Uses Google Gemini for context-aware sentiment scoring
Falls back to NLTK VADER if Gemini is unavailable
"""
import logging
from typing import List, Dict
import nltk
from nltk.sentiment.vader import SentimentIntensityAnalyzer

# Import Gemini sentiment module
try:
    from sentiment_gemini import (
        analyze_sentiment_gemini, 
        analyze_posts_sentiment_gemini,
        is_gemini_available
    )
    GEMINI_IMPORTED = True
except ImportError:
    GEMINI_IMPORTED = False

logger = logging.getLogger(__name__)

# Download VADER lexicon if not present
try:
    nltk.data.find('sentiment/vader_lexicon.zip')
except LookupError:
    nltk.download('vader_lexicon', quiet=True)

# Initialize VADER analyzer (singleton)
_sia = None

# Custom lexicon updates for trucking/automotive domain
# Positive values = positive sentiment, negative values = negative sentiment
CUSTOM_LEXICON = {
    # Positive slang/expressions commonly used positively
    'insane': 2.5,      # "insane torque" = very positive
    'beast': 2.0,       # "this truck is a beast" = positive
    'killer': 1.5,      # "killer performance" = positive
    'sick': 1.5,        # "sick truck" = positive (slang)
    'monster': 1.5,     # "monster power" = positive
    'badass': 2.0,      # positive slang
    'smooth': 1.5,      # "smooth ride" = positive
    'quiet': 1.5,       # "so quiet" = positive for EVs
    'torque': 0.5,      # generally positive in truck context
    'reliable': 2.0,    # very positive
    'uptime': 1.5,      # positive for fleet operators
    
    # Negative terms in trucking context
    'breakdown': -2.5,  # very negative
    'derate': -2.5,     # engine derate = very negative
    'downtime': -2.0,   # negative for fleet
    'stranded': -2.5,   # very negative
    'waiting': -1.0,    # negative (waiting for parts, etc.)
    'backlog': -1.5,    # negative (order backlog)
    'delay': -1.5,      # negative
    'desert': -0.5,     # "charging desert" context
    
    # Neutral/context-dependent - reduce extreme scores
    'not': -0.5,        # negation (VADER handles this but reinforce)
}

def _get_analyzer() -> SentimentIntensityAnalyzer:
    """Get or create VADER analyzer singleton with custom lexicon"""
    global _sia
    if _sia is None:
        _sia = SentimentIntensityAnalyzer()
        # Update lexicon with domain-specific terms
        _sia.lexicon.update(CUSTOM_LEXICON)
    return _sia


def analyze_sentiment(text: str) -> Dict:
    """
    Analyze sentiment of a single text
    Uses Gemini if available, falls back to VADER
    
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
    if not text or not text.strip():
        return {
            'compound': 0.0,
            'positive': 0.0,
            'negative': 0.0,
            'neutral': 1.0
        }
    
    # Try Gemini first for better context understanding
    if GEMINI_IMPORTED and is_gemini_available():
        result = analyze_sentiment_gemini(text)
        if result:
            return result
    
    # Fallback to VADER
    return _analyze_sentiment_vader(text)


def _analyze_sentiment_vader(text: str) -> Dict:
    """
    Analyze sentiment using VADER (fallback method)
    """
    sia = _get_analyzer()
    scores = sia.polarity_scores(text)
    
    return {
        'compound': scores['compound'],
        'positive': scores['pos'],
        'negative': scores['neg'],
        'neutral': scores['neu']
    }


def analyze_posts_sentiment(posts: List[dict]) -> List[dict]:
    """
    Add sentiment scores to a list of posts
    Uses Gemini batch processing if available, falls back to VADER
    
    Args:
        posts: List of post dictionaries with 'content' field
        
    Returns:
        Posts with added 'sentiment' field
    """
    # Try Gemini batch processing first (more efficient)
    if GEMINI_IMPORTED and is_gemini_available():
        logger.info(f"Using Gemini for sentiment analysis of {len(posts)} posts")
        result = analyze_posts_sentiment_gemini(posts)
        if result:
            return result
        logger.warning("Gemini batch analysis failed, falling back to VADER")
    
    # Fallback to VADER (individual processing)
    logger.info(f"Using VADER for sentiment analysis of {len(posts)} posts")
    results = []
    
    for post in posts:
        content = post.get('content', '')
        sentiment = _analyze_sentiment_vader(content)
        
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
