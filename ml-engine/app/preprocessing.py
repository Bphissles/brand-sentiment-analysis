"""
Text preprocessing for ML analysis
Handles tokenization, TF-IDF, and text normalization
"""
import re
from typing import List


# Trucking/Peterbilt-specific stop words to add
DOMAIN_STOP_WORDS = {
    'truck', 'trucks', 'trucker', 'truckers', 'trucking',
    'peterbilt', 'pete', 'paccar',
    'just', 'like', 'got', 'get', 'would', 'could', 'really'
}


def clean_text(text: str) -> str:
    """
    Clean and normalize text for analysis
    
    Args:
        text: Raw text content
        
    Returns:
        Cleaned text string
    """
    if not text:
        return ""
    
    # Convert to lowercase
    text = text.lower()
    
    # Remove URLs
    text = re.sub(r'http\S+|www\S+', '', text)
    
    # Remove mentions and hashtags (keep the text)
    text = re.sub(r'@\w+', '', text)
    text = re.sub(r'#(\w+)', r'\1', text)
    
    # Remove special characters but keep alphanumeric and spaces
    text = re.sub(r'[^a-zA-Z0-9\s]', ' ', text)
    
    # Remove extra whitespace
    text = re.sub(r'\s+', ' ', text).strip()
    
    return text


def tokenize(text: str) -> List[str]:
    """
    Tokenize text into words
    
    Args:
        text: Cleaned text string
        
    Returns:
        List of tokens
    """
    if not text:
        return []
    
    return text.split()


def remove_stop_words(tokens: List[str], custom_stop_words: set = None) -> List[str]:
    """
    Remove stop words from token list
    
    Args:
        tokens: List of word tokens
        custom_stop_words: Additional stop words to remove
        
    Returns:
        Filtered token list
    """
    # Will be populated with NLTK stop words in Sprint 2
    stop_words = DOMAIN_STOP_WORDS.copy()
    
    if custom_stop_words:
        stop_words.update(custom_stop_words)
    
    return [t for t in tokens if t not in stop_words and len(t) > 2]


def preprocess_posts(posts: List[dict]) -> List[dict]:
    """
    Preprocess a list of posts for ML analysis
    
    Args:
        posts: List of post dictionaries with 'content' field
        
    Returns:
        Posts with added 'tokens' and 'cleaned_content' fields
    """
    processed = []
    
    for post in posts:
        content = post.get('content', '')
        cleaned = clean_text(content)
        tokens = tokenize(cleaned)
        filtered_tokens = remove_stop_words(tokens)
        
        processed.append({
            **post,
            'cleaned_content': cleaned,
            'tokens': filtered_tokens
        })
    
    return processed
