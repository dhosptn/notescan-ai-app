package com.example.notescanai;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class SplashScreenActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY = 6500; // Durasi splash screen (3 detik)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        // Temukan ImageView dari layout
        ImageView splashGif = findViewById(R.id.nssplash);

        // Load GIF menggunakan Glide
        Glide.with(this)
                .asGif()
                .load(R.drawable.ns_splash)
                .into(splashGif);

        // Handler untuk menunda pindah ke HomeActivity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashScreenActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
}




