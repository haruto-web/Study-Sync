package com.example.studysync_project.ui.quiz;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.R;
import com.example.studysync_project.databinding.ActivityPdfViewerBinding;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Lightweight in-app PDF viewer for uploaded module files.
 */
public class PdfViewerActivity extends AppCompatActivity {

    public static final String EXTRA_PDF_URI = "extra_pdf_uri";
    public static final String EXTRA_PDF_TITLE = "extra_pdf_title";

    private ActivityPdfViewerBinding binding;
    private ParcelFileDescriptor fileDescriptor;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private Bitmap currentBitmap;
    private int currentPageIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        String title = getIntent().getStringExtra(EXTRA_PDF_TITLE);
        if (title != null && !title.trim().isEmpty()) {
            binding.toolbar.setTitle(title);
        }

        String uriText = getIntent().getStringExtra(EXTRA_PDF_URI);
        if (uriText == null || uriText.trim().isEmpty()) {
            Toast.makeText(this, R.string.pdf_viewer_unavailable, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Uri pdfUri = Uri.parse(uriText);
        if (!openPdf(pdfUri)) {
            finish();
            return;
        }

        binding.btnPrevPage.setOnClickListener(v -> renderPage(currentPageIndex - 1));
        binding.btnNextPage.setOnClickListener(v -> renderPage(currentPageIndex + 1));

        renderPage(0);
    }

    private boolean openPdf(Uri pdfUri) {
        try {
            fileDescriptor = getContentResolver().openFileDescriptor(pdfUri, "r");
            if (fileDescriptor == null) {
                Toast.makeText(this, R.string.pdf_viewer_open_error, Toast.LENGTH_LONG).show();
                return false;
            }
            pdfRenderer = new PdfRenderer(fileDescriptor);
            if (pdfRenderer.getPageCount() == 0) {
                Toast.makeText(this, R.string.pdf_viewer_open_error, Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        } catch (FileNotFoundException e) {
            Toast.makeText(this, R.string.pdf_viewer_open_error, Toast.LENGTH_LONG).show();
            return false;
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.pdf_viewer_permission_error, Toast.LENGTH_LONG).show();
            return false;
        } catch (IOException e) {
            Toast.makeText(this, R.string.pdf_viewer_open_error, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void renderPage(int pageIndex) {
        if (pdfRenderer == null) {
            return;
        }
        if (pageIndex < 0 || pageIndex >= pdfRenderer.getPageCount()) {
            return;
        }

        closeCurrentPage();
        currentPage = pdfRenderer.openPage(pageIndex);

        int screenWidth = Math.max(getResources().getDisplayMetrics().widthPixels - 24, 1);
        float scale = screenWidth / (float) Math.max(currentPage.getWidth(), 1);
        int bitmapWidth = Math.max((int) (currentPage.getWidth() * scale), 1);
        int bitmapHeight = Math.max((int) (currentPage.getHeight() * scale), 1);

        if (currentBitmap != null) {
            currentBitmap.recycle();
        }
        currentBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        currentBitmap.eraseColor(Color.WHITE);

        currentPage.render(currentBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        binding.ivPdfPage.setImageBitmap(currentBitmap);

        currentPageIndex = pageIndex;
        updatePaginationUi();
    }

    private void updatePaginationUi() {
        if (pdfRenderer == null) {
            return;
        }

        int totalPages = pdfRenderer.getPageCount();
        binding.tvPageIndicator.setText(getString(
                R.string.pdf_viewer_page_indicator,
                currentPageIndex + 1,
                totalPages
        ));
        binding.btnPrevPage.setEnabled(currentPageIndex > 0);
        binding.btnNextPage.setEnabled(currentPageIndex < totalPages - 1);
    }

    private void closeCurrentPage() {
        if (currentPage != null) {
            currentPage.close();
            currentPage = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCurrentPage();

        if (currentBitmap != null) {
            currentBitmap.recycle();
            currentBitmap = null;
        }

        if (pdfRenderer != null) {
            pdfRenderer.close();
            pdfRenderer = null;
        }

        if (fileDescriptor != null) {
            try {
                fileDescriptor.close();
            } catch (IOException ignored) {
            }
            fileDescriptor = null;
        }
    }
}
