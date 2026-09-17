package com.example.pachetescan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.pachetescan.data.TokenStorage;
import com.example.pachetescan.data.imagini.model.ImagineRequestModel;
import com.example.pachetescan.ui.imagini.ImagineViewModel;
import com.example.pachetescan.ui.imagini.ImagineViewModelFactory;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdaugaImagini extends AppCompatActivity {

    public static final String EXTRA_ID_ENTITATE = "id_entitate";
    public static final String EXTRA_TIP_ENDPOINT = "tip_endpoint"; // "infolipsa" sau "deteriorare"
    public static final String EXTRA_AWB = "awb";
    public static final String EXTRA_MODE = "mode";
    public static final int MODE_UPLOAD_IMEDIAT = 1; // InfoLipsa - upload direct
    public static final int MODE_DOAR_CAPTURA = 2;   // Deteriorare fisier returnat, fara upload

    public static final String EXTRA_FISIER_CAPTURAT = "fisier_capturat";

    public final String TAG = "AdaugaImagini";

    PreviewView viewFinder;
    ImageView imageResult;
    android.widget.LinearLayout layoutCameraActive;
    android.widget.LinearLayout layoutPozaValidare;
    MaterialButton btnFaPoza;
    MaterialButton btnRetakePoza;
    MaterialButton btnConfirmaPoza;
    MaterialButton btnAnuleaza;

    ImageCapture imageCapture;
    ExecutorService cameraExecutor;
    File pozaTemporara;

    int idEntitate;
    String tipEndpoint;
    String awb;
    int mod;
    boolean aFostConfirmata;

    ImagineViewModel imagineViewModel;

    ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_awb);

        idEntitate = getIntent().getIntExtra(EXTRA_ID_ENTITATE, 0);
        tipEndpoint = getIntent().getStringExtra(EXTRA_TIP_ENDPOINT);
        awb = getIntent().getStringExtra(EXTRA_AWB);
        mod = getIntent().getIntExtra(EXTRA_MODE, MODE_UPLOAD_IMEDIAT);

        if (mod == MODE_UPLOAD_IMEDIAT && (idEntitate == 0 || tipEndpoint == null)) {
            Toast.makeText(this, "Date lipsa pentru pornirea activitatii", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        MaterialToolbar toolbar = findViewById(R.id.topToolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> anuleazaSiInchide());

        viewFinder = findViewById(R.id.viewFinder);
        imageResult = findViewById(R.id.imageResult);
        layoutCameraActive = findViewById(R.id.layoutCameraActive);
        layoutPozaValidare = findViewById(R.id.layoutPozaValidare);
        btnFaPoza = findViewById(R.id.btnFaPoza);
        btnRetakePoza = findViewById(R.id.btnRetakePoza);
        btnConfirmaPoza = findViewById(R.id.btnConfirmaPoza);
        btnAnuleaza = findViewById(R.id.btnAnuleaza);

        cameraExecutor = Executors.newSingleThreadExecutor();

        imagineViewModel = new ViewModelProvider(this, new ImagineViewModelFactory(this))
                .get(ImagineViewModel.class);

        imagineViewModel.getImagineResult().observe(this, result -> {
            setButoaneActive(true);

            if (mod == MODE_DOAR_CAPTURA) {
                aFostConfirmata = true;
                Intent rezultat = new Intent();
                rezultat.putExtra(EXTRA_FISIER_CAPTURAT, pozaTemporara.getAbsolutePath());
                setResult(RESULT_OK, rezultat);
                finish();
                return;
            }

            if (result.getSuccess() != null) {
                stergePozaTemporara();
                Toast.makeText(this, "Imagine incarcata cu succes", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else if (result.getFieldErrors() != null) {
                afiseazaEroriValidare(result.getFieldErrors());
            } else {
                Toast.makeText(this, result.getErrorMessage(), Toast.LENGTH_LONG).show();
                // pastram poza locala, userul poate incerca din nou (retry) fara sa refaca poza
            }
        });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), acordat -> {
                    if (acordat) {
                        porneseCamera();
                    } else {
                        Toast.makeText(this, "Permisiunea de camera este necesara", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            porneseCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        btnFaPoza.setOnClickListener(v -> facePoza());
        btnRetakePoza.setOnClickListener(v -> retakePoza());
        btnConfirmaPoza.setOnClickListener(v -> confirmaPoza());
        btnAnuleaza.setOnClickListener(v -> anuleazaSiInchide());
    }

    private void porneseCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Eroare pornire camera", e);
                Toast.makeText(this, "Camera indisponibila", Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void facePoza() {
        if (imageCapture == null) {
            return;
        }

        String numeFisierTemp = "temp-" +
                new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new java.util.Date()) +
                ".png";

        File fisierIesire = new File(getCacheDir(), numeFisierTemp); // stocare locala, doar in cache-ul aplicatiei

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(fisierIesire).build();

        btnFaPoza.setEnabled(false);

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        pozaTemporara = fisierIesire;
                        afiseazaPozaCapturata();
                        btnFaPoza.setEnabled(true);
                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        if (BuildConfig.DEBUG) Log.e(TAG, "Eroare captura poza", exception);
                        Toast.makeText(AdaugaImagini.this, "Eroare la realizarea pozei", Toast.LENGTH_LONG).show();
                        btnFaPoza.setEnabled(true);
                    }
                });
    }

    private void afiseazaPozaCapturata() {
        imageResult.setImageURI(android.net.Uri.fromFile(pozaTemporara));
        imageResult.setVisibility(android.view.View.VISIBLE);
        viewFinder.setVisibility(android.view.View.GONE);

        layoutCameraActive.setVisibility(android.view.View.GONE);
        layoutPozaValidare.setVisibility(android.view.View.VISIBLE);
    }

    private void retakePoza() {
        stergePozaTemporara();

        imageResult.setVisibility(android.view.View.GONE);
        viewFinder.setVisibility(android.view.View.VISIBLE);

        layoutPozaValidare.setVisibility(android.view.View.GONE);
        layoutCameraActive.setVisibility(android.view.View.VISIBLE);
    }

    private void confirmaPoza() {
        if (pozaTemporara == null || !pozaTemporara.exists()) {
            Toast.makeText(this, "Nicio poza de trimis", Toast.LENGTH_LONG).show();
            return;
        }

        if (mod == MODE_DOAR_CAPTURA) {
            aFostConfirmata = true;
            Intent rezultat = new Intent();
            rezultat.putExtra(EXTRA_FISIER_CAPTURAT, pozaTemporara.getAbsolutePath());
            setResult(RESULT_OK, rezultat);
            finish();
            return;
        }
        String token = TokenStorage.getToken(getApplicationContext());
        if (token == null) {
            Toast.makeText(this, "Sesiune expirata, log in din nou", Toast.LENGTH_LONG).show();
            return;
        }

        ImagineRequestModel requestModel = new ImagineRequestModel();
        requestModel.fisier = pozaTemporara;
        requestModel.awb = awb;

        setButoaneActive(false);
        imagineViewModel.uploadImagine(requestModel, idEntitate, tipEndpoint, token);
    }

    private void setButoaneActive(boolean active) {
        btnConfirmaPoza.setEnabled(active);
        btnRetakePoza.setEnabled(active);
        btnAnuleaza.setEnabled(active);
    }

    private void afiseazaEroriValidare(Map<String, String> fieldErrors) {
        StringBuilder mesaj = new StringBuilder();
        for (Map.Entry<String, String> entry : fieldErrors.entrySet()) {
            mesaj.append(entry.getValue()).append("\n");
        }
        Toast.makeText(this, mesaj.toString().trim(), Toast.LENGTH_LONG).show();
        // pastram poza locala; userul poate retake sau incerca din nou dupa ce corecteaza ceva pe server, daca e cazul
    }

    private void anuleazaSiInchide() {
        stergePozaTemporara();
        setResult(RESULT_CANCELED);
        finish();
    }

    private void stergePozaTemporara() {
        if (pozaTemporara != null && pozaTemporara.exists()) {
            boolean stearsa = pozaTemporara.delete();
            if (!stearsa && BuildConfig.DEBUG) {
                Log.e(TAG, "Nu s-a putut sterge poza temporara: " + pozaTemporara.getAbsolutePath());
            }
            pozaTemporara = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!aFostConfirmata) {
            stergePozaTemporara();
        }
        cameraExecutor.shutdown();
    }
}