package com.example.pachetescan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.pachetescan.data.TokenStorage;
import com.example.pachetescan.data.infolipsa.model.CreateInfoLipsaRequestModel;
import com.example.pachetescan.ui.imagini.ImagineViewModel;
import com.example.pachetescan.ui.imagini.ImagineViewModelFactory;
import com.example.pachetescan.ui.imagini.UploadCuRetryHelper;
import com.example.pachetescan.ui.infolipsa.InfoLipsaViewModel;
import com.example.pachetescan.ui.infolipsa.InfoLipsaViewModelFactory;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.InvalidScannerNameException;
import com.honeywell.aidc.ScannerUnavailableException;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfoLipsaActivity extends AppCompatActivity implements BarcodeReader.BarcodeListener {

    public final String TAG = "InfoLipsaActivity";

    AidcManager manager;
    BarcodeReader reader;

    TextInputEditText etAwbPachet;
    TextInputEditText etCampAfectat;
    TextInputEditText etDescriereInfoLipsa;
    MaterialCheckBox esteAwbLipsa;
    AutoCompleteTextView dropdownTipLipsa;
    TextView atentionareDescriere;
    com.google.android.material.textfield.TextInputLayout awbPachetContainer;
    com.google.android.material.textfield.TextInputLayout campAfectatContainer;
    TextView tvPozaEticheta;
    LinearLayout layoutThumbnail;
    ExecutorService thumbnailExecutor = Executors.newSingleThreadExecutor();
    Button btnFaPoza;
    Button btnSubmit;

    CreateInfoLipsaRequestModel lastRequest;
    InfoLipsaViewModel infoLipsaViewModel;

    File pozaEticheta;
    ImagineViewModel imagineViewModel;
    Handler mainHandler = new Handler(Looper.getMainLooper());
    AlertDialog dialogProgres;
    ActivityResultLauncher<Intent> capturaPozaLauncher;
    boolean uploadInCurs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_info_lipsa);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.topToolbar);
        dropdownTipLipsa = findViewById(R.id.dropdownTipLipsa);
        esteAwbLipsa = findViewById(R.id.esteAwbLipsa);
        etAwbPachet = findViewById(R.id.etAwbPachet);
        etCampAfectat = findViewById(R.id.etCampAfectat);
        awbPachetContainer = findViewById(R.id.awbPachet);
        campAfectatContainer = findViewById(R.id.campAfectat);
        etDescriereInfoLipsa = findViewById(R.id.etDescriereInfoLipsa);
        atentionareDescriere = findViewById(R.id.atentionareDescriere);
        Button btnAnuleaza = findViewById(R.id.btnAnuleaza);
        tvPozaEticheta = findViewById(R.id.tvPozaEticheta);
        layoutThumbnail = findViewById(R.id.layoutThumbnail);
        btnFaPoza = findViewById(R.id.btnFaPozaEticheta);
        btnSubmit = findViewById(R.id.btnAdaugaInfoLipsa);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        infoLipsaViewModel = new ViewModelProvider(this, new InfoLipsaViewModelFactory(this))
                .get(InfoLipsaViewModel.class);
        imagineViewModel = new ViewModelProvider(this, new ImagineViewModelFactory(this))
                .get(ImagineViewModel.class);

        capturaPozaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String cale = result.getData().getStringExtra(AdaugaImagini.EXTRA_FISIER_CAPTURAT);
                        if (cale != null) {
                            pozaEticheta = new File(cale);
                            tvPozaEticheta.setText("Poza atasata");
                            afiseazaThumbnailEticheta();
                        }
                    }
                });

        infoLipsaViewModel.getInfoLipsaResult().observe(this, result -> {
            btnSubmit.setEnabled(true);

            if (result.getSuccess() != null) {
                int idInfoLipsaNoua = result.getSuccess().idInfoLipsa;

                if (pozaEticheta == null) {
                    Toast.makeText(this, "Info lipsa inregistrata", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                String token = TokenStorage.getToken(getApplicationContext());
                String awbPentruUpload = lastRequest.esteAwbLipsa ? null : lastRequest.awb;

                uploadInCurs = true;
                aratatDialogProgres();

                UploadCuRetryHelper.proceseazaCoada(
                        Collections.singletonList(pozaEticheta),
                        awbPentruUpload, idInfoLipsaNoua, "infolipsa", token,
                        imagineViewModel, mainHandler,
                        new UploadCuRetryHelper.RezultatCoada() {
                            @Override
                            public void onProgres(int curent, int total) {
                                runOnUiThread(() -> actualizeazaDialogProgres(curent, total));
                            }

                            @Override
                            public void onFinalizat(List<File> reusite, List<File> esuate) {
                                runOnUiThread(() -> {
                                    uploadInCurs = false;
                                    for (File f : reusite) f.delete();
                                    dialogProgres.dismiss();

                                    if (esuate.isEmpty()) {
                                        pozaEticheta = null;
                                        Toast.makeText(InfoLipsaActivity.this, "Info lipsa si poza au fost salvate", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        aratatDialogEsecPartial(esuate, idInfoLipsaNoua, token, awbPentruUpload);
                                    }
                                });
                            }
                        });

            } else if (result.getFieldErrors() != null) {
                afiseazaEroriCamp(result.getFieldErrors());
            } else {
                Toast.makeText(this, result.getErrorMessage(), Toast.LENGTH_LONG).show();
            }
        });

        AidcManager.create(this, aidcManager -> {
            manager = aidcManager;
            try {
                reader = aidcManager.createBarcodeReader();
                if (reader != null) {
                    try {
                        reader.addBarcodeListener(InfoLipsaActivity.this);
                        reader.claim();
                    } catch (ScannerUnavailableException ex) {
                        if (BuildConfig.DEBUG) Log.e(TAG, "ScannerUnavailableException :: " + ex.getMessage(), ex);
                        Toast.makeText(InfoLipsaActivity.this, "Scanner indisponibil", Toast.LENGTH_LONG).show();
                    }
                }
            } catch (InvalidScannerNameException ex) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Scanner ul nu exista");
            }
        });

        String[] optiuniTipLipsa = {"Partiala", "Lipsa"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, optiuniTipLipsa);
        dropdownTipLipsa.setAdapter(adapter);

        esteAwbLipsa.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                awbPachetContainer.setVisibility(View.GONE);
                campAfectatContainer.setVisibility(View.GONE);

                if (atentionareDescriere != null) {
                    atentionareDescriere.setVisibility(View.VISIBLE);
                }

                etCampAfectat.setText("AWB");
                etAwbPachet.setText("");
            } else {
                awbPachetContainer.setVisibility(View.VISIBLE);
                campAfectatContainer.setVisibility(View.VISIBLE);

                if (atentionareDescriere != null) {
                    atentionareDescriere.setVisibility(View.GONE);
                }

                etCampAfectat.setText("");
            }
        });

        btnAnuleaza.setOnClickListener(v -> finish());
        btnFaPoza.setOnClickListener(v -> faPozaEticheta());
        btnSubmit.setOnClickListener(v -> submitInfoLipsa());

        etAwbPachet.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void afterTextChanged(android.text.Editable s) {
                awbPachetContainer.setError(null);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        etCampAfectat.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void afterTextChanged(android.text.Editable s) {
                etCampAfectat.setError(null);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        etDescriereInfoLipsa.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void afterTextChanged(android.text.Editable s) {
                etDescriereInfoLipsa.setError(null);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        findViewById(R.id.main).post(() -> {
            findViewById(R.id.main).requestLayout();
        });
    }

    private void faPozaEticheta() {
        Intent intent = new Intent(this, AdaugaImagini.class);
        intent.putExtra(AdaugaImagini.EXTRA_MODE, AdaugaImagini.MODE_DOAR_CAPTURA);
        capturaPozaLauncher.launch(intent);
    }

    private void afiseazaThumbnailEticheta() {
        layoutThumbnail.removeAllViews();

        if (pozaEticheta == null) {
            return;
        }

        ImageView thumb = new ImageView(this);
        int dimensiune = dpToPx(70);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dimensiune, dimensiune);
        thumb.setLayoutParams(params);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumb.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

        thumb.setOnLongClickListener(v -> {
            confirmaStergerePozaEticheta();
            return true;
        });

        layoutThumbnail.addView(thumb);

        File pozaCurenta = pozaEticheta;
        thumbnailExecutor.execute(() -> {
            Bitmap bitmap = decodeazaEficient(pozaCurenta.getAbsolutePath(), dimensiune);
            mainHandler.post(() -> {
                if (bitmap != null) {
                    thumb.setImageBitmap(bitmap);
                }
            });
        });
    }

    private void confirmaStergerePozaEticheta() {
        Bitmap preview = decodeazaEficient(pozaEticheta.getAbsolutePath(), dpToPx(100));
        Drawable iconPreview = preview != null ? new BitmapDrawable(getResources(), preview) : null;

        new AlertDialog.Builder(this)
                .setTitle("Sterge poza")
                .setIcon(iconPreview)
                .setMessage("Vrei sa elimini poza etichetei?")
                .setPositiveButton("Sterge", (dialog, which) -> {
                    if (pozaEticheta.exists()) {
                        pozaEticheta.delete();
                    }
                    pozaEticheta = null;
                    tvPozaEticheta.setText("Nicio poza atasata");
                    layoutThumbnail.removeAllViews();
                })
                .setNegativeButton("Anuleaza", null)
                .show();
    }

    private Bitmap decodeazaEficient(String caleFisier, int dimensiuneTintaPx) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(caleFisier, options);

        int inSampleSize = 1;
        int latimeOriginala = options.outWidth;
        int inaltimeOriginala = options.outHeight;

        while ((latimeOriginala / inSampleSize) > dimensiuneTintaPx * 2
                || (inaltimeOriginala / inSampleSize) > dimensiuneTintaPx * 2) {
            inSampleSize *= 2;
        }

        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(caleFisier, options);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reader != null) {
            try {
                reader.claim();
            } catch (ScannerUnavailableException ex) {
                if (BuildConfig.DEBUG) Log.e(TAG, "ScannerUnavailableException :: " + ex.getMessage(), ex);
                Toast.makeText(InfoLipsaActivity.this, "Scanner indisponibil", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!uploadInCurs && pozaEticheta != null && pozaEticheta.exists()) {
            pozaEticheta.delete();
        }
    }

    @Override
    public void onBarcodeEvent(BarcodeReadEvent barcodeReadEvent) {
        final String data = barcodeReadEvent.getBarcodeData();

        runOnUiThread(() -> {
            if (data != null && !data.trim().isEmpty()) {

                String textScanat = data.trim();
                String regex = "^AWB-\\d{8}-[A-Z0-9]{10}$";
                Pattern pattern = Pattern.compile(regex);

                Matcher matcher = pattern.matcher(textScanat);
                if (matcher.matches()) {
                    etAwbPachet.setText(textScanat);
                    findViewById(android.R.id.content).performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                    Toast.makeText(this, "AWB Scanat: " + textScanat, Toast.LENGTH_SHORT).show();
                } else {
                    if (BuildConfig.DEBUG) Log.e(TAG, "AWB Invalid");
                    Toast.makeText(this, "AWB Invalid", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent) {
        if (BuildConfig.DEBUG) Log.e(TAG, "Barcode reader failed");
    }

    public void submitInfoLipsa() {
        CreateInfoLipsaRequestModel request = buildRequestFromForm();
        if (request == null) {
            return;
        }

        lastRequest = request;

        String token = TokenStorage.getToken(getApplicationContext());
        if (token == null) {
            Toast.makeText(this, "Sesiune expirata, log in din nou", Toast.LENGTH_LONG).show();
            return;
        }
        if (pozaEticheta == null) {
            Toast.makeText(this, "Poza la eticheta este obligatorie", Toast.LENGTH_LONG).show();
            return;
        }

        btnSubmit.setEnabled(false);
        infoLipsaViewModel.createInfoLipsa(request, token);
    }

    private CreateInfoLipsaRequestModel buildRequestFromForm() {
        CreateInfoLipsaRequestModel request = new CreateInfoLipsaRequestModel();
        request.esteAwbLipsa = esteAwbLipsa.isChecked();
        request.awb = etAwbPachet.getText().toString().trim();
        request.campAfectat = etCampAfectat.getText().toString().trim();
        request.descriereInfoLipsa = etDescriereInfoLipsa.getText().toString().trim();

        String tipSelectat = dropdownTipLipsa.getText().toString();
        request.idTipLipsa = tipSelectat.equals("Partiala") ? 1 : 2;

        return request;
    }

    private void afiseazaEroriCamp(Map<String, String> fieldErrors) {
        awbPachetContainer.setError(null);
        campAfectatContainer.setError(null);
        etDescriereInfoLipsa.setError(null);

        for (Map.Entry<String, String> entry : fieldErrors.entrySet()) {
            String camp = entry.getKey();
            String mesaj = entry.getValue();

            switch (camp.toLowerCase()) {
                case "awb":
                    awbPachetContainer.setError(mesaj);
                    break;
                case "ecampafectat":
                    campAfectatContainer.setError(mesaj);
                    break;
                case "descriereinfolipsa":
                    etDescriereInfoLipsa.setError(mesaj);
                    Toast.makeText(this, mesaj, Toast.LENGTH_LONG).show();
                    break;
                case "idtiplipsa":
                    Toast.makeText(this, mesaj, Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(this, camp + ": " + mesaj, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void aratatDialogProgres() {
        dialogProgres = new AlertDialog.Builder(this)
                .setTitle("Se incarca poza")
                .setMessage("Pregatire...")
                .setCancelable(false)
                .create();
        dialogProgres.show();
    }

    private void actualizeazaDialogProgres(int curent, int total) {
        if (dialogProgres != null && dialogProgres.isShowing()) {
            dialogProgres.setMessage("Poza " + curent + " din " + total + "...");
        }
    }

    private void aratatDialogEsecPartial(List<File> esuate, int idInfoLipsa, String token, String awbPentruUpload) {
        new AlertDialog.Builder(this)
                .setTitle("Poza nu s-a incarcat")
                .setMessage("Info lipsa a fost inregistrata, dar poza nu a putut fi salvata. Incerci din nou?")
                .setPositiveButton("Reincearca", (dialog, which) -> {
                    uploadInCurs = true;
                    aratatDialogProgres();
                    UploadCuRetryHelper.proceseazaCoada(
                            esuate, awbPentruUpload, idInfoLipsa, "infolipsa", token,
                            imagineViewModel, mainHandler,
                            new UploadCuRetryHelper.RezultatCoada() {
                                @Override
                                public void onProgres(int curent, int total) {
                                    runOnUiThread(() -> actualizeazaDialogProgres(curent, total));
                                }

                                @Override
                                public void onFinalizat(List<File> reusite, List<File> nouEsuate) {
                                    runOnUiThread(() -> {
                                        uploadInCurs = false;
                                        for (File f : reusite) f.delete();
                                        dialogProgres.dismiss();

                                        if (nouEsuate.isEmpty()) {
                                            pozaEticheta = null;
                                            Toast.makeText(InfoLipsaActivity.this, "Poza a fost salvata", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            aratatDialogEsecPartial(nouEsuate, idInfoLipsa, token, awbPentruUpload);
                                        }
                                    });
                                }
                            });
                })
                .setNegativeButton("Renunta la poza", (dialog, which) -> {
                    for (File f : esuate) if (f.exists()) f.delete();
                    pozaEticheta = null;
                    Toast.makeText(this, "Info lipsa salvata, fara poza", Toast.LENGTH_LONG).show();
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}