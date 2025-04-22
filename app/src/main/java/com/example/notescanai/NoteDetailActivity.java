package com.example.notescanai;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.text.style.LineHeightSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoteDetailActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 101;

    private EditText fullTextView;
    private TextView dateTextView;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault());
    private String noteId;
    private DatabaseReference noteRef;
    private Note currentNote;
    private boolean isInitialTextSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        fullTextView = findViewById(R.id.fullTextView);
        dateTextView = findViewById(R.id.dateTextView);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Note Detail");
        }

        // Improve text view readability
        setupTextView();

        // Get note ID from intent
        String noteId = getIntent().getStringExtra("NOTE_ID");
        if (noteId == null) {
            finish(); // Tutup activity biar ga crash
            return;
        }

        if (noteId != null) {
            // Initialize Firebase reference with device ID
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            String deviceId = getUniqueDeviceId();
            noteRef = database.getReference("users").child(deviceId).child("notes").child(noteId);

            // Load note data from Firebase
            loadNoteFromFirebase();
        } else {
            Toast.makeText(this, "Error: Note ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupTextView() {
        // Set proper text size
        fullTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        // Add padding for better readability
        int padding = dpToPx(16);
        fullTextView.setPadding(padding, padding, padding, padding);

        // Enable scrolling for long texts
        fullTextView.setVerticalScrollBarEnabled(true);
        fullTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

        // Set line spacing
        fullTextView.setLineSpacing(dpToPx(4), 1.2f);

        // Add text change listener to auto-format text
        fullTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Skip formatting during initial text setting to avoid infinite loop
                if (!isInitialTextSet) return;

                // Simple formatting could be added here if needed
                // For example, removing excessive blank lines
                removeExcessiveBlankLines(s);
            }
        });
    }

    // Convert dp to pixels
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // Remove more than 2 consecutive blank lines
    private void removeExcessiveBlankLines(Editable s) {
        String text = s.toString();
        String pattern = "\n{3,}"; // 3 or more newlines
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(text);

        // Don't modify during initial text processing
        if (!isInitialTextSet) return;

        // Replace excessive newlines with just two
        if (m.find()) {
            isInitialTextSet = false; // Prevent recursion
            String newText = text.replaceAll(pattern, "\n\n");
            s.replace(0, text.length(), newText);
            isInitialTextSet = true;
        }
    }

    // Format recognized text for better readability
    private String formatRecognizedText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Normalize line breaks
        text = text.replaceAll("\r\n", "\n");

        // Remove excessive spaces
        text = text.replaceAll(" {2,}", " ");

        // Ensure proper paragraph breaks (replace single newlines with space, keep double newlines)
        text = text.replaceAll("(?<!\n)\n(?!\n)", " ");

        // Fix spacing after period, question mark, or exclamation mark followed by lowercase
        text = text.replaceAll("([.!?])\\s*([a-z])", "$1 $2");

        // Remove excessive blank lines (more than 2)
        text = text.replaceAll("\n{3,}", "\n\n");

        // Ensure capital letter after period at end of sentence
        Pattern pattern = Pattern.compile("([.!?])\\s+([a-z])");
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(result, matcher.group(1) + " " + matcher.group(2).toUpperCase());
        }
        matcher.appendTail(result);
        text = result.toString();

        return text;
    }


    // Method renamed to avoid conflict with ContextWrapper's getDeviceId()
    private String getUniqueDeviceId() {
        SharedPreferences prefs = getSharedPreferences("NoteScanAIPrefs", MODE_PRIVATE);
        return prefs.getString("device_id", "");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_detail, menu);
        return true;
    }

    private void loadNoteFromFirebase() {
        noteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentNote = dataSnapshot.getValue(Note.class);
                if (currentNote != null) {
                    // First format the text for better readability
                    String formattedText = formatRecognizedText(currentNote.getText());

                    // Set initial formatting flag to false to avoid triggering text watcher
                    isInitialTextSet = false;

                    // Display the formatted note data
                    fullTextView.setText(formattedText);

                    // Now allow the text watcher to work
                    isInitialTextSet = true;

                    // Update date display
                    dateTextView.setText(dateFormat.format(new Date(currentNote.getTimestamp())));
                } else {
                    Toast.makeText(NoteDetailActivity.this, "Note not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(NoteDetailActivity.this, "Failed to load note: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveNoteChanges();
    }

    private void saveNoteChanges() {
        String updatedText = fullTextView.getText().toString().trim();
        if (currentNote != null && !updatedText.equals(currentNote.getText())) {
            noteRef.child("text").setValue(updatedText)
                    .addOnSuccessListener(aVoid -> {
                        // Note saved successfully
                        currentNote.setText(updatedText);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(NoteDetailActivity.this, "Gagal menyimpan perubahan", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem pinItem = menu.findItem(R.id.pinButton);

        if (pinItem != null && currentNote != null) {
            if (currentNote.isPinned()) {
                pinItem.setIcon(R.drawable.ic_pin); // Icon pin aktif
                pinItem.setTitle("Unpin Note");
            } else {
                pinItem.setIcon(R.drawable.ic_pin_outline); // Icon pin tidak aktif
                pinItem.setTitle("Pin Note");
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_export_pdf) {
            checkPermissionAndExportPdf();
            return true;
        } else if (id == R.id.pinButton) { // <<< Tambahan baru
            togglePinStatus(); // Toggle pin/unpin ketika tombol pin di menu diklik
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void togglePinStatus() {
        if (currentNote != null) {
            boolean newPinStatus = !currentNote.isPinned();
            currentNote.setPinned(newPinStatus);

            // Update in Firebase
            noteRef.child("pinned").setValue(newPinStatus)
                    .addOnSuccessListener(aVoid -> {
                        // Update menu icons
                        invalidateOptionsMenu(); // <<< ini yang penting supaya menu refresh

                        // Show message
                        String message = newPinStatus ? "Note pinned" : "Note unpinned";
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to update pin status", Toast.LENGTH_SHORT).show());
        }
    }

    private void checkPermissionAndExportPdf() {
        // Check for storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        } else {
            // Permission already granted, export PDF
            exportTextToPdf();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportTextToPdf();
            } else {
                Toast.makeText(this, "Izin penyimpanan diperlukan untuk mengekspor PDF", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void exportTextToPdf() {
        // Get text content
        String textContent = fullTextView.getText().toString();
        if (textContent.isEmpty()) {
            Toast.makeText(this, "Catatan kosong, tidak ada yang diekspor", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ukuran A4 dalam point (satuan standar PDF)
        int pageWidth = 595;  // 8.27 inch (A4 width)
        int pageHeight = 842; // 11.69 inch (A4 height)

        // Buat dokumen PDF
        PdfDocument document = new PdfDocument();

        // Buat halaman dengan ukuran A4
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Objek paint untuk menggambar teks
        Paint paint = new Paint();
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(12); // Ukuran font standar di Word

        // Paint untuk judul
        Paint titlePaint = new Paint();
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(16);

        // Margin kiri & atas
        int marginLeft = 55;
        int marginTop = 50;

        // Tambahkan judul (tanggal)
        String title = dateFormat.format(new Date(currentNote.getTimestamp()));
        canvas.drawText(title, marginLeft, marginTop, titlePaint);

        // Tambahkan garis pembatas
        Paint linePaint = new Paint();
        linePaint.setColor(0xFF000000);
        linePaint.setStrokeWidth(1);
        canvas.drawLine(marginLeft, marginTop + 10, pageWidth - marginLeft, marginTop + 10, linePaint);

        // Update posisi y untuk konten
        int y = marginTop + 40;

        // Lebar area teks (agar tidak mepet ke tepi)
        int textWidth = pageWidth - 2 * marginLeft;

        // Menulis teks dengan line wrapping
        String[] paragraphs = textContent.split("\n\n");
        int lineSpacing = 18; // Jarak antar baris seperti di Word

        for (String paragraph : paragraphs) {
            // Tambahkan indentasi untuk paragraf
            boolean firstLine = true;

            // Bagi paragraf menjadi baris berdasarkan lebar
            String[] words = paragraph.trim().split("\\s+");
            StringBuilder line = new StringBuilder();

            for (String word : words) {
                if (paint.measureText(line + " " + word) < textWidth - (firstLine ? 20 : 0)) {
                    if (line.length() > 0) line.append(" ");
                    line.append(word);
                } else {
                    // Tulis baris sekarang
                    int indent = firstLine ? 20 : 0; // Indentasi hanya untuk baris pertama
                    canvas.drawText(line.toString(), marginLeft + indent, y, paint);
                    y += lineSpacing;
                    firstLine = false;

                    // Reset untuk baris baru
                    line = new StringBuilder(word);

                    // Cek jika halaman penuh
                    if (y > pageHeight - marginTop) {
                        document.finishPage(page);
                        page = document.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = marginTop;
                    }
                }
            }

            // Tulis sisa teks di baris terakhir
            if (line.length() > 0) {
                int indent = firstLine ? 20 : 0;
                canvas.drawText(line.toString(), marginLeft + indent, y, paint);
                y += lineSpacing;
            }

            // Tambahkan jarak antar paragraf
            y += lineSpacing / 2;

            // Cek jika halaman penuh
            if (y > pageHeight - marginTop) {
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = marginTop;
            }
        }

        // Finish the page
        document.finishPage(page);

        // Create file in Documents directory
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Note_" + timestamp + ".pdf";

        // Get directory for saving files
        File pdfDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "NoteScanAI");

        // Create directory if it doesn't exist
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }

        File pdfFile = new File(pdfDir, fileName);

        try {
            // Write document content to file
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            Toast.makeText(this, "PDF berhasil disimpan di " + pdfFile.getPath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal membuat file PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}