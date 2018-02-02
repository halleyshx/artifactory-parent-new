package org.artifactory.storage.fs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author gidis
 */
public interface VfsMutableItemProvider {
    @Nullable
    MutableVfsItem getMutableFsItem();

    @Nonnull
    MutableVfsItem getOrCreateMutableFsItem(boolean b);
}
