package org.artifactory.repo;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * Redirect strategy that supports 308 Permanent Redirect status code
 *
 * @author nadavy
 */
public class JFrogRedirectStrategy extends DefaultRedirectStrategy {

    private static final int REDIRECT_PERMANENTLY = 308;

    @Override
    public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)
            throws ProtocolException {
        return super.isRedirected(request, response, context) ||
                (response.getStatusLine().getStatusCode() == REDIRECT_PERMANENTLY &&
                        this.isRedirectable(request.getRequestLine().getMethod()));
    }
}
