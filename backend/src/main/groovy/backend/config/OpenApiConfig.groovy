package backend.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI/Swagger configuration for the Backend API
 * Grails controllers don't use Spring MVC annotations, so we define the spec programmatically
 */
@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Brand Sentiment Analyzer API")
                .description("Backend API for the Brand Sentiment Analyzer - manages posts, clusters, analysis, and authentication")
                .version("1.0.0")
                .contact(new Contact().name("API Support")))
            .servers([
                new Server().url("http://localhost:8080").description("Local development")
            ])
            .tags([
                new Tag().name("System").description("Health and status endpoints"),
                new Tag().name("Authentication").description("User authentication and authorization"),
                new Tag().name("Posts").description("Social media post management"),
                new Tag().name("Clusters").description("Topic cluster management"),
                new Tag().name("Analysis").description("ML analysis and processing"),
                new Tag().name("Data Ingestion").description("Web scraping and data import"),
                new Tag().name("Insights").description("AI-generated insights")
            ])
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
            .paths(buildPaths())
    }

    private Paths buildPaths() {
        def paths = new Paths()

        // ===== System =====
        paths.addPathItem("/api/health", new PathItem()
            .get(op("Health check", "System", "Returns health status of backend and connected services")
                .responses(responses200("Health status"))))

        // ===== Authentication =====
        paths.addPathItem("/api/auth/login", new PathItem()
            .post(op("Login", "Authentication", "Authenticate user and return JWT token")
                .requestBody(jsonBody())
                .responses(responses200("Login successful").addApiResponse("401", resp("Invalid credentials")))))

        paths.addPathItem("/api/auth/register", new PathItem()
            .post(op("Register", "Authentication", "Register a new user account")
                .requestBody(jsonBody())
                .responses(responses200("Registration successful")
                    .addApiResponse("400", resp("Invalid input"))
                    .addApiResponse("409", resp("Email already registered")))))

        paths.addPathItem("/api/auth/me", new PathItem()
            .get(opSecure("Get current user", "Authentication", "Get current user info from JWT token")
                .responses(responses200("User info").addApiResponse("401", resp("Invalid or missing token")))))

        paths.addPathItem("/api/auth/logout", new PathItem()
            .post(op("Logout", "Authentication", "Logout (client-side token removal)")
                .responses(responses200("Logout successful"))))

        paths.addPathItem("/api/auth/promote", new PathItem()
            .post(opSecure("Promote to admin", "Authentication", "Promote a user to admin role")
                .requestBody(jsonBody())
                .responses(responses200("User promoted")
                    .addApiResponse("403", resp("Admin access required"))
                    .addApiResponse("404", resp("User not found")))))

        // ===== Posts =====
        paths.addPathItem("/api/posts", new PathItem()
            .get(opSecure("List posts", "Posts", "List all posts with optional filtering")
                .addParametersItem(queryParam("source", "Filter by source"))
                .addParametersItem(queryParam("clusterId", "Filter by cluster ID"))
                .addParametersItem(queryParam("sentimentLabel", "Filter by sentiment (positive/neutral/negative)"))
                .addParametersItem(queryParam("max", "Maximum results (default 100)"))
                .addParametersItem(queryParam("offset", "Pagination offset"))
                .responses(responses200("Paginated list of posts")))
            .post(opSecure("Create post", "Posts", "Create a new social media post")
                .requestBody(jsonBody())
                .responses(responses201("Post created").addApiResponse("400", resp("Validation error")))))

        paths.addPathItem("/api/posts/{id}", new PathItem()
            .get(opSecure("Get post", "Posts", "Get a single post by ID")
                .addParametersItem(pathParam("id", "Post ID"))
                .responses(responses200("Post details").addApiResponse("404", resp("Post not found"))))
            .delete(opSecure("Delete post", "Posts", "Delete a post by ID")
                .addParametersItem(pathParam("id", "Post ID"))
                .responses(responses204().addApiResponse("404", resp("Post not found")))))

        paths.addPathItem("/api/posts/sources", new PathItem()
            .get(opSecure("List sources", "Posts", "Get available source types with post counts")
                .responses(responses200("List of sources with counts"))))

        // ===== Clusters =====
        paths.addPathItem("/api/clusters", new PathItem()
            .get(opSecure("List clusters", "Clusters", "List all clusters with optional filtering")
                .addParametersItem(queryParam("analysisRunId", "Filter by analysis run"))
                .addParametersItem(queryParam("sentimentLabel", "Filter by sentiment"))
                .addParametersItem(queryParam("source", "Filter by post source"))
                .responses(responses200("List of clusters with sample posts"))))

        paths.addPathItem("/api/clusters/{id}", new PathItem()
            .get(opSecure("Get cluster", "Clusters", "Get a single cluster with all its posts")
                .addParametersItem(pathParam("id", "Cluster ID"))
                .responses(responses200("Cluster details with posts").addApiResponse("404", resp("Cluster not found")))))

        paths.addPathItem("/api/clusters/summary", new PathItem()
            .get(opSecure("Dashboard summary", "Clusters", "Get dashboard summary with sentiment distribution")
                .addParametersItem(queryParam("source", "Filter by source"))
                .responses(responses200("Dashboard summary data"))))

        // ===== Analysis =====
        paths.addPathItem("/api/analysis", new PathItem()
            .get(opSecure("List analysis runs", "Analysis", "List all analysis runs")
                .responses(responses200("List of analysis runs"))))

        paths.addPathItem("/api/analysis/{id}", new PathItem()
            .get(opSecure("Get analysis run", "Analysis", "Get a single analysis run by ID")
                .addParametersItem(pathParam("id", "Analysis run ID"))
                .responses(responses200("Analysis run details").addApiResponse("404", resp("Analysis run not found")))))

        paths.addPathItem("/api/analysis/trigger", new PathItem()
            .post(opSecure("Trigger analysis", "Analysis", "Trigger a new ML analysis on all posts")
                .responses(responses200("Analysis completed").addApiResponse("500", resp("Analysis failed")))))

        paths.addPathItem("/api/analysis/load-fixtures", new PathItem()
            .post(opSecure("Load fixtures", "Analysis", "Load sample data from fixtures into database")
                .responses(responses200("Fixtures loaded").addApiResponse("500", resp("Failed to load fixtures")))))

        paths.addPathItem("/api/analysis/clear", new PathItem()
            .delete(opSecure("Clear data", "Analysis", "Clear all posts and clusters (for testing)")
                .responses(responses200("Data cleared"))))

        // ===== Data Ingestion =====
        paths.addPathItem("/api/ingestion/status", new PathItem()
            .get(opSecure("Ingestion status", "Data Ingestion", "Get the current status of data ingestion")
                .responses(responses200("Ingestion status"))))

        paths.addPathItem("/api/ingestion/scrapeAll", new PathItem()
            .post(opSecure("Scrape all sources", "Data Ingestion", "Scrape all configured sources and import posts")
                .responses(responses200("Scraping completed")
                    .addApiResponse("400", resp("Gemini API not configured"))
                    .addApiResponse("409", resp("Scraping already in progress")))))

        paths.addPathItem("/api/ingestion/scrape/{source}", new PathItem()
            .post(opSecure("Scrape source", "Data Ingestion", "Scrape a specific source (twitter, youtube, forums)")
                .addParametersItem(pathParam("source", "Source name"))
                .responses(responses200("Scraping completed").addApiResponse("400", resp("Invalid source")))))

        paths.addPathItem("/api/ingestion/import", new PathItem()
            .post(opSecure("Manual import", "Data Ingestion", "Import posts from JSON body")
                .requestBody(jsonBody())
                .responses(responses200("Import successful")
                    .addApiResponse("400", resp("Missing posts array"))
                    .addApiResponse("500", resp("Import failed")))))

        // ===== Insights =====
        paths.addPathItem("/api/insights", new PathItem()
            .get(opSecure("Get insights", "Insights", "Get cached AI insights for the current analysis")
                .addParametersItem(queryParam("source", "Filter by source (default: all)"))
                .responses(responses200("AI insights"))))

        paths.addPathItem("/api/insights/generate", new PathItem()
            .post(opSecure("Generate insights", "Insights", "Generate new AI insights (clears cache)")
                .addParametersItem(queryParam("source", "Source to analyze (default: all)"))
                .responses(responses200("Generated insights").addApiResponse("500", resp("Failed to generate")))))

        return paths
    }

    // Helper methods
    private Operation op(String summary, String tag, String description) {
        new Operation().summary(summary).addTagsItem(tag).description(description)
    }

    private Operation opSecure(String summary, String tag, String description) {
        op(summary, tag, description).addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
    }

    private Parameter pathParam(String name, String desc) {
        new Parameter().name(name).in("path").required(true).description(desc)
            .schema(new Schema().type("string"))
    }

    private Parameter queryParam(String name, String desc) {
        new Parameter().name(name).in("query").required(false).description(desc)
            .schema(new Schema().type("string"))
    }

    private RequestBody jsonBody() {
        new RequestBody().content(new Content().addMediaType("application/json",
            new MediaType().schema(new Schema().type("object"))))
    }

    private ApiResponse resp(String desc) {
        new ApiResponse().description(desc)
    }

    private ApiResponses responses200(String desc) {
        new ApiResponses().addApiResponse("200", resp(desc))
    }

    private ApiResponses responses201(String desc) {
        new ApiResponses().addApiResponse("201", resp(desc))
    }

    private ApiResponses responses204() {
        new ApiResponses().addApiResponse("204", resp("No content"))
    }
}
