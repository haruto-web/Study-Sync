package com.example.studysync_project.utils;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TextExtractorUtil {

    /**
     * Extracts readable text from a URI (TXT or PDF).
     * For PDF, reads raw bytes and pulls out readable ASCII strings.
     * For TXT/PPT, reads line by line.
     */
    public static String extract(Context context, Uri uri) {
        try {
            String mimeType = context.getContentResolver().getType(uri);
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            if (mimeType != null && mimeType.equals("text/plain")) {
                return readTextStream(inputStream);
            } else {
                // For PDF and PPT: extract readable text segments from raw bytes
                return extractReadableText(inputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String readTextStream(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Extracts human-readable text segments from binary files (PDF/PPT).
     * Reads consecutive printable ASCII characters of length >= 4.
     */
    private static String extractReadableText(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[65536];
        int bytesRead;
        StringBuilder sb = new StringBuilder();
        StringBuilder word = new StringBuilder();

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i++) {
                char c = (char) (buffer[i] & 0xFF);
                if (c >= 32 && c < 127) {
                    word.append(c);
                } else {
                    if (word.length() >= 4) {
                        sb.append(word).append(" ");
                    }
                    word.setLength(0);
                }
            }
        }
        if (word.length() >= 4) sb.append(word);
        inputStream.close();
        return sb.toString();
    }
}
