package com.example.studysync_project.utils;

import java.util.UUID;

/**
 * Utility class for generating unique IDs for documents
 */
public class IdUtil {

    /**
     * Generate a unique ID for any document
     */
    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate a document ID with prefix (for better organization)
     */
    public static String generateId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString();
    }
}
