package com.example.notescanai;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;



import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class HomeActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int REQUEST_PICK_IMAGE = 103;

    private EditText searchEditText;
    private RecyclerView notesRecyclerView;
    private FloatingActionButton cameraFab;
    private View deleteButton;
    private View UsreIcon;
    private NoteAdapter noteAdapter;
    private List<Note> notesList;
    private Map<String, TextRecognizer> textRecognizers;
    private Uri photoURI;
    private String currentPhotoPath;


    // Firebase
    private DatabaseReference notesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Firebase dengan ID perangkat
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String deviceId = getStoredDeviceId();
        notesRef = database.getReference("users").child(deviceId).child("notes");

        // Initialize multiple TextRecognizers for different languages
        initializeTextRecognizers();



        // Initialize views
        searchEditText = findViewById(R.id.searchEditText);
        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        cameraFab = findViewById(R.id.cameraFab);
        deleteButton = findViewById(R.id.deleteButton);

        // Sembunyikan deleteButton di awal
        deleteButton.setVisibility(View.GONE);

        // Handle click delete button
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSelectedNotes();
            }
        });

        // Initialize UsreIcon view
        UsreIcon = findViewById(R.id.UsreIcon);

        UsreIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("DEBUG", "UserIcon diklik!");
                Toast.makeText(HomeActivity.this, "Klik berhasil", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(HomeActivity.this, ProfileUploadActivity.class);
                startActivity(intent);
            }
        });

        // Initialize notes list and adapter
        notesList = new ArrayList<>();
        noteAdapter = new NoteAdapter(this, notesList, new NoteAdapter.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(int count) {
                if (count > 0) {
                    deleteButton.setVisibility(View.VISIBLE);
                } else {
                    deleteButton.setVisibility(View.GONE);
                }
            }
        });

        // Set up RecyclerView
        notesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        notesRecyclerView.setAdapter(noteAdapter);

        // Set click listener for camera FAB
        cameraFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSourceDialog();
            }
        });

        // Set up search functionality
        setupSearch();

        // Load notes from Firebase
        loadNotesFromFirebase();

        ImageView imageView = findViewById(R.id.UsreIcon); // your ImageView

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String base64Str = prefs.getString("profileImage", null);

        if (base64Str != null) {
            Bitmap bitmap = ProfilePreferencesManajer.base64ToBitmap(base64Str);

            // Load image from drawable
            Glide.with(this)
                    .load(bitmap)
                    .circleCrop() // makes it rounded
                    .into(imageView);

            imageView.setImageBitmap(bitmap);
        } else {
            // If no image is saved, show default
            imageView.setImageResource(R.drawable.circle_user_solid);
        }
    }

    // Method to show dialog for choosing image source
    private void showImageSourceDialog() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Photo");

        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Take Photo")) {
                checkPermissionsAndOpenCamera();
            } else if (options[item].equals("Choose from Gallery")) {
                checkPermissionsAndOpenGallery();
            } else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    // Method to check permissions and open gallery
    private void checkPermissionsAndOpenGallery() {
        // Check for storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
            return;
        }

        // Open gallery
        openGallery();
    }

    // Method to open gallery
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    // Metode untuk mendapatkan ID perangkat unik dari SharedPreferences atau membuat yang baru
    private String getStoredDeviceId() {
        SharedPreferences prefs = getSharedPreferences("NoteScanAIPrefs", MODE_PRIVATE);
        String deviceId = prefs.getString("device_id", null);

        if (deviceId == null) {
            // Generate device ID baru jika belum ada
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString("device_id", deviceId).apply();
        }

        return deviceId;
    }

    private void initializeTextRecognizers() {
        textRecognizers = new HashMap<>();

        // Latin text recognizer with enhanced options
        TextRecognizerOptions latinOptions = TextRecognizerOptions.DEFAULT_OPTIONS;
        textRecognizers.put("latin", TextRecognition.getClient(latinOptions));

        // Chinese text recognizer
        textRecognizers.put("chinese", TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build()));

        // Japanese text recognizer
        textRecognizers.put("japanese", TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build()));

        // Korean text recognizer
        textRecognizers.put("korean", TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build()));

        // Devanagari text recognizer (Hindi, Sanskrit, etc.)
        textRecognizers.put("devanagari", TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close all text recognizers
        for (TextRecognizer recognizer : textRecognizers.values()) {
            recognizer.close();
        }
    }

    // Fungsi untuk menghapus catatan yang dipilih
    private void deleteSelectedNotes() {
        List<Note> selectedNotes = noteAdapter.getSelectedNotes(); // Dapatkan catatan yang dipilih
        if (selectedNotes.isEmpty()) return;

        for (Note note : selectedNotes) {
            notesRef.child(note.getId()).removeValue(); // Hapus dari Firebase
        }

        Toast.makeText(this, "Selected notes deleted", Toast.LENGTH_SHORT).show();
        deleteButton.setVisibility(View.GONE); // Sembunyikan tombol delete setelah menghapus
    }

    private void loadNotesFromFirebase() {
        // Order by timestamp in descending order (newest first)
        Query query = notesRef.orderByChild("timestamp");

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                notesList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Note note = snapshot.getValue(Note.class);
                    notesList.add(note);
                }

                // Sort by timestamp in descending order (newest first)
                Collections.reverse(notesList);

                // Update the adapter
                noteAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(HomeActivity.this,
                        "Failed to load notes: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearch() {
        searchEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // You could show suggestions or recent searches here
                }
            }
        });

        // Implement text change listener for filtering notes
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                filterNotes(s.toString());
            }
        });
    }

    private void filterNotes(String query) {
        if (query.isEmpty()) {
            loadNotesFromFirebase(); // Reload all notes
            return;
        }

        List<Note> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (Note note : notesList) {
            if (note.getText().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(note);
            }
        }

        // Update the adapter with filtered results
        notesList.clear();
        notesList.addAll(filteredList);
        noteAdapter.notifyDataSetChanged();
    }

    private void checkPermissionsAndOpenCamera() {
        // Check for camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
            return;
        }

        // Check for storage permission (needed for saving full-resolution photos)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
            return;
        }

        // If all permissions are granted, open camera
        dispatchTakePictureIntent();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.notescanai.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermissionsAndOpenCamera(); // Check if all permissions are granted now
            } else {
                Toast.makeText(this, "Camera permission is required to take photos",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Check if this was for camera or gallery
                if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    checkPermissionsAndOpenCamera(); // For camera flow
                } else if (permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    openGallery(); // For gallery flow
                }
            } else {
                Toast.makeText(this, "Storage permission is required to access images",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Toast.makeText(this, "Processing image from camera...", Toast.LENGTH_SHORT).show();

                try {
                    InputImage image = InputImage.fromFilePath(this, photoURI);
                    processImageForTextRecognition(image);
                } catch (IOException e) {
                    Toast.makeText(this, "Error reading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                Toast.makeText(this, "Processing image from gallery...", Toast.LENGTH_SHORT).show();

                try {
                    Uri selectedImageUri = data.getData();
                    InputImage image = InputImage.fromFilePath(this, selectedImageUri);
                    processImageForTextRecognition(image);
                } catch (IOException e) {
                    Toast.makeText(this, "Error reading gallery image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void processImageForTextRecognition(InputImage image) {
        Toast.makeText(this, "Analyzing text in image...", Toast.LENGTH_SHORT).show();

        // Step 1: Try with the default recognizer first (optimized for Latin scripts)
        TextRecognizer defaultRecognizer = textRecognizers.get("latin");

        defaultRecognizer.process(image)
                .addOnSuccessListener(text -> {
                    String recognizedText = text.getText();

                    // If default recognizer found substantial text, proceed
                    if (recognizedText.trim().length() > 10) {
                        Log.d("TextRecognition", "Success with default recognizer: " + recognizedText.substring(0, Math.min(50, recognizedText.length())));
                        finishTextRecognition(recognizedText);
                    } else {
                        // If not much text found, try with other recognizers in sequence
                        Log.d("TextRecognition", "Default recognizer found little or no text. Trying specialized recognizers...");
                        trySpecializedRecognizers(image);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TextRecognition", "Default recognizer failed: " + e.getMessage());
                    trySpecializedRecognizers(image);
                });
    }


    // New method to try different language recognizers
    private void trySpecializedRecognizers(InputImage image) {
        // List of recognizers to try (excluding the default "latin" one we already tried)
        List<String> recognizerKeys = new ArrayList<>(textRecognizers.keySet());
        recognizerKeys.remove("latin");

        // Try each recognizer and collect results
        Map<String, String> allResults = new HashMap<>();
        final int[] completedRecognizers = {0};

        for (String key : recognizerKeys) {
            TextRecognizer recognizer = textRecognizers.get(key);

            recognizer.process(image)
                    .addOnSuccessListener(text -> {
                        String result = text.getText().trim();
                        if (!result.isEmpty()) {
                            allResults.put(key, result);
                        }

                        completedRecognizers[0]++;
                        if (completedRecognizers[0] >= recognizerKeys.size()) {
                            // All recognizers have completed their work
                            processCombinedResults(allResults);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("TextRecognition", key + " recognizer failed: " + e.getMessage());
                        completedRecognizers[0]++;
                        if (completedRecognizers[0] >= recognizerKeys.size()) {
                            // All recognizers have completed their work
                            processCombinedResults(allResults);
                        }
                    });
        }
    }

    // Process all collected results
    private void processCombinedResults(Map<String, String> allResults) {
        if (allResults.isEmpty()) {
            // No recognizer found any text
            Toast.makeText(this, "Could not recognize text. Try with clearer image or different angle.", Toast.LENGTH_LONG).show();
            return;
        }

        // Find the result with the most content (assuming more text = better recognition)
        String bestResult = "";
        int maxLength = 0;
        String bestRecognizer = "";

        for (Map.Entry<String, String> entry : allResults.entrySet()) {
            if (entry.getValue().length() > maxLength) {
                maxLength = entry.getValue().length();
                bestResult = entry.getValue();
                bestRecognizer = entry.getKey();
            }
        }

        Log.d("TextRecognition", "Best result from " + bestRecognizer + " recognizer");
        finishTextRecognition(bestResult);
    }



    private void finishTextRecognition(String recognizedText) {
        if (!recognizedText.isEmpty()) {
            // Create a new note with the recognized text
            long timestamp = System.currentTimeMillis();
            String noteId = String.valueOf(timestamp);
            Note newNote = new Note(noteId, recognizedText, timestamp);

            // Save note to Firebase
            saveNoteToFirebase(newNote);

            // Open detail activity to show the full text
            openNoteDetailActivity(newNote);
        } else {
            Toast.makeText(HomeActivity.this,
                    "No text recognized in the image",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void saveNoteToFirebase(Note note) {
        notesRef.child(note.getId()).setValue(note)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(HomeActivity.this,
                                "Note saved successfully",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(HomeActivity.this,
                                "Failed to save note: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public Bitmap getImageFromSharedPreferences(Context context) {
        // Ambil SharedPreferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        // Ambil string encoded gambar
        String encodedImage = sharedPreferences.getString("profileImage", null);

        // Periksa apakah gambar tersimpan
        if (encodedImage != null) {
            try {
                // Decode base64 string kembali menjadi bitmap
                byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    private void openNoteDetailActivity(Note note) {
        Intent intent = new Intent(this, NoteDetailActivity.class);
        intent.putExtra("NOTE_ID", note.getId());
        startActivity(intent);
    }
}