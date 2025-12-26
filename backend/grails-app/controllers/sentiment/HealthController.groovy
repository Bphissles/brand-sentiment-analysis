package sentiment

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * Health check controller for API status
 */
@Tag(name = "System", description = "System health and status endpoints")
class HealthController {

    static responseFormats = ['json']

    MlEngineService mlEngineService

    /**
     * GET /api/health
     * Returns health status of backend and connected services
     */
    @Operation(
        summary = "Health check",
        description = "Returns health status of backend and connected services"
    )
    @ApiResponse(responseCode = "200", description = "Service health status")
    def index() {
        def mlHealthy = mlEngineService.isHealthy()

        respond([
            status: 'healthy',
            service: 'grails-backend',
            timestamp: new Date().toInstant().toString(),
            mlEngineStatus: mlHealthy ? 'healthy' : 'unavailable',
            dependencies: [
                mlEngine: mlHealthy ? 'healthy' : 'unavailable'
            ]
        ])
    }
}
