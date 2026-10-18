
package com.example.momentum.activities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.momentum.R;
import com.example.momentum.data.Session;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CertificateActivity extends AppCompatActivity {

    private TextView tvCertificateStudent;
    private TextView tvCertificateCourse;
    private TextView tvCertificateDate;
    private Button tvCertificateBack;
    private Button btnDownloadCertificate;

    private ActivityResultLauncher<String> createDocumentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate);

        tvCertificateStudent = findViewById(R.id.tvCertificateStudent);
        tvCertificateCourse = findViewById(R.id.tvCertificateCourse);
        tvCertificateDate = findViewById(R.id.tvCertificateDate);
        tvCertificateBack = findViewById(R.id.tvCertificateBack);
        btnDownloadCertificate = findViewById(R.id.btnDownloadCertificate);

        createDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/pdf"),
                uri -> {
                    if (uri != null) {
                        saveCertificatePdf(uri);
                    }
                }
        );

        String courseTitle = getIntent().getStringExtra("courseTitle");

        if (courseTitle == null || courseTitle.trim().isEmpty()) {
            courseTitle = "Completed Course";
        }

        // Keep a final copy so it can be used inside the lambda.
        final String certificateCourseTitle = courseTitle;

        tvCertificateCourse.setText(certificateCourseTitle);
        tvCertificateDate.setText("Loading completion date...");

        tvCertificateBack.setOnClickListener(v -> finish());

        btnDownloadCertificate.setOnClickListener(v -> {
            String fileName = makeSafeFileName(certificateCourseTitle)
                    + "_Certificate.pdf";

            createDocumentLauncher.launch(fileName);
        });

        loadStudentName();
        loadCompletionDate();
    }

    private String makeSafeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private void loadStudentName() {
        if (!Session.isLoggedIn()) {
            tvCertificateStudent.setText("Student");
            return;
        }

        String email = Session.getUser().email;

        String fallbackName = "Student";

        if (email != null && email.contains("@")) {
            fallbackName = email.substring(0, email.indexOf("@"));
        }

        tvCertificateStudent.setText(fallbackName);

        String name = Session.getUser().fullName;

        if (name != null && !name.trim().isEmpty()) {
            tvCertificateStudent.setText(name);
        }
    }

    // The completion date comes from LearnTech, passed in by the course screen.
    private void loadCompletionDate() {
        String completedOn = getIntent().getStringExtra("completedOn");

        if (completedOn == null || completedOn.length() < 10) {
            tvCertificateDate.setText(
                    "Completion date not recorded"
            );
            return;
        }

        try {
            Date completedAt = new SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
            ).parse(completedOn.substring(0, 10));

            String formattedDate = new SimpleDateFormat(
                    "dd MMMM yyyy",
                    Locale.getDefault()
            ).format(completedAt);

            tvCertificateDate.setText(
                    "Completed on " + formattedDate
            );

        } catch (Exception e) {
            tvCertificateDate.setText(
                    "Could not load completion date"
            );
        }
    }

    private void saveCertificatePdf(Uri uri) {
        PdfDocument pdfDocument = new PdfDocument();

        try {
            // Landscape page: 842 x 595 points.
            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(
                            842, 595, 1
                    ).create();

            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            // White page background.
            canvas.drawColor(Color.WHITE);

            // Outer blue border.
            paint.setColor(Color.rgb(118, 102, 232));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            canvas.drawRect(
                    new RectF(24, 24, 818, 571), paint
            );

            // Inner green border.
            paint.setColor(Color.rgb(53, 184, 165));
            paint.setStrokeWidth(2);
            canvas.drawRect(
                    new RectF(36, 36, 806, 559), paint
            );

            paint.setStyle(Paint.Style.FILL);

            drawCenteredText(
                    canvas, paint,
                    "LEARNTECH MOMENTUM", 70,
                    52, Color.rgb(118, 102, 232), true
            );

            drawCenteredText(
                    canvas, paint,
                    "CERTIFICATE OF COMPLETION", 38,
                    125, Color.rgb(7, 29, 53), true
            );

            drawCenteredText(
                    canvas, paint,
                    "This certificate is proudly presented to", 21,
                    180, Color.rgb(104, 119, 131), false
            );

            drawCenteredText(
                    canvas, paint,
                    tvCertificateStudent.getText().toString(), 34,
                    235, Color.rgb(118, 102, 232), true
            );

            drawCenteredText(
                    canvas, paint,
                    "for successfully completing", 21,
                    280, Color.rgb(104, 119, 131), false
            );

            drawCenteredText(
                    canvas, paint,
                    tvCertificateCourse.getText().toString(), 28,
                    330, Color.rgb(7, 29, 53), true
            );

            drawCenteredText(
                    canvas, paint,
                    tvCertificateDate.getText().toString(), 19,
                    385, Color.rgb(104, 119, 131), false
            );

            // Green divider.
            paint.setColor(Color.rgb(53, 184, 165));
            paint.setStrokeWidth(3);
            canvas.drawLine(270, 420, 572, 420, paint);

            drawCenteredText(
                    canvas, paint,
                    "Keep learning. Keep growing.", 20,
                    465, Color.rgb(53, 184, 165), true
            );

            drawCenteredText(
                    canvas, paint,
                    "LearnTech Momentum", 15,
                    510, Color.rgb(104, 119, 131), false
            );

            pdfDocument.finishPage(page);

            try (OutputStream outputStream =
                         getContentResolver().openOutputStream(uri)) {

                if (outputStream == null) {
                    throw new java.io.IOException(
                            "Unable to open the selected file."
                    );
                }

                pdfDocument.writeTo(outputStream);
            }

            Toast.makeText(
                    this,
                    "Certificate PDF saved successfully!",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Could not save PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

        } finally {
            pdfDocument.close();
        }
    }

    private void drawCenteredText(
            Canvas canvas,
            Paint paint,
            String text,
            float textSize,
            float y,
            int color,
            boolean bold
    ) {
        paint.setColor(color);
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(bold);

        // Shrink long names or course titles to fit the page.
        float maxWidth = 720;

        while (paint.measureText(text) > maxWidth
                && paint.getTextSize() > 14) {
            paint.setTextSize(paint.getTextSize() - 1);
        }

        canvas.drawText(text, 421, y, paint);
        paint.setFakeBoldText(false);
    }
}
