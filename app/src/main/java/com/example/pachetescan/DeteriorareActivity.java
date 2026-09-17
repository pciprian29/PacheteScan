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
import com.example.pachetescan.data.deteriorare.model.CreateDeteriorareRequestModel;
import com.example.pachetescan.ui.deteriorare.DeteriorareResult;
import com.example.pachetescan.ui.deteriorare.DeteriorareViewModel;
import com.example.pachetescan.ui.deteriorare.DeteriorareViewModelFactory;
import com.example.pachetescan.ui.imagini.ImagineViewModel;
import com.example.pachetescan.ui.imagini.ImagineViewModelFactory;

import com.example.pachetescan.ui.imagini.UploadCuRetryHelper;
import com.google.android.material.appbar.MaterialToolbar;

import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.InvalidScannerNameException;
import com.honeywell.aidc.ScannerUnavailableException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class DeteriorareActivity extends AppCompatActivity implements BarcodeReader.BarcodeListener {

    public final String TAG = "DeteriorareActivity";
    AidcManager manager;
    BarcodeReader reader;
    com.google.android.material.textfield.TextInputEditText etAwbPachet;

    CreateDeteriorareRequestModel lastRequest;
    DeteriorareViewModel deteriorareViewModel;
    ImagineViewModel imagineViewModel;

    private com.google.android.material.textfield.TextInputEditText etDescriere;
    private com.google.android.material.textfield.TextInputEditText etLocatieDeteriorare;

    TextView tvPozeAdaugate;
    LinearLayout layoutThumbnail;
    ExecutorService thumbnailExecutor = Executors.newSingleThreadExecutor();
    Button btnFaPoza;
    Button btnAdaugaDeteriorare;
    Button btnAnuleaza;
    boolean uploadInCurs = false;

    List<File> pozeQueued = new ArrayList<>();
    AlertDialog dialogProgres;
    Handler mainHandler = new Handler(Looper.getMainLooper());

    ActivityResultLauncher<Intent> capturaPozaLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_deteriorare);

        AidcManager.create(this, aidcManager -> {
            manager = aidcManager;
            try{
                reader = aidcManager.createBarcodeReader();
                if(reader != null){
                    try{
                        reader.addBarcodeListener(DeteriorareActivity.this);
                        reader.claim();
                    }catch(ScannerUnavailableException ex){
                        if (BuildConfig.DEBUG) Log.e (TAG, "ScannerUnavailableException :: " + ex.getMessage(), ex);
                        Toast.makeText(DeteriorareActivity.this, "Scanner indisponibil", Toast.LENGTH_LONG).show();
                    }
                }

            }catch (InvalidScannerNameException ex){
                if (BuildConfig.DEBUG) Log.e (TAG, "Scanner ul nu exista");
            }
        });

        MaterialToolbar toolbar = findViewById(R.id.topToolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        etAwbPachet = findViewById(R.id.etAwbPachet);
        etLocatieDeteriorare = findViewById(R.id.etLocatieDeteriorare);
        etDescriere = findViewById(R.id.etDescriereDeteriorare);

        tvPozeAdaugate = findViewById(R.id.tvPozeAdaugate);
        layoutThumbnail = findViewById(R.id.layoutThumbnail);
        btnFaPoza = findViewById(R.id.btnFaPoza);
        btnAdaugaDeteriorare = findViewById(R.id.btnAdaugaDeteriorare);
        btnAnuleaza = findViewById(R.id.btnAnuleaza);

        deteriorareViewModel = new ViewModelProvider(this, new DeteriorareViewModelFactory(getApplicationContext()))
                .get(DeteriorareViewModel.class);
        deteriorareViewModel.getDeteriorareResult().observe(this, this::handleDeteriorareResult);

        imagineViewModel = new ViewModelProvider(this, new ImagineViewModelFactory(this))
                .get(ImagineViewModel.class);

        capturaPozaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String cale = result.getData().getStringExtra(AdaugaImagini.EXTRA_FISIER_CAPTURAT);
                        if (cale != null) {
                            pozeQueued.add(new File(cale));
                            actualizeazaListaPoze();
                        }
                    }
                });

        btnFaPoza.setOnClickListener(v -> faPoza());
        btnAdaugaDeteriorare.setOnClickListener(v -> submitDeterioare());
        btnAnuleaza.setOnClickListener(v -> anuleazaSiInchide());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void faPoza() {
        Intent intent = new Intent(this, AdaugaImagini.class);
        intent.putExtra(AdaugaImagini.EXTRA_MODE, AdaugaImagini.MODE_DOAR_CAPTURA);
        capturaPozaLauncher.launch(intent);
    }

    private void actualizeazaListaPoze() {
        int n = pozeQueued.size();
        tvPozeAdaugate.setText(n + (n == 1 ? " poza adaugata" : " poze adaugate"));
        afiseazaThumbnail();
    }

    private void afiseazaThumbnail(){
        layoutThumbnail.removeAllViews();

        for(File poza : pozeQueued){
            ImageView thumb = new ImageView(this);
            int dimensiune = dpToPx(70);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dimensiune, dimensiune);
            params.setMarginEnd(dpToPx(8));
            thumb.setLayoutParams(params);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

            thumb.setOnLongClickListener(v -> {
                confirmaStergerePoza(poza);
                return true;
            });

            layoutThumbnail.addView(thumb);

            thumbnailExecutor.execute(() -> {
                Bitmap bitmap = decodeazaEficient(poza.getAbsolutePath(), dimensiune);
                mainHandler.post(() -> {
                    if (bitmap != null) {
                        thumb.setImageBitmap(bitmap);
                    }
                });
            });
        }
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

    private void confirmaStergerePoza(File poza){
        Bitmap preview = decodeazaEficient(poza.getAbsolutePath(), dpToPx(100));
        Drawable iconPreview = preview != null
                ? new BitmapDrawable(getResources(), preview)
                : null;

        new AlertDialog.Builder(this)
                .setTitle("Sterge Poza")
                .setIcon(iconPreview)
                .setMessage("Vrei sa stergi poza?")
                .setPositiveButton("Da", (dialog, which) -> {
                    pozeQueued.remove(poza);
                    actualizeazaListaPoze();
                })
                .setNegativeButton("Nu", null)
                .show();
    }



    @Override
    protected void onResume(){
        super.onResume();
        if(reader != null){
            try{
                reader.claim();
            }catch(ScannerUnavailableException ex){
                if (BuildConfig.DEBUG) Log.e (TAG, "ScannerUnavailableException :: " + ex.getMessage(), ex);
                Toast.makeText(DeteriorareActivity.this, "Scanner indisponibil", Toast.LENGTH_LONG).show();
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
        if (!uploadInCurs) {
            for (File f : pozeQueued) {
                if (f.exists()) f.delete();
            }
        }
        if(BuildConfig.DEBUG) Log.v(TAG, "Override onDestroy");
        thumbnailExecutor.shutdown();
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
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent){
        if(BuildConfig.DEBUG) Log.e (TAG, "Barcode reader failed");
    }

    public void submitDeterioare(){
        CreateDeteriorareRequestModel request = buildRequestFromForm();
        if(request == null){
            return;
        }

        lastRequest = request;

        String token = TokenStorage.getToken(getApplicationContext());
        if(token == null){
            Toast.makeText(this, "Sesiune expirata, log in din nou", Toast.LENGTH_LONG).show();
            return;
        }

        setFormularActiv(false);
        deteriorareViewModel.createDeteriorare(request, token);
    }

    public CreateDeteriorareRequestModel buildRequestFromForm(){
        CreateDeteriorareRequestModel request = new CreateDeteriorareRequestModel();
        request.awb = etAwbPachet.getText().toString().trim();
        request.locatieDeteriorare = etLocatieDeteriorare.getText().toString().trim();
        request.descriere = etDescriere.getText().toString().trim();
        if(request.awb.isEmpty() || request.locatieDeteriorare.isEmpty() || request.descriere.isEmpty()){
            Toast.makeText(this, "Completati toate campurile", Toast.LENGTH_SHORT).show();
            return null;
        }
        return request;
    }

    public void handleDeteriorareResult(DeteriorareResult result){
        if(result == null) return;

        if(result.getSuccess() != null){
            int idDeteriorareNoua = result.getSuccess().idDeteriorare; //

            if (pozeQueued.isEmpty()) {
                Toast.makeText(this, "Deteriorare creata! AWB: " + result.getSuccess().awb, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            String token = TokenStorage.getToken(getApplicationContext());
            aratatDialogProgres();

            uploadInCurs = true; ///teste ca nu merge

            UploadCuRetryHelper.proceseazaCoada(
                    pozeQueued, lastRequest.awb, idDeteriorareNoua, "deteriorare", token,
                    imagineViewModel, mainHandler,
                    new UploadCuRetryHelper.RezultatCoada() {
                        @Override
                        public void onProgres(int curent, int total) {
                            runOnUiThread(() -> actualizeazaDialogProgres(curent, total));
                        }

                        @Override
                        public void onFinalizat(List<File> reusite, List<File> esuate) {
                            uploadInCurs = false;
                            runOnUiThread(() -> {
                                for (File f : reusite) f.delete();
                                dialogProgres.dismiss();

                                if (esuate.isEmpty()) {
                                    Toast.makeText(DeteriorareActivity.this, "Deteriorare si toate pozele au fost salvate", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    aratatDialogEsecPartial(reusite.size(), esuate, idDeteriorareNoua, token);
                                }
                            });
                        }
                    });

        }else if (result.getFieldErrors() != null && !result.getFieldErrors().isEmpty()) {
            setFormularActiv(true);
            StringBuilder sb = new StringBuilder();
            for (var entry : result.getFieldErrors().entrySet()) {
                sb.append(entry.getValue()).append("\n");
            }
            Toast.makeText(this, sb.toString().trim(), Toast.LENGTH_LONG).show();

        } else if (result.getErrorMessage() != null) {
            setFormularActiv(true);
            Toast.makeText(this, result.getErrorMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setFormularActiv(boolean activ) {
        btnAdaugaDeteriorare.setEnabled(activ);
        btnFaPoza.setEnabled(activ);
        btnAnuleaza.setEnabled(activ);
    }

    private void aratatDialogProgres() {
        dialogProgres = new AlertDialog.Builder(this)
                .setTitle("Se incarca pozele")
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

    private void aratatDialogEsecPartial(int nrReusite, List<File> esuate, int idDeteriorare, String token) {
        new AlertDialog.Builder(this)
                .setTitle("Cateva poze nu s-au incarcat")
                .setMessage(nrReusite + " poze salvate cu succes, " + esuate.size() + " au esuat. Incerci din nou pozele esuate?")
                .setPositiveButton("Reincearca", (dialog, which) -> {
                    aratatDialogProgres();
                    UploadCuRetryHelper.proceseazaCoada(
                            esuate, lastRequest.awb, idDeteriorare, "deteriorare", token,
                            imagineViewModel, mainHandler,
                            new UploadCuRetryHelper.RezultatCoada() {
                                @Override
                                public void onProgres(int curent, int total) {
                                    runOnUiThread(() -> actualizeazaDialogProgres(curent, total));
                                }

                                @Override
                                public void onFinalizat(List<File> reusite, List<File> nouEsuate) {
                                    uploadInCurs = false;
                                    runOnUiThread(() -> {
                                        for (File f : reusite) f.delete();
                                        dialogProgres.dismiss();

                                        if (nouEsuate.isEmpty()) {
                                            Toast.makeText(DeteriorareActivity.this, "Toate pozele au fost salvate", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            aratatDialogEsecPartial(reusite.size(), nouEsuate, idDeteriorare, token);
                                        }
                                    });
                                }
                            });
                })
                .setNegativeButton("Renunta la pozele ramase", (dialog, which) -> {
                    for (File f : esuate) if (f.exists()) f.delete();
                    Toast.makeText(this, "Deteriorare salvata, " + nrReusite + " poze incarcate", Toast.LENGTH_LONG).show();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void anuleazaSiInchide() {
        for (File f : pozeQueued) if (f.exists()) f.delete();
        finish();
    }
}