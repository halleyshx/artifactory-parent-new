package org.artifactory.api.build;

import org.artifactory.common.StatusEntry;

import java.util.Comparator;

/**
 * @author Alexei Vainshtein
 */
public class StatusEntryComparators {

    public static Comparator<StatusEntry> sortByStatusCodeImportency() {
        return new BasicStatusHolderByStatusCodeComparator();
    }
    /**
     * Compares builds based on the status code from status holders.
     */
    private static class BasicStatusHolderByStatusCodeComparator implements Comparator<StatusEntry> {

        @Override
        public int compare(StatusEntry statusEntry1, StatusEntry statusEntry2) {
            return ((Integer)statusEntry1.getStatusCode()).compareTo(statusEntry2.getStatusCode());
        }
    }
}
