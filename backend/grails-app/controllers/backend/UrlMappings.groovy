package backend

class UrlMappings {
    static mappings = {
        
        // ===================
        // REST API Endpoints
        // ===================
        
        // Posts API
        "/api/posts"(controller: 'post') {
            action = [GET: 'index', POST: 'save']
        }
        "/api/posts/$id"(controller: 'post') {
            action = [GET: 'show', DELETE: 'delete']
        }
        "/api/posts/sources"(controller: 'post', action: 'sources')
        
        // Clusters API
        "/api/clusters"(controller: 'cluster', action: 'index')
        "/api/clusters/summary"(controller: 'cluster', action: 'summary')
        "/api/clusters/$id"(controller: 'cluster', action: 'show')
        
        // Analysis API
        "/api/analysis"(controller: 'analysis', action: 'index', method: 'GET')
        "/api/analysis/trigger"(controller: 'analysis', action: 'trigger', method: 'POST')
        "/api/analysis/load-fixtures"(controller: 'analysis', action: 'loadFixtures', method: 'POST')
        "/api/analysis/clear"(controller: 'analysis', action: 'clear', method: 'DELETE')
        "/api/analysis/$id"(controller: 'analysis', action: 'show', method: 'GET')
        
        // Health check
        "/api/health"(controller: 'health', action: 'index')

        // Data Ingestion API (Gemini web scraping)
        "/api/ingestion/status"(controller: 'dataIngestion', action: 'status')
        "/api/ingestion/scrapeAll"(controller: 'dataIngestion', action: 'scrapeAll')
        "/api/ingestion/scrape/$source"(controller: 'dataIngestion', action: 'scrapeSource')
        "/api/ingestion/import"(controller: 'dataIngestion', action: 'manualImport')

        // Authentication API
        "/api/auth/login"(controller: 'auth', action: 'login')
        "/api/auth/register"(controller: 'auth', action: 'register')
        "/api/auth/me"(controller: 'auth', action: 'me')
        "/api/auth/logout"(controller: 'auth', action: 'logout')
        "/api/auth/promote"(controller: 'auth', action: 'promoteToAdmin')

        // Default mappings
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
