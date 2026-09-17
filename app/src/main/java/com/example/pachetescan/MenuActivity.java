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
import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.InvalidScannerNameException;
import com.honeywell.aidc.ScannerUnavailableException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MenuActivity extends AppCompatActivity implements BarcodeReader.BarcodeListener {

    public final String TAG = "MenuActivity";

    AidcManager manager;
    BarcodeReader reader;

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

        AidcManager.create(this, aidcManager -> {
            manager = aidcManager;
            try{
                reader = aidcManager.createBarcodeReader();
                if(reader != null){
                    try{
                        reader.addBarcodeListener(MenuActivity.this);
                        reader.claim();
                    }catch(ScannerUnavailableException ex){
                        if (BuildConfig.DEBUG) Log.e (TAG, "ScannerUnavailableException :: " + ex.getMessage(), ex);
                        Toast.makeText(MenuActivity.this, "Scanner indisponibil", Toast.LENGTH_LONG).show();
                    }
                }
            }catch (InvalidScannerNameException ex){
                if (BuildConfig.DEBUG) Log.e (TAG, "Scanner ul nu exista");
            }
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

    @Override
    protected void onResume(){
        super.onResume();
        if(reader != null){
            try{
                reader.claim();
            }catch(ScannerUnavailableException ex){
                if (BuildConfig.DEBUG) Log.e (TAG, "ScannerUnavailableException :: " + ex.getMessage(), ex);
                Toast.makeText(MenuActivity.this, "Scanner indisponibil", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(reader != null){
            reader.release();
            if(BuildConfig.DEBUG) Log.v(TAG, "Scanner released?");
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(reader != null){
            reader.removeBarcodeListener(this);
            reader.close();
        }
        if(manager != null){
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
                    findViewById(android.R.id.content).performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);

                    Intent intent = new Intent(MenuActivity.this, PachetDetaliiActivity.class);
                    intent.putExtra(PachetDetaliiActivity.EXTRA_AWB, textScanat);
                    startActivity(intent);
                }else{
                    if (BuildConfig.DEBUG) Log.e (TAG, "AWB Invalid");
                    Toast.makeText(this, "AWB Invalid", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent){
        if(BuildConfig.DEBUG) Log.e (TAG, "Barcode reader failed");
    }
}