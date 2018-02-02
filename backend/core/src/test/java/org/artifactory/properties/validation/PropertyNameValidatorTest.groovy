package org.artifactory.properties.validation

import org.artifactory.exception.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

class PropertyNameValidatorTest extends Specification {

    @Unroll
    def "check validation with ##propName"() {


        when:
        PropertyNameValidator.validate(propName)

        then:
        noExceptionThrown()

        where:
        propName << ["xyz", "xx3df", "latest:build", "ff_gh"]
    }

    @Unroll
    def "fail on bad validation with #badPropName"() {


        when:
        PropertyNameValidator.validate(badPropName)

        then:
        thrown(ValidationException)

        where:
        badPropName << ["x yz", "3ss", "sd!", "w@", "fds#", "fds\$fr", "df^yy", "fg%gbfd", "fd&jj",
                        "kk*kk", " ", "", null, "fs/f", "das\\gfd", "ds~sf"
                        , "ss+s", "sd>f", "ww>w", "oo=o", "hh;h", "fgg,g", "vv±f", "aa§a",
                        "das`fds", "fds[", "fd]fds", "qw{d", "tr}fd", "fds(f", "as)fd", ""]
    }
}
