package sentiment

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

/**
 * Unit tests for HealthController
 * Tests health check endpoint
 */
class HealthControllerSpec extends Specification implements ControllerUnitTest<HealthController> {

    MlEngineService mlEngineService = Mock()

    def setup() {
        controller.mlEngineService = mlEngineService
    }

    void "test health check with ML engine healthy"() {
        given: "ML engine is healthy"
        mlEngineService.isHealthy() >> true

        when: "health endpoint is called"
        controller.index()

        then: "response indicates healthy status"
        response.status == 200
        response.json.status == 'healthy'
        response.json.service == 'grails-backend'
        response.json.mlEngineStatus == 'healthy'
        response.json.dependencies.mlEngine == 'healthy'
    }

    void "test health check with ML engine unavailable"() {
        given: "ML engine is unavailable"
        mlEngineService.isHealthy() >> false

        when: "health endpoint is called"
        controller.index()

        then: "response indicates ML engine unavailable"
        response.status == 200
        response.json.status == 'healthy'
        response.json.mlEngineStatus == 'unavailable'
        response.json.dependencies.mlEngine == 'unavailable'
    }
}
