"""
Data models for ML Engine
Python dataclasses matching the shared schema
"""
from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional, List
from enum import Enum


class SourceType(Enum):
    """Data source types"""
    TWITTER = "twitter"
    YOUTUBE = "youtube"
    FORUMS = "forums"


class SentimentLabel(Enum):
    """Sentiment classification labels"""
    POSITIVE = "positive"
    NEGATIVE = "negative"
    NEUTRAL = "neutral"


@dataclass
class SentimentScore:
    """Sentiment analysis result"""
    compound: float           # Overall score: -1 to 1
    positive: float           # Positive component: 0 to 1
    negative: float           # Negative component: 0 to 1
    neutral: float            # Neutral component: 0 to 1
    label: SentimentLabel = SentimentLabel.NEUTRAL
    
    def to_dict(self) -> dict:
        return {
            "compound": self.compound,
            "positive": self.positive,
            "negative": self.negative,
            "neutral": self.neutral,
            "label": self.label.value
        }
    
    @classmethod
    def from_dict(cls, data: dict) -> "SentimentScore":
        return cls(
            compound=data.get("compound", 0.0),
            positive=data.get("positive", 0.0),
            negative=data.get("negative", 0.0),
            neutral=data.get("neutral", 1.0),
            label=SentimentLabel(data.get("label", "neutral"))
        )


@dataclass
class Post:
    """Raw social media/forum post"""
    id: str
    source: SourceType
    external_id: str
    content: str
    author: str
    published_at: datetime
    
    # Optional fields
    author_url: Optional[str] = None
    post_url: Optional[str] = None
    fetched_at: datetime = field(default_factory=datetime.utcnow)
    
    # ML-generated fields
    sentiment: Optional[SentimentScore] = None
    cluster_id: Optional[str] = None
    keywords: List[str] = field(default_factory=list)
    cleaned_content: Optional[str] = None
    tokens: List[str] = field(default_factory=list)
    
    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "source": self.source.value,
            "externalId": self.external_id,
            "content": self.content,
            "author": self.author,
            "authorUrl": self.author_url,
            "postUrl": self.post_url,
            "publishedAt": self.published_at.isoformat() if self.published_at else None,
            "fetchedAt": self.fetched_at.isoformat() if self.fetched_at else None,
            "sentiment": self.sentiment.to_dict() if self.sentiment else None,
            "clusterId": self.cluster_id,
            "keywords": self.keywords
        }
    
    @classmethod
    def from_dict(cls, data: dict) -> "Post":
        return cls(
            id=data["id"],
            source=SourceType(data["source"]),
            external_id=data.get("externalId", data.get("external_id", "")),
            content=data["content"],
            author=data.get("author", "unknown"),
            author_url=data.get("authorUrl"),
            post_url=data.get("postUrl"),
            published_at=datetime.fromisoformat(data["publishedAt"]) if data.get("publishedAt") else datetime.utcnow(),
            fetched_at=datetime.fromisoformat(data["fetchedAt"]) if data.get("fetchedAt") else datetime.utcnow()
        )


@dataclass
class Cluster:
    """Group of related posts"""
    id: str
    taxonomy_id: str
    label: str
    
    # Aggregated data
    keywords: List[str]
    sentiment: float
    sentiment_label: SentimentLabel
    post_count: int
    post_ids: List[str]
    
    # Optional
    description: Optional[str] = None
    insight: Optional[str] = None
    analysis_run_id: Optional[str] = None
    created_at: datetime = field(default_factory=datetime.utcnow)
    
    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "taxonomyId": self.taxonomy_id,
            "label": self.label,
            "description": self.description,
            "keywords": self.keywords,
            "sentiment": self.sentiment,
            "sentimentLabel": self.sentiment_label.value,
            "postCount": self.post_count,
            "postIds": self.post_ids,
            "insight": self.insight,
            "analysisRunId": self.analysis_run_id,
            "createdAt": self.created_at.isoformat() if self.created_at else None
        }


@dataclass
class AnalysisRequest:
    """Request payload for /api/analyze endpoint"""
    posts: List[dict]
    
    @classmethod
    def from_dict(cls, data: dict) -> "AnalysisRequest":
        return cls(posts=data.get("posts", []))


@dataclass
class AnalysisResponse:
    """Response payload from /api/analyze endpoint"""
    clusters: List[Cluster]
    posts_analyzed: int
    processing_time_ms: int
    
    def to_dict(self) -> dict:
        return {
            "clusters": [c.to_dict() for c in self.clusters],
            "postsAnalyzed": self.posts_analyzed,
            "processingTimeMs": self.processing_time_ms
        }
