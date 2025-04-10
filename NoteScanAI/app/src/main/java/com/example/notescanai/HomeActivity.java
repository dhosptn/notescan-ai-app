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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;

    private EditText searchEditText;
    private RecyclerView notesRecyclerView;
    private FloatingActionButton cameraFab;
    private View deleteButton;
    private View UsreIcon;
    private NoteAdapter noteAdapter;
    private List<Note> notesList;
    private TextRecognizer textRecognizer;
    private Uri photoURI;
    private String currentPhotoPath;

    // Firebase
    private DatabaseReference notesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        notesRef = database.getReference("notes");

        // Initialize TextRecognizer with high accuracy option
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText);
        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        cameraFab = findViewById(R.id.cameraFab);
        deleteButton = findViewById(R.id.btn_delete); // Tambahkan ini

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
                checkPermissionsAndOpenCamera();
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

    public class MainActivity extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_home);
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
        if (requestCode == REQUEST_CAMERA_PERMISSION || requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermissionsAndOpenCamera(); // Check if all permissions are granted now
            } else {
                Toast.makeText(this, "Permission is required to use this feature",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show();

            // Process the full-resolution image from the file URI
            try {
                InputImage image = InputImage.fromFilePath(this, photoURI);
                processImageForTextRecognition(image);
            } catch (IOException e) {
                Toast.makeText(this, "Error reading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processImageForTextRecognition(InputImage image) {
        // Show a loading state if needed
        // You could add a ProgressBar here and set it to visible

        Task<Text> result = textRecognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text text) {
                        // Hide loading state if you added one

                        String recognizedText = text.getText();
                        if (!recognizedText.isEmpty()) {
                            // Create a new note with the recognized text
                            long timestamp = System.currentTimeMillis();
                            String noteId = String.valueOf(timestamp);
                            Note newNote = new Note(noteId, recognizedText, timestamp);

                            // Optionally, add the image path to the note
                            // If you want to include the image with the note, add a field to your Note class
                            // newNote.setImagePath(currentPhotoPath);

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
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Hide loading state if you added one

                        Toast.makeText(HomeActivity.this,
                                "Text recognition failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
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

    // Contoh penggunaan di activity lain
    public void displaySavedImage() {
        // Misalnya di ProfileDisplayActivity
        ImageView imageView = findViewById(R.id.UsreIcon);

        Bitmap savedBitmap = getImageFromSharedPreferences(this);
        if (savedBitmap != null) {
            imageView.setImageBitmap(savedBitmap);
        } else {
            // Tampilkan gambar default jika tidak ada gambar tersimpan
            imageView.setImageResource(R.drawable.pprotfile);
        }
    }

    private void openNoteDetailActivity(Note note) {
        Intent intent = new Intent(this, NoteDetailActivity.class);
        intent.putExtra("NOTE_ID", note.getId());
        startActivity(intent);
    }
}