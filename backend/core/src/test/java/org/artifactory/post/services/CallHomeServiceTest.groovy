package org.artifactory.post.services

import spock.lang.Specification
import spock.lang.Unroll

class CallHomeServiceTest extends Specification {

    @Unroll
    def "check call home account name"() {
        given:
        def callHomeService = new CallHomeService()

        expect:
        callHomeService.getAccountName(accountUrl) == expectedAccountName


        where:
        accountUrl                                   | expectedAccountName
        "https://bezirk.jfrog.io/bezirk/webapp/"     | "bezirk"
        "https://localhost:8080/artifactory/webapp/" | "N/A"
        null                                         | "N/A"
    }
}
