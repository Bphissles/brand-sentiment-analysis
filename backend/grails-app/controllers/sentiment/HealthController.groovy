package sentiment

/**
 * Health check controller for API status
 */
class HealthController {

    static responseFormats = ['json']

    MlEngineService mlEngineService

    /**
     * GET /api/health
     * Returns health status of backend and connected services
     */
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
