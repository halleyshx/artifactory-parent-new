package org.artifactory.rest.common.access;

import com.google.common.collect.Sets;
import com.sun.jersey.spi.container.ContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * REST URI pattern matcher based on a scope token in the format:
 * <p><tt>&lt;prefix&gt;:&lt;path-pattern&gt;</tt></p>
 * <p>Where:<ul>
 * <li><tt>&lt;prefix&gt;</tt> is one of the predefined prefixes: <tt>api</tt>, <tt>ui</tt><br></li>
 * <li><tt>&lt;path-pattern&gt;</tt> is the path to match (after the prefix). The pattern supports the <tt>'*'</tt> wildcard.</li>
 * </ul>
 * </p>
 * <p>For example:<br>
 * The scope token <tt>"api:*"</tt> will match paths such as <tt>"/api/foo/bar"</tt> but not <tt>"/ui/foo/bar"</tt><br>
 * The scope token <tt>"api:foo/*"</tt> will match paths such as <tt>"/api/foo/bar"</tt> but not <tt>"/api/bar"</tt><br>
 * The scope token <tt>"api:foo/bar"</tt> will match only the path <tt>"/api/foo/bar"</tt>
 * </p>
 * <p>Usage:<pre>
 * ContainerRequest request = ...
 * RestPathScopePattern pattern = RestPathScopePattern.parse("api:foo/*");
 * if (pattern.matches(request)) {
 *     //do something
 * }
 * </pre></p>
 * @author Yinon Avraham.
 */
class RestPathScopePattern {

    private static final Logger log = LoggerFactory.getLogger(RestPathScopePattern.class);

    private static final Set<String> PATH_PREFIXES = Collections.unmodifiableSet(Sets.newHashSet("api", "ui"));
    private static final Pattern PATH_PATTERN_GROUPS = Pattern.compile("(\\*)|([^*]+)");

    private final String prefix;
    private final Pattern pattern;

    private RestPathScopePattern(@Nonnull String prefix, @Nonnull Pattern pattern) {
        this.prefix = requireNonNull(prefix, "prefix is required");
        this.pattern = requireNonNull(pattern, "pattern is required");
    }

    /**
     * Check whether a given request URI matches this pattern
     * @param request the request to check
     * @return <tt>true</tt> if the request matches, <tt>false</tt> otherwise.
     */
    boolean matches(ContainerRequest request) {
        String basePath = request.getBaseUri().getPath();
        String path = request.getPath();
        boolean result = basePath.endsWith("/" + prefix + "/") && pattern.matcher(path).matches();
        log.debug("Matching request '{}' with path scope pattern '{}:{}', result: {}",
                request.getAbsolutePath(), prefix, pattern, result);
        return result;
    }

    /**
     * Parse the scope token and compile a REST path pattern
     * @param scopeToken the scope token to parse
     * @return the compiled pattern
     * @throws IllegalArgumentException if the given scope token could not be parsed
     */
    @Nonnull
    static RestPathScopePattern parse(@Nonnull String scopeToken) {
        return parseOptional(scopeToken)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Could not parse scope token '" + scopeToken + "' as a path pattern"));
    }

    /**
     * Parse the scope token and compile a REST path pattern
     * @param scopeToken the scope token to parse
     * @return an optional with the compiled pattern or an empty optional if the scope token was not a REST path pattern
     */
    @Nonnull
    static Optional<RestPathScopePattern> parseOptional(@Nonnull String scopeToken) {
        int colonIndex = scopeToken.indexOf(":");
        if (colonIndex > 0) {
            String prefix = scopeToken.substring(0, colonIndex);
            if (PATH_PREFIXES.contains(prefix)) {
                try {
                    Pattern pattern = compilePathPattern(scopeToken.substring((prefix + ":").length()));
                    RestPathScopePattern pathScopePattern = new RestPathScopePattern(prefix, pattern);
                    return Optional.of(pathScopePattern);
                } catch (Exception e) {
                    log.debug("Failed to compile path pattern from scope token '{}'.", scopeToken, e);
                }
            }
        }
        return Optional.empty();
    }

    private static Pattern compilePathPattern(String pattern) {
        StringBuilder regex = new StringBuilder("^");
        Matcher matcher = PATH_PATTERN_GROUPS.matcher(pattern);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                regex.append(".*");
            } else if (matcher.group(2) != null) {
                regex.append(Pattern.quote(matcher.group(2)));
            } else {
                throw new IllegalStateException("Unexpected state - check the pattern");
            }
        }
        regex.append("$");
        return Pattern.compile(regex.toString());
    }
}
