"""
Gemini-based sentiment analysis module
Uses Google Gemini API for context-aware sentiment scoring
Falls back to VADER if Gemini is unavailable
"""
import os
import json
import logging
from typing import List, Dict, Optional
from dotenv import load_dotenv

# Load environment variables from root .env file
load_dotenv(os.path.join(os.path.dirname(__file__), '..', '..', '.env'))

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Gemini client singleton
_gemini_model = None
_gemini_available = None

def _init_gemini():
    """Initialize Gemini client if API key is available"""
    global _gemini_model, _gemini_available
    
    if _gemini_available is not None:
        return _gemini_available
    
    api_key = os.environ.get('GEMINI_API_KEY')
    if not api_key:
        logger.warning("GEMINI_API_KEY not set - falling back to VADER for sentiment")
        _gemini_available = False
        return False
    
    try:
        from google import genai
        _gemini_model = genai.Client(api_key=api_key)
        _gemini_available = True
        logger.info("Gemini sentiment analysis initialized successfully")
        return True
    except Exception as e:
        logger.error(f"Failed to initialize Gemini: {e}")
        _gemini_available = False
        return False


def analyze_sentiment_gemini(text: str) -> Optional[Dict]:
    """
    Analyze sentiment using Gemini API
    
    Args:
        text: Text content to analyze
        
    Returns:
        Dictionary with sentiment scores or None if failed
    """
    if not _init_gemini() or not _gemini_model:
        return None
    
    if not text or not text.strip():
        return {
            'compound': 0.0,
            'positive': 0.0,
            'negative': 0.0,
            'neutral': 1.0
        }
    
    prompt = f"""Analyze the sentiment of the following social media post about trucks/vehicles.

Post: "{text}"

Respond with ONLY a JSON object (no markdown, no explanation) in this exact format:
{{"compound": <float from -1.0 to 1.0>, "positive": <float 0-1>, "negative": <float 0-1>, "neutral": <float 0-1>, "label": "<positive|negative|neutral>"}}

Guidelines:
- compound: Overall sentiment score (-1.0 = very negative, 0 = neutral, 1.0 = very positive)
- Consider context: "insane torque" is POSITIVE (slang for amazing)
- Consider negations: "NOT there yet" indicates frustration/negative
- positive/negative/neutral should sum to approximately 1.0
- Be accurate to the true sentiment, not just keyword matching"""

    try:
        response = _gemini_model.models.generate_content(
            model='gemini-2.0-flash',
            contents=prompt
        )
        result_text = response.text.strip()
        
        # Clean up response (remove markdown code blocks if present)
        if result_text.startswith('```'):
            result_text = result_text.split('\n', 1)[1]
            result_text = result_text.rsplit('```', 1)[0]
        result_text = result_text.strip()
        
        result = json.loads(result_text)
        
        # Validate and normalize
        return {
            'compound': float(result.get('compound', 0.0)),
            'positive': float(result.get('positive', 0.0)),
            'negative': float(result.get('negative', 0.0)),
            'neutral': float(result.get('neutral', 0.0))
        }
    except json.JSONDecodeError as e:
        logger.warning(f"Failed to parse Gemini response: {e}")
        return None
    except Exception as e:
        logger.warning(f"Gemini sentiment analysis failed: {e}")
        return None


def analyze_posts_sentiment_gemini(posts: List[dict]) -> List[dict]:
    """
    Analyze sentiment for multiple posts using Gemini with batching
    
    Args:
        posts: List of post dictionaries with 'content' field
        
    Returns:
        Posts with added 'sentiment' field
    """
    if not _init_gemini() or not _gemini_model:
        return None
    
    results = []
    
    # Process in batches to reduce API calls
    batch_size = 5
    for i in range(0, len(posts), batch_size):
        batch = posts[i:i + batch_size]
        batch_results = _analyze_batch(batch)
        
        if batch_results:
            results.extend(batch_results)
        else:
            # Fallback to individual analysis if batch fails
            for post in batch:
                content = post.get('content', '')
                sentiment = analyze_sentiment_gemini(content)
                if sentiment:
                    results.append({**post, 'sentiment': sentiment})
                else:
                    # Return None to trigger VADER fallback
                    return None
    
    return results


def _analyze_batch(posts: List[dict]) -> Optional[List[dict]]:
    """
    Analyze a batch of posts in a single API call
    """
    if not posts:
        return []
    
    # Build batch prompt
    posts_text = "\n".join([
        f'{i+1}. "{p.get("content", "")}"' 
        for i, p in enumerate(posts)
    ])
    
    prompt = f"""Analyze the sentiment of these social media posts about trucks/vehicles.

Posts:
{posts_text}

Respond with ONLY a JSON array (no markdown, no explanation) with one object per post:
[{{"compound": <float -1 to 1>, "positive": <float 0-1>, "negative": <float 0-1>, "neutral": <float 0-1>}}, ...]

Guidelines:
- compound: Overall sentiment (-1.0 = very negative, 0 = neutral, 1.0 = very positive)
- Consider context: "insane torque" is POSITIVE (slang for amazing)
- Consider negations: "NOT there yet" indicates frustration/negative
- Be accurate to true sentiment, not keyword matching"""

    try:
        response = _gemini_model.models.generate_content(
            model='gemini-2.0-flash',
            contents=prompt
        )
        result_text = response.text.strip()
        
        # Clean up response
        if result_text.startswith('```'):
            result_text = result_text.split('\n', 1)[1]
            result_text = result_text.rsplit('```', 1)[0]
        result_text = result_text.strip()
        
        sentiments = json.loads(result_text)
        
        if len(sentiments) != len(posts):
            logger.warning(f"Batch size mismatch: expected {len(posts)}, got {len(sentiments)}")
            return None
        
        results = []
        for post, sentiment in zip(posts, sentiments):
            results.append({
                **post,
                'sentiment': {
                    'compound': float(sentiment.get('compound', 0.0)),
                    'positive': float(sentiment.get('positive', 0.0)),
                    'negative': float(sentiment.get('negative', 0.0)),
                    'neutral': float(sentiment.get('neutral', 0.0))
                }
            })
        
        return results
    except Exception as e:
        logger.warning(f"Batch sentiment analysis failed: {e}")
        return None


def is_gemini_available() -> bool:
    """Check if Gemini API is available"""
    return _init_gemini()
