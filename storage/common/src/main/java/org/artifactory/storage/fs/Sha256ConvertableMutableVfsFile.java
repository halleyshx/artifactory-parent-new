package org.artifactory.storage.fs;

/**
 * DANGER WILL ROBINSON!! DO NOT use this, its meant only for the sha256 converter.
 *
 * This interface is used to update the sha256 value for existing nodes WHICH YOU SHOULD NEVER DO - the choice was
 * between an extra DB call (using {@link MutableVfsFile#tryUsingExistingBinary} or this interface.
 *
 * As a great leader once said:
 * "History Will Absolve Me" -Fidel Castro
 * @author Dan Feldman
 */
@Deprecated
public interface Sha256ConvertableMutableVfsFile {

    void setSha2(String sha2);
}
