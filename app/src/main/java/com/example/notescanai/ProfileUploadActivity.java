package com.example.notescanai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;

public class ProfileUploadActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    // View elements
    private ImageView profileImageView, buttonBack;
    private TextView tvUserNameLabel, tvAppVersion;
    private MaterialButton btnSaveProfile, btnResetProfile;
    private FloatingActionButton btnUploadPhoto;

    // Data variables
    private Uri selectedImageUri;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_upload);

        // Initialize SharedPreferences
        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        // Initialize views
        profileImageView = findViewById(R.id.profileImageView);
        buttonBack = findViewById(R.id.buttonBack);
        tvUserNameLabel = findViewById(R.id.tvUserNameLabel);
        tvAppVersion = findViewById(R.id.tvAppVersion);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnResetProfile = findViewById(R.id.btnResetProfile);

        // Set app version
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            tvAppVersion.setText("Versi " + version);
        } catch (PackageManager.NameNotFoundException e) {
            tvAppVersion.setText("Versi 1.0.0");
        }

        // Set up listeners
        buttonBack.setOnClickListener(v -> navigateToHome());
        btnUploadPhoto.setOnClickListener(v -> openImageChooser());
        btnSaveProfile.setOnClickListener(v -> saveProfileImage());
        btnResetProfile.setOnClickListener(v -> resetProfileImage());

        // Load profile image
        loadProfileImage();
    }

    private void loadProfileImage() {
        String base64Str = prefs.getString("profileImage", null);

        if (base64Str != null) {
            Bitmap bitmap = ProfilePreferencesManajer.base64ToBitmap(base64Str);

            // Load image with Glide
            Glide.with(this)
                    .load(bitmap)
                    .circleCrop()
                    .into(profileImageView);

            // Set username if available
            String username = prefs.getString("userName", "NoteScan User");
            tvUserNameLabel.setText(username);
        } else {
            // If no image is saved, show default
            Glide.with(this)
                    .load(R.drawable.circle_user_solid)
                    .circleCrop()
                    .into(profileImageView);
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

                // Display selected image
                Glide.with(this)
                        .load(bitmap)
                        .circleCrop()
                        .into(profileImageView);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveProfileImage() {
        // Save profile image
        profileImageView.setDrawingCacheEnabled(true);
        profileImageView.buildDrawingCache();

        // Get Bitmap
        Bitmap bitmap = Bitmap.createBitmap(profileImageView.getDrawingCache());

        // Turn off drawing cache
        profileImageView.setDrawingCacheEnabled(false);

        // Convert to Base64
        String base64Image = ProfilePreferencesManajer.bitmapToBase64(bitmap);

        // Save to SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("profileImage", base64Image);
        editor.putString("userName", tvUserNameLabel.getText().toString());
        editor.apply();

        Toast.makeText(this, "Profil berhasil disimpan", Toast.LENGTH_SHORT).show();
    }

    private void resetProfileImage() {
        // Remove profile image
        prefs.edit().remove("profileImage").apply();

        // Reset image to default
        Glide.with(this)
                .load(R.drawable.circle_user_solid)
                .circleCrop()
                .into(profileImageView);

        // Reset username
        tvUserNameLabel.setText("NoteScan User");
        prefs.edit().putString("userName", "NoteScan User").apply();

        Toast.makeText(this, "Foto profil berhasil dihapus", Toast.LENGTH_SHORT).show();
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}