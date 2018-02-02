package org.artifactory.build;

import java.io.Serializable;
import java.util.Date;

/**
 * A basic build id info holder
 *
 * @author Shay Bagants
 */
public interface BuildId extends Serializable {

    String getName();

    String getNumber();

    String getStarted();

    Date getStartedDate();
}
