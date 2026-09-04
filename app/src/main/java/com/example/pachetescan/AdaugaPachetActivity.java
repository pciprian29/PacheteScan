package com.example.pachetescan;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class AdaugaPachetActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_adauga_pachet);

        MaterialToolbar toolbar = findViewById(R.id.topToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            finish();
        });

//  -----------------------------------------------------------------
//        Functionalitati pagina

        String[] optiuni = {"Plic", "Cutie", "Palet"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, optiuni);

        AutoCompleteTextView dropdown = findViewById(R.id.dropdownTipPachet);
        dropdown.setAdapter(adapter);

        RadioGroup rgTipExpeditor = findViewById(R.id.rgTipExpeditor);
        LinearLayout containerExpNumePrenume = findViewById(R.id.containerExpNumePrenume);
        containerExpNumePrenume.setVisibility(View.GONE);

        RadioGroup rgTipDestinatar = findViewById(R.id.rgTipDestinatar);
        LinearLayout containerDestNumePrenume = findViewById(R.id.containerDestNumePrenume);
        containerDestNumePrenume.setVisibility(View.GONE);

        rgTipExpeditor.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbExpNou) {
                containerExpNumePrenume.setVisibility(View.VISIBLE);
            }
            if (checkedId == R.id.rbExpExistent) {
                containerExpNumePrenume.setVisibility(View.GONE);
            }
        });

        rgTipDestinatar.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDestNou) {
                containerDestNumePrenume.setVisibility(View.VISIBLE);
            }
            if (checkedId == R.id.rbDestExistent) {
                containerDestNumePrenume.setVisibility(View.GONE);
            }
        });

        String[] optiuniStatus = {"Inregistrat", "In Tranzit", "Livrat", "Deteriorat", "Informatii Lipsa"};
        ArrayAdapter<String> adapterStatus = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, optiuniStatus);

        AutoCompleteTextView dropdownStatus = findViewById(R.id.dropdownStatusPachet);
        dropdownStatus.setAdapter(adapterStatus);

        Button btnAnuleaza = findViewById(R.id.btnAnuleaza);
        btnAnuleaza.setOnClickListener(view ->{
            finish();
        });
//      ---------------------------------------------------------------------------------
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}