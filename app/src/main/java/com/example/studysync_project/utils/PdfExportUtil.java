package com.example.studysync_project.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfExportUtil {

    private static final int PAGE_WIDTH = 595;   // A4 at 72dpi
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 48;
    private static final int LINE_HEIGHT = 22;

    /**
     * Exports a simple text report as a PDF and opens the share sheet.
     *
     * @param context  calling context
     * @param title    document title
     * @param lines    list of text lines to write
     * @param fileName output file name (without extension)
     */
    public static void exportAndShare(Context context, String title, List<String> lines, String fileName) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#6750A4"));
        titlePaint.setTextSize(22f);
        titlePaint.setFakeBoldText(true);

        Paint bodyPaint = new Paint();
        bodyPaint.setColor(Color.parseColor("#1C1B1F"));
        bodyPaint.setTextSize(13f);

        Paint datePaint = new Paint();
        datePaint.setColor(Color.parseColor("#49454F"));
        datePaint.setTextSize(11f);

        int y = MARGIN + 30;
        canvas.drawText(title, MARGIN, y, titlePaint);
        y += 20;
        canvas.drawText("Generated: " + new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                .format(new Date()), MARGIN, y, datePaint);
        y += 30;

        // Divider
        Paint divider = new Paint();
        divider.setColor(Color.parseColor("#79747E"));
        divider.setStrokeWidth(1f);
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, divider);
        y += 20;

        for (String line : lines) {
            if (y + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) break; // simple single-page guard
            if (line.startsWith("##")) {
                titlePaint.setTextSize(15f);
                canvas.drawText(line.substring(2).trim(), MARGIN, y, titlePaint);
                titlePaint.setTextSize(22f);
            } else {
                canvas.drawText(line, MARGIN, y, bodyPaint);
            }
            y += LINE_HEIGHT;
        }

        document.finishPage(page);

        try {
            File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir != null && !dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName + ".pdf");
            document.writeTo(new FileOutputStream(file));
            document.close();

            Uri uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/pdf");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(share, "Share PDF"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
