package org.artifactory.aql.action;

/**
 * @author Dan Feldman
 */
public class AqlActionException extends Exception {

    private Reason reason;

    public AqlActionException(String msg, Reason reason) {
        super(msg);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        ACTION_FAILED("Action failed"), UNSUPPORTED_FOR_DOMAIN("Action unsupported for domain"),
        UNEXPECTED_CONTENT("Missing required row content");

        public String code;

        Reason(String code) {
            this.code = code;
        }
    }
}
