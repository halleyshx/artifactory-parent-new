package org.artifactory.api.download;

import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * Represents a ready-to-go folder download stream. created ease the use of StreamingOutput with the validation process
 * (exceptions thrown inside the stream's write() method arrive to late to act upon with the Response).
 *
 * @author Dan Feldman
 */
public interface FolderDownloadResult extends Consumer<OutputStream> {

}
