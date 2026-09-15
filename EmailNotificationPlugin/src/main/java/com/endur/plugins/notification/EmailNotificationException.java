package com.endur.plugins.notification;

/**
 * Checked exception raised for any failure encountered while preparing or
 * sending an email notification (invalid config, SMTP failure, invalid
 * recipient list, attachment errors, etc.).
 *
 * Kept as a single exception type deliberately, so calling Endur plug-ins
 * only need to catch/handle one exception type when integrating with this
 * reusable component.
 */
public class EmailNotificationException extends Exception {

    private static final long serialVersionUID = 1L;

    public EmailNotificationException(String message) {
        super(message);
    }

    public EmailNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
