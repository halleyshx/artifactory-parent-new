package org.artifactory.converter.postinit

import spock.lang.Specification
import spock.lang.Unroll

class ConanPathConverterTest extends Specification {

    @Unroll
    def "check fix conan path"() {
        given:
        ConanRepoPathConverter converter = new ConanRepoPathConverter()

        expect:
        converter.fixConanPath(path) == fixedPath

        where:
        path                             | fixedPath
        "pkg/version/user/channel"       | "user/pkg/version/channel"
        "pkg/version/user/channel/a/b/c" | "user/pkg/version/channel/a/b/c"
    }

    @Unroll
    def "check fail to fix conan path"() {
        given:
        ConanRepoPathConverter converter = new ConanRepoPathConverter()

        expect:
        converter.fixConanPath("path/too/short/" ) == null
    }
}
