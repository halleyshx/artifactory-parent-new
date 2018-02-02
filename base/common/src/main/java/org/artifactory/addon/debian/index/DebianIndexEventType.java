package org.artifactory.addon.debian.index;

/**
 * It's here because repomd isn't visible from here and the {@link DebianCalculationEvent} has to be here
 * for HA to see it.
 *
 * @author Dan Feldman
 */
public enum DebianIndexEventType {

    ADD, DELETE, OVERRIDE, FORCED, ALL

}
