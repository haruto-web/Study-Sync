package com.example.studysync_project.utils;

import android.content.Context;
import android.net.Uri;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class TextExtractorUtil {

    /**
     * Extracts readable text from a URI (TXT or PDF).
     * For PDF, reads raw bytes and pulls out readable ASCII strings.
     * For TXT/PPT, reads line by line.
     */
    public static String extract(Context context, Uri uri) {
        String mimeType = null;
        try {
            mimeType = context.getContentResolver().getType(uri);
        } catch (Exception ignored) {
        }

        String normalized = mimeType != null ? mimeType.toLowerCase(Locale.US) : null;

        try {
            if ("text/plain".equals(normalized)) {
                try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                    if (inputStream == null) return null;
                    return readTextStream(inputStream);
                }
            }

            if ("application/pdf".equals(normalized)) {
                PDFBoxResourceLoader.init(context.getApplicationContext());
                try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                     PDDocument document = inputStream != null ? PDDocument.load(inputStream) : null) {
                    if (document == null) return null;
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setSortByPosition(true);
                    return stripper.getText(document);
                }
            }

            // Fallback: attempt to extract readable text segments from raw bytes (legacy behavior)
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                if (inputStream == null) return null;
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
        return sb.toString();
    }
}
