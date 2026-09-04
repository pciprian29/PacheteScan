package com.example.pachetescan;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu);

        MaterialToolbar toolbar = findViewById(R.id.topToolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            finish();
        });

        Button btnPachet = findViewById(R.id.btnPachet);
        Button btnDeteriorare = findViewById(R.id.btnDeteriorare);
        Button btnInfoLipsa = findViewById(R.id.btnInfoLipsa);

        btnPachet.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, PachetActivity.class);
            startActivity(intent);
        });
        btnDeteriorare.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, DeteriorareActivity.class);
            startActivity(intent);
        });
        btnInfoLipsa.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, InfoLipsaActivity.class);
            startActivity(intent);
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}