package org.artifactory.concurrent;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.common.ArtifactoryHome;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Noam Shemesh
 */
public class ArtifactoryRunnable implements Runnable {
    private final Runnable delegate;
    private ArtifactoryContext context;
    private final Authentication authentication;


    public ArtifactoryRunnable(Runnable delegate, ArtifactoryContext context, Authentication authentication) {
        this.delegate = delegate;
        this.context = context;
        this.authentication = authentication;
    }

    @Override
    public void run() {
        try {
            ArtifactoryContextThreadBinder.bind(context);
            ArtifactoryHome.bind(context.getArtifactoryHome());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            delegate.run();
        } finally {
            // in case an async operation is fired while shutdown (i.e gc) the context holder strategy is
            // cleared and NPE can happen after the async finished (or is finishing). see RTFACT-2812
            if (context.isReady()) {
                SecurityContextHolder.clearContext();
            }
            ArtifactoryContextThreadBinder.unbind();
            ArtifactoryHome.unbind();
        }
    }
}
