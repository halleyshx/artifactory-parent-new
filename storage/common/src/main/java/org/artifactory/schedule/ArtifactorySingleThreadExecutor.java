/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.schedule;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.artifactory.concurrent.ArtifactoryRunnable;
import org.artifactory.storage.spring.ArtifactoryStorageContext;
import org.artifactory.storage.spring.StorageContextHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.*;

/**
 * Fixed size execution service with bounded authentication
 *
 * @author Shay Yaakov
 */
public class ArtifactorySingleThreadExecutor extends AbstractExecutorService {

    private final ArtifactoryStorageContext storageContext;
    private final ExecutorService executor;

    public ArtifactorySingleThreadExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("art-fixed-%s").build();
        executor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory);
        storageContext = StorageContextHelper.get();
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.isShutdown();
    }

    @Override
    public void execute(Runnable task) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        task = new ArtifactoryRunnable(task, storageContext, authentication);
        executor.execute(task);
    }
}
