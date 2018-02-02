package org.artifactory.addon.ha.propagation;

import javax.annotation.Nullable;

/**
 * Created by Yinon Avraham.
 */
public interface PropagationResult<T> {

    int getStatusCode();

    @Nullable
    String getErrorMessage();

    T getContent();

}
