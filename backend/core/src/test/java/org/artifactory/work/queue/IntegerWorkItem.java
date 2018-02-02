package org.artifactory.work.queue;

import org.artifactory.api.repo.WorkItem;

import javax.annotation.Nonnull;

/**
 * @author Yossi Shaul
 */
class IntegerWorkItem implements WorkItem {
    private Integer value;

    public IntegerWorkItem(int value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "value=" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntegerWorkItem that = (IntegerWorkItem) o;

        return value != null ? value.equals(that.value) : that.value == null;

    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    @Nonnull
    public String getUniqueKey() {
        return String.valueOf(value);
    }
}
