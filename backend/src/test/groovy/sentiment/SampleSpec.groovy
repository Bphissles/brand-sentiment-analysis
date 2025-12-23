package sentiment

import spock.lang.Specification

/**
 * Sample test to verify testing framework is working
 * Replace with actual tests for your services and controllers
 */
class SampleSpec extends Specification {

    void "test basic assertion"() {
        expect:
        1 + 1 == 2
    }

    void "test string operations"() {
        given:
        def text = "Peterbilt"

        expect:
        text.length() == 9
        text.toLowerCase() == "peterbilt"
    }
}
