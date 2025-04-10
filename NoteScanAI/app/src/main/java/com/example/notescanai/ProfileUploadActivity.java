package com.example.notescanai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;


public class ProfileUploadActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView profileImageView, buttonback1;
    private Button btnSaveProfile, btnResetProfile;

    private FloatingActionButton btnUploadPhoto;

    private Uri selectedImageUri;
    private int selectedAvatarResourceId = -1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_upload);

        // Inisialisasi view dan listener saja tanpa preferences manager
        profileImageView = findViewById(R.id.profileImageView);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnResetProfile = findViewById(R.id.btnResetProfile);
        buttonback1 = findViewById(R.id.buttonback);

        // Setup listeners langsung di sini
        btnUploadPhoto.setOnClickListener(v -> openImageChooser());
        btnSaveProfile.setOnClickListener(v -> saveProfileImage());
        btnResetProfile.setOnClickListener(v -> resetProfileImage());
        buttonback1.setOnClickListener(v -> kembaliKehome());

        // Setup avatar recycler view
        setupAvatarRecyclerView();

        ImageView imageView = findViewById(R.id.profileImageView); // your ImageView

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

    private void setupAvatarRecyclerView() {
            if (selectedImageUri != null || selectedAvatarResourceId != -1) {
                // Untuk sementara hanya tampilkan toast
                Toast.makeText(this, "Foto profil berhasil disimpan", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Pilih foto profil terlebih dahulu", Toast.LENGTH_SHORT).show();
            }
        }


    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                profileImageView.setImageBitmap(bitmap);
                selectedAvatarResourceId = -1;

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onAvatarSelected(int avatarResourceId) {
        profileImageView.setImageResource(avatarResourceId);
        selectedAvatarResourceId = avatarResourceId;
        selectedImageUri = null;
    }

    private void saveProfileImage() {
        if (selectedImageUri != null || selectedAvatarResourceId != -1) {
            // Simpan foto profil, misalnya ke SharedPreferences

            ImageView imageView = findViewById(R.id.profileImageView);

            // Enable drawing cache
            imageView.setDrawingCacheEnabled(true);
            imageView.buildDrawingCache();

            // Get Bitmap
            Bitmap bitmap = Bitmap.createBitmap(imageView.getDrawingCache());

            // Turn off drawing cache
            imageView.setDrawingCacheEnabled(false);

            // Convert to Base64
            String base64Image = ProfilePreferencesManajer.bitmapToBase64(bitmap);

            // Save to SharedPreferences
            SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("profileImage", base64Image);
            editor.apply();

            Toast.makeText(this, "Foto profil berhasil disimpan", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Pilih foto profil terlebih dahulu", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetProfileImage() {
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        prefs.edit().remove("profileImage").apply();
        Toast.makeText(this, "foto berhasil di hapus", Toast.LENGTH_SHORT).show();
        ImageView imageView = findViewById(R.id.profileImageView); // your ImageView
        // Load image from drawable
        Glide.with(this)
                .load(R.drawable.circle_user_solid)
                .circleCrop() // makes it rounded
                .into(imageView);
    }

    private void kembaliKehome() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

}