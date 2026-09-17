package com.example.pachetescan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.InvalidScannerNameException;
import com.honeywell.aidc.ScannerUnavailableException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntroducereAwbDetalii extends AppCompatActivity implements BarcodeReader.BarcodeListener {

    public final String TAG = "SelectarePachetActivity";
    private static final String REGEX_AWB = "^AWB-\\d{8}-[A-Z0-9]{10}$";

    AidcManager manager;
    BarcodeReader reader;

    TextInputLayout awbPachetContainer;
    TextInputEditText etAwbPachet;
    Button btnCautaPachet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_introducere_awb_detalii);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.topToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        awbPachetContainer = findViewById(R.id.awbPachet);
        etAwbPachet = findViewById(R.id.etAwbPachet);
        btnCautaPachet = findViewById(R.id.btnCautaPachet);

        btnCautaPachet.setOnClickListener(v -> cautaPachetDinInput());

        etAwbPachet.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void afterTextChanged(android.text.Editable s) {
                awbPachetContainer.setError(null);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        AidcManager.create(this, aidcManager -> {
            manager = aidcManager;
            try {
                reader = aidcManager.createBarcodeReader();
                if (reader != null) {
                    try {
                        reader.addBarcodeListener(IntroducereAwbDetalii.this);
                        reader.claim();
                    } catch (ScannerUnavailableException ex) {
                        if (BuildConfig.DEBUG) Log.e(TAG, "ScannerUnavailableException :: " + ex.getMessage(), ex);
                        Toast.makeText(IntroducereAwbDetalii.this, "Scanner indisponibil", Toast.LENGTH_LONG).show();
                    }
                }
            } catch (InvalidScannerNameException ex) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Scanner ul nu exista");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reader != null) {
            try {
                reader.claim();
            } catch (ScannerUnavailableException ex) {
                if (BuildConfig.DEBUG) Log.e(TAG, "ScannerUnavailableException :: " + ex.getMessage(), ex);
                Toast.makeText(IntroducereAwbDetalii.this, "Scanner indisponibil", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (reader != null) {
            reader.release();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reader != null) {
            reader.removeBarcodeListener(this);
            reader.close();
        }
        if (manager != null) {
            manager.close();
        }
    }

    @Override
    public void onBarcodeEvent(BarcodeReadEvent barcodeReadEvent){
        final String data = barcodeReadEvent.getBarcodeData();

        runOnUiThread(() ->{
            if(data != null && !data.trim().isEmpty()){

                String textScanat = data.trim();
                String regex = "^AWB-\\d{8}-[A-Z0-9]{10}$";
                Pattern pattern = Pattern.compile(regex);

                Matcher matcher = pattern.matcher(textScanat);
                if(matcher.matches()){
                    etAwbPachet.setText(textScanat);
                    findViewById(android.R.id.content).performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                    android.widget.Toast.makeText(this, "AWB selectat: " + textScanat, Toast.LENGTH_SHORT).show();
                }else{
                    if (BuildConfig.DEBUG) Log.e (TAG, "AWB Invalid");
                    Toast.makeText(this, "AWB Invalid", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent) {
        if (BuildConfig.DEBUG) Log.e(TAG, "Barcode reader failed");
    }

    private void cautaPachetDinInput() {
        String awb = etAwbPachet.getText().toString().trim();

        if (awb.isEmpty()) {
            awbPachetContainer.setError("Introduceti un AWB");
            return;
        }

        Matcher matcher = Pattern.compile(REGEX_AWB).matcher(awb);
        if (!matcher.matches()) {
            awbPachetContainer.setError("Format AWB invalid");
            return;
        }

        deschideDetalii(awb);
    }

    private void deschideDetalii(String awb) {
        Intent intent = new Intent(this, com.example.pachetescan.PachetDetaliiActivity.class);
        intent.putExtra(com.example.pachetescan.PachetDetaliiActivity.EXTRA_AWB, awb);
        startActivity(intent);
    }
}