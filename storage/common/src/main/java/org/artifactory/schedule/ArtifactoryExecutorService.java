package org.artifactory.schedule;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.concurrent.ArtifactoryRunnable;
import org.artifactory.security.AuthenticationHelper;
import org.springframework.security.core.Authentication;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Noam Shemesh
 */
public class ArtifactoryExecutorService extends AbstractExecutorService {
    private final ExecutorService threadPool;
    private Authentication authentication;
    private ArtifactoryContext context;

    public ArtifactoryExecutorService() {
        this(Executors.newCachedThreadPool());
    }

    public ArtifactoryExecutorService(ExecutorService executorService) {
        this(AuthenticationHelper.getAuthentication(), ContextHelper.get(), executorService);
    }

    public ArtifactoryExecutorService(Authentication authentication, ArtifactoryContext context, ExecutorService executorService) {
        this.authentication = authentication;
        this.context = context;
        this.threadPool = executorService;
    }

    @Override
    public void shutdown() {
        threadPool.shutdown();
    }

    @Override
    @Nonnull
    public List<Runnable> shutdownNow() {
        return threadPool.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return threadPool.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return threadPool.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return threadPool.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        threadPool.execute(new ArtifactoryRunnable(command, context, authentication));
    }

    public ArtifactoryExecutorService copyOfWithCurrentThreadLocals() {
        return new ArtifactoryExecutorService(this.threadPool);
    }
}
