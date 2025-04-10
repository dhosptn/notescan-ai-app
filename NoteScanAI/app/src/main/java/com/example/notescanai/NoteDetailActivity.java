package com.example.notescanai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
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

public class NoteDetailActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 101;

    private EditText fullTextView;
    private TextView dateTextView;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault());
    private String noteId;
    private DatabaseReference noteRef;
    private Note currentNote;

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

        // Get note ID from intent
        noteId = getIntent().getStringExtra("NOTE_ID");

        if (noteId != null) {
            // Initialize Firebase reference
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            noteRef = database.getReference("notes").child(noteId);

            // Load note data from Firebase
            loadNoteFromFirebase();
        } else {
            Toast.makeText(this, "Error: Note ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }
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
                    // Display the note data
                    fullTextView.setText(currentNote.getText());
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
        if (!updatedText.isEmpty() && currentNote != null) {
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_export_pdf) {
            checkPermissionAndExportPdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        paint.setTextSize(12); // Ukuran font standar di Word

        // Margin kiri & atas
        int marginLeft = 55;
        int marginTop = 85;

        // Lebar area teks (agar tidak mepet ke tepi)
        int textWidth = pageWidth - 2 * marginLeft;

        // Menulis teks dengan line wrapping
        String[] lines = textContent.split("\n");
        int y = marginTop;
        int lineSpacing = 16; // Jarak antar baris seperti di Word

        for (String line : lines) {
            // Potong teks panjang agar tidak melebihi lebar kertas
            int charPerLine = 80; // Estimasi jumlah karakter per baris
            int startIdx = 0;

            while (startIdx < line.length()) {
                int endIdx = Math.min(startIdx + charPerLine, line.length());
                String subLine = line.substring(startIdx, endIdx);
                canvas.drawText(subLine, marginLeft, y, paint);
                y += lineSpacing; // Jarak antar baris
                startIdx = endIdx;

                // Cek apakah sudah penuh halaman
                if (y > pageHeight - marginTop) {
                    document.finishPage(page);
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = marginTop;
                }
            }
            y += lineSpacing / 2; // Jarak antar paragraf
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