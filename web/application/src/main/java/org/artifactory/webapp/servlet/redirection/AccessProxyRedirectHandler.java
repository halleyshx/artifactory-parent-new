package org.artifactory.webapp.servlet.redirection;

import org.apache.commons.lang.StringUtils;
import org.jfrog.client.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AccessProxyRedirectHandler implements RedirectionHandler {
    private static final Logger log = LoggerFactory.getLogger(AccessProxyRedirectHandler.class);

    private static final String PATH_ACCESS_PROXY_ROOT_PREFIX = "artifactory/api/access/";
    private static final String PATH_ACCESS_CONTEXT = "/access";

    @Override
    public boolean shouldRedirect(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();
        path = PathUtils.trimLeadingSlashes(path);
        path = path.toLowerCase();
        return path.startsWith(PATH_ACCESS_PROXY_ROOT_PREFIX);
    }

    @Override
    public void redirect(ServletRequest req, ServletResponse resp) {
        try {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) resp;
            String path = request.getRequestURI();
            String targetUrl = StringUtils.replace(path, PATH_ACCESS_PROXY_ROOT_PREFIX, "");
            ServletContext accessContext = request.getServletContext().getContext(PATH_ACCESS_CONTEXT);
            RequestDispatcher dispatcher = accessContext.getRequestDispatcher(targetUrl);
            dispatcher.forward(request, response);
        } catch (Exception e) {
            log.error("Failed to redirect Access Proxy request.", e);
        }
    }
}
