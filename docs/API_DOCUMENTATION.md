# API Documentation

This project uses **OpenAPI/Swagger** for interactive API documentation across all services.

## Accessing the Documentation

### ML Engine (Python/Flask)
- **Swagger UI**: http://localhost:5000/apidocs/
- **OpenAPI JSON**: http://localhost:5000/apispec.json

### Backend (Grails/Spring Boot)
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

## Architecture

The API documentation is configured as follows:

| Service | Framework | Documentation Tool |
|---------|-----------|-------------------|
| ML Engine | Flask | flasgger |
| Backend | Grails 6 / Spring Boot | springdoc-openapi |

## ML Engine Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |
| `/api/analyze` | POST | Analyze posts for clustering and sentiment |
| `/api/sentiment` | POST | Analyze sentiment only (no clustering) |

## Backend Endpoints

### System
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/health` | GET | Health check with dependency status |

### Posts
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/posts` | GET | List posts with filtering |
| `/api/posts` | POST | Create a new post |
| `/api/posts/{id}` | GET | Get a single post |
| `/api/posts/{id}` | DELETE | Delete a post |
| `/api/posts/sources` | GET | List sources with counts |

### Clusters
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/clusters` | GET | List clusters with sample posts |
| `/api/clusters/{id}` | GET | Get cluster with all posts |
| `/api/clusters/summary` | GET | Dashboard summary |

### Analysis
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/analysis` | GET | List analysis runs |
| `/api/analysis/{id}` | GET | Get analysis run details |
| `/api/analysis/trigger` | POST | Trigger ML analysis |
| `/api/analysis/fixture-count` | GET | Get available fixture count |
| `/api/analysis/load-fixtures` | POST | Load sample data |
| `/api/analysis/clear` | DELETE | Clear all data |

### Authentication
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/login` | POST | Login and get JWT token |
| `/api/auth/register` | POST | Register new user |
| `/api/auth/me` | GET | Get current user info |
| `/api/auth/logout` | POST | Logout |
| `/api/auth/promote` | POST | Promote user to admin |

### Data Ingestion
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/ingestion/status` | GET | Get ingestion status |
| `/api/ingestion/scrapeAll` | POST | Scrape all sources |
| `/api/ingestion/scrape/{source}` | POST | Scrape specific source |
| `/api/ingestion/import` | POST | Manual JSON import |
| `/api/ingestion/clean-content` | POST | Clean corrupted post content |

### AI Insights
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/insights` | GET | Get cached AI insights |
| `/api/insights/generate` | POST | Generate new insights |

## Adding New Endpoints

### ML Engine (Python)
Add a YAML docstring to your Flask route:

```python
@app.route('/api/example', methods=['POST'])
def example():
    """Example endpoint
    ---
    tags:
      - Example
    parameters:
      - in: body
        name: body
        required: true
        schema:
          type: object
          properties:
            field:
              type: string
    responses:
      200:
        description: Success
    """
    pass
```

### Backend (Groovy)
Since Grails controllers don't use Spring MVC annotations, endpoints are defined programmatically in `backend/src/main/groovy/backend/config/OpenApiConfig.groovy`:

```groovy
// In the buildPaths() method, add a new path item:
paths.addPathItem("/api/example", new PathItem()
    .post(opSecure("Example endpoint", "Example", "Description of the endpoint")
        .requestBody(jsonBody())
        .responses(responses200("Success response"))))
```

Key helper methods available:
- `op(summary, tag, description)` - Create an operation
- `opSecure(summary, tag, description)` - Create an operation requiring JWT auth
- `pathParam(name, desc)` - Add a path parameter
- `queryParam(name, desc)` - Add a query parameter
- `jsonBody()` - Add a JSON request body
- `responses200(desc)` / `responses201(desc)` / `responses204()` - Standard responses

## Exporting OpenAPI Specs

To export the OpenAPI specification for external use:

```bash
# ML Engine
curl http://localhost:5000/apispec.json > docs/openapi-ml-engine.json

# Backend
curl http://localhost:8080/api-docs > docs/openapi-backend.json
```

## Configuration

### ML Engine
Configuration is in `ml-engine/app/api.py`:
- `swagger_config`: UI and spec route settings
- `swagger_template`: API metadata (title, version, description)

### Backend
Configuration is in `backend/grails-app/conf/application.yml`:
```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
  packages-to-scan: sentiment
  paths-to-match: /api/**
```

OpenAPI metadata is in `backend/src/main/groovy/backend/config/OpenApiConfig.groovy`.
