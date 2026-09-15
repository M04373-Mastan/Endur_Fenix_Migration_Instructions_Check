package com.endur.plugins.notification;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Represents a single email attachment.
 *
 * Content can be supplied either as raw bytes (e.g. a generated report,
 * an Endur-exported PDF/Excel, an in-memory CSV, etc.) or the class also
 * offers a convenience static factory to load from a file path on the
 * Endur application server.
 */
public final class EmailAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final byte[] content;
    private final String mimeType;

    public EmailAttachment(String fileName, byte[] content, String mimeType) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Attachment file name must not be empty");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Attachment content must not be empty");
        }
        this.fileName = fileName;
        this.content = Arrays.copyOf(content, content.length);
        this.mimeType = (mimeType == null || mimeType.trim().isEmpty())
                ? "application/octet-stream"
                : mimeType;
    }

    public static EmailAttachment fromFile(java.io.File file) throws EmailNotificationException {
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            String mime = java.nio.file.Files.probeContentType(file.toPath());
            return new EmailAttachment(file.getName(), bytes, mime);
        } catch (java.io.IOException e) {
            throw new EmailNotificationException("Unable to read attachment file: " + file.getAbsolutePath(), e);
        }
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getContent() {
        return Arrays.copyOf(content, content.length);
    }

    public String getMimeType() {
        return mimeType;
    }
}
