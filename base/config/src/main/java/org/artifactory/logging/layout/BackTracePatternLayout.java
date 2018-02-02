package org.artifactory.logging.layout;

/**
 * Kept for backward compatibility since existing logback configurations use this fully qualified class name.
 * <p>
 * Keep in mind - the logback configuration file is read before the conversions are executed, so even the converter
 * created for this case does not help...
 * </p>
 *
 * @deprecated Instead you should use {@link org.jfrog.common.logging.logback.layout.BackTracePatternLayout} directly.
 * @author Yinon Avraham
 */
@Deprecated
public class BackTracePatternLayout extends org.jfrog.common.logging.logback.layout.BackTracePatternLayout {
}
