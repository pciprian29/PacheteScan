package com.example.pachetescan;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.lifecycle.ViewModelProvider;

import com.example.pachetescan.data.TokenStorage;
import com.example.pachetescan.ui.pachet.PachetViewModelFactory;
import com.example.pachetescan.ui.pachet.PachetResult;
import com.example.pachetescan.data.pachet.model.CreatePachetRequestModel;
import com.example.pachetescan.ui.pachet.PachetViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.InvalidScannerNameException;
import com.honeywell.aidc.ScannerUnavailableException;

import java.text.NumberFormat;

public class AdaugaPachetActivity extends AppCompatActivity implements BarcodeReader.BarcodeListener {

    public static final String TAG = "PacheteScan AdaugaPachetActivity";
    private AidcManager manager;
    private BarcodeReader reader;

    private com.google.android.material.textfield.TextInputEditText etGreutateTeoretica;
    private com.google.android.material.textfield.TextInputEditText etGreutateEfectiva;
    private AutoCompleteTextView dropdownTipPachet;
    long startTime;

    private PachetViewModel pachetViewModel;

    private com.google.android.material.textfield.TextInputEditText etDescriere;
    private com.google.android.material.textfield.TextInputEditText etEmailExpeditor;
    private com.google.android.material.textfield.TextInputEditText etAdresaExpeditor;
    private com.google.android.material.textfield.TextInputEditText etNumeExpeditor;
    private com.google.android.material.textfield.TextInputEditText etPrenumeExpeditor;
    private com.google.android.material.textfield.TextInputEditText etEmailDestinatar;
    private com.google.android.material.textfield.TextInputEditText etAdresaDestinatar;
    private com.google.android.material.textfield.TextInputEditText etNumeDestinatar;
    private com.google.android.material.textfield.TextInputEditText etPrenumeDestinatar;
    private RadioGroup rgTipExpeditor;
    private RadioGroup rgTipDestinatar;
    private AutoCompleteTextView dropdownStatus;

    private CreatePachetRequestModel lastRequest;

    private static final java.util.Map<String, Integer> TIP_PACHET_IDS = new java.util.HashMap<>() {{
        put("Plic", 1);
        put("Cutie", 2);
        put("Palet", 3);
    }};

    private static final java.util.Map<String, Integer> STATUS_PACHET_IDS = new java.util.HashMap<>() {{
        put("Inregistrat", 1);
        put("In Tranzit", 2);
        put("Livrat", 3);
        put("Deteriorat", 4);
        put("Informatii Lipsa", 5);
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        startTime = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_adauga_pachet);
        long timpTrecut = System.currentTimeMillis() - startTime;

        Log.d(TAG, "Timp pt setContentView: " + timpTrecut + " ms");

        startTime = System.currentTimeMillis();

        AidcManager.create(this, aidcManager -> {
            manager = aidcManager;
            try{
                reader = aidcManager.createBarcodeReader();
                if(reader != null){
                    try{
                        reader.addBarcodeListener(AdaugaPachetActivity.this);
                        reader.claim();
                    }catch(ScannerUnavailableException ex){
                        if (BuildConfig.DEBUG) Log.e (TAG, "ScannerUnavailableException :: " + ex.getMessage(), ex);
                        Toast.makeText(AdaugaPachetActivity.this, "Scanner indisponibil", Toast.LENGTH_LONG).show();
                    }
                }

            }catch (InvalidScannerNameException ex){
                if (BuildConfig.DEBUG) Log.e(TAG, "Scannerul nu exista");
            }
        });

        MaterialToolbar toolbar = findViewById(R.id.topToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            finish();
        });

        etGreutateTeoretica = findViewById(R.id.etgreutateTeoretica);
        etGreutateEfectiva = findViewById(R.id.etgreutateEfectiva);

        // --- câmpuri noi, pentru submit ---
        etDescriere = findViewById(R.id.etDescrierePachet);
        etEmailExpeditor = findViewById(R.id.etEmailExpeditor);
        etAdresaExpeditor = findViewById(R.id.etAdresaExpeditor);
        etNumeExpeditor = findViewById(R.id.etNumeExpeditor);
        etPrenumeExpeditor = findViewById(R.id.etPrenumeExpeditor);
        etEmailDestinatar = findViewById(R.id.etEmailDestinatar);
        etAdresaDestinatar = findViewById(R.id.etAdresaDestinatar);
        etNumeDestinatar = findViewById(R.id.etNumeDestinatar);
        etPrenumeDestinatar = findViewById(R.id.etPrenumeDestinatar);

        pachetViewModel = new ViewModelProvider(this, new PachetViewModelFactory(getApplicationContext()))
                .get(PachetViewModel.class);
        pachetViewModel.getPachetResult().observe(this, this::handlePachetResult);

//  -----------------------------------------------------------------
//        Functionalitati pagina

        String[] optiuni = {"Plic", "Cutie", "Palet"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, optiuni);
        dropdownTipPachet = findViewById(R.id.dropdownTipPachet);
        dropdownTipPachet.setAdapter(adapter);

        rgTipExpeditor = findViewById(R.id.rgTipExpeditor);
        LinearLayout containerExpNumePrenume = findViewById(R.id.containerExpNumePrenume);
        containerExpNumePrenume.setVisibility(View.GONE);

        rgTipDestinatar = findViewById(R.id.rgTipDestinatar);
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

        dropdownStatus = findViewById(R.id.dropdownStatusPachet);
        dropdownStatus.setAdapter(adapterStatus);

        Button btnCreeaza = findViewById(R.id.btnCreeaza);
        btnCreeaza.setOnClickListener(v -> submitPachet());

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
        Log.d(TAG, "Timp total onCreate: " + (System.currentTimeMillis() - startTime) + " ms");
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(reader != null){
            try {
                reader.claim();
                if(BuildConfig.DEBUG) Log.v(TAG, "Scanner Claimed");
            }catch(ScannerUnavailableException ex){
                if (BuildConfig.DEBUG) Log.e (TAG, "ScannerUnavailableException :: " + ex.getMessage(), ex);
                Toast.makeText(this, "Scanner indisponibil", Toast.LENGTH_LONG).show();
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
        if(BuildConfig.DEBUG) Log.v(TAG, "Override onDestroy");
    }

    @Override
    public void onBarcodeEvent(BarcodeReadEvent barcodeReadEvent){
        final String data = barcodeReadEvent.getBarcodeData();

        runOnUiThread(() ->{
            if(data != null && !data.trim().isEmpty()){

                String textScanat = data.trim();

                if(textScanat.equalsIgnoreCase("Plic")|| textScanat.equalsIgnoreCase("Cutie")|| textScanat.equalsIgnoreCase("Palet")){
                    dropdownTipPachet.setText(textScanat, false);
                    findViewById(android.R.id.content).performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                    android.widget.Toast.makeText(this, "Tip pachet selectat: " + textScanat, Toast.LENGTH_SHORT).show();
                    return;
                }

                String formatZecimal = data.trim().replace(",", ".");

                try{
                    Double.parseDouble(formatZecimal);
                }catch (NumberFormatException ex){
                    if(BuildConfig.DEBUG) Log.e(TAG, "A fost scanat un text invalid");
                    android.widget.Toast.makeText(this, "Text invalid", Toast.LENGTH_SHORT).show();
                    return;
                }

                String valoareTeoretica = etGreutateTeoretica.getText().toString().trim();
                String valoareEfectiva = etGreutateEfectiva.getText().toString().trim();

                if(valoareTeoretica.isEmpty()){
                    etGreutateTeoretica.setText(formatZecimal);
                    findViewById(android.R.id.content).performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                    android.widget.Toast.makeText(this, "Greutate Teoretica: " + formatZecimal, Toast.LENGTH_SHORT).show();
                }
                else if(valoareEfectiva.isEmpty()){
                    etGreutateEfectiva.setText(formatZecimal);
                    findViewById(android.R.id.content).performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                    android.widget.Toast.makeText(this, "Greutate Efectiva: " + formatZecimal, Toast.LENGTH_SHORT).show();
                }else{
                    android.widget.Toast.makeText(this, "Greutatile au fost deja completate", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent) {
        if (BuildConfig.DEBUG) Log.e(TAG, "Barcode read failed!");
    }
    private void submitPachet() {
        CreatePachetRequestModel request = buildRequestFromForm();
        if (request == null) return;

        lastRequest = request;

        String token = TokenStorage.getToken(getApplicationContext());
        if (token == null) {
            Toast.makeText(this, "Sesiune expirata, log in din nou", Toast.LENGTH_LONG).show();
            return;
        }

        pachetViewModel.createPachet(request, token);
    }

    private CreatePachetRequestModel buildRequestFromForm() {
        CreatePachetRequestModel request = new CreatePachetRequestModel();

        String tipPachetText = dropdownTipPachet.getText().toString().trim();
        Integer idTipPachet = TIP_PACHET_IDS.get(tipPachetText);
        if (idTipPachet == null) {
            Toast.makeText(this, "Alege un tip de pachet valid", Toast.LENGTH_SHORT).show();
            return null;
        }
        request.idTipPachet = idTipPachet;

        request.descriere = etDescriere.getText().toString().trim();

        try {
            request.greutateTeoretica = Double.parseDouble(
                    etGreutateTeoretica.getText().toString().trim().replace(",", "."));
            request.greutateEfectiva = Double.parseDouble(
                    etGreutateEfectiva.getText().toString().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Greutăți invalide", Toast.LENGTH_SHORT).show();
            return null;
        }

        boolean expeditorNou = rgTipExpeditor.getCheckedRadioButtonId() == R.id.rbExpNou;
        request.tipExpeditor = expeditorNou ? "nou" : "existent";
        request.emailExpeditor = etEmailExpeditor.getText().toString().trim();
        request.adresaExpeditor = etAdresaExpeditor.getText().toString().trim();
        if (expeditorNou) {
            request.numeExpeditor = etNumeExpeditor.getText().toString().trim();
            request.prenumeExpeditor = etPrenumeExpeditor.getText().toString().trim();
        }

        boolean destinatarNou = rgTipDestinatar.getCheckedRadioButtonId() == R.id.rbDestNou;
        request.tipDestinatar = destinatarNou ? "nou" : "existent";
        request.emailDestinatar = etEmailDestinatar.getText().toString().trim();
        request.adresaDestinatar = etAdresaDestinatar.getText().toString().trim();
        if (destinatarNou) {
            request.numeDestinatar = etNumeDestinatar.getText().toString().trim();
            request.prenumeDestinatar = etPrenumeDestinatar.getText().toString().trim();
        }

        String statusText = dropdownStatus.getText().toString().trim();
        Integer idStatus = STATUS_PACHET_IDS.get(statusText);
        if (idStatus == null) {
            Toast.makeText(this, "Alege un status valid", Toast.LENGTH_SHORT).show();
            return null;
        }
        request.idStatusPachet = idStatus;

        return request;
    }

    private void handlePachetResult(PachetResult result) {
        if (result == null) return;

        if (result.getSuccess() != null) {
            Toast.makeText(this, "Pachet creat! AWB: " + result.getSuccess().awb, Toast.LENGTH_LONG).show();
            finish();

        } else if (result.getNecesitaConfirmare()) {
            var fieldErrors = result.getFieldErrors();
            String cheie = (fieldErrors != null && !fieldErrors.isEmpty())
                    ? fieldErrors.keySet().iterator().next()
                    : null;
            String mesaj = (fieldErrors != null && cheie != null) ? fieldErrors.get(cheie) : "Confirmare necesara";

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Confirmare")
                    .setMessage(mesaj + "\nContinui cu suprascrierea?")
                    .setPositiveButton("Da", (dialog, which) -> {
                        if (lastRequest == null) return;

                        if ("emailExpeditor".equals(cheie)) {
                            lastRequest.confirmaSuprascriereExpeditor = true;
                        } else if ("emailDestinatar".equals(cheie)) {
                            lastRequest.confirmaSuprascriereDestinatar = true;
                        }

                        String token = TokenStorage.getToken(getApplicationContext());
                        pachetViewModel.createPachet(lastRequest, token);
                    })
                    .setNegativeButton("Anulează", null)
                    .show();

        } else if (result.getFieldErrors() != null && !result.getFieldErrors().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (var entry : result.getFieldErrors().entrySet()) {
                sb.append(entry.getValue()).append("\n");
            }
            Toast.makeText(this, sb.toString().trim(), Toast.LENGTH_LONG).show();

        } else if (result.getErrorMessage() != null) {
            Toast.makeText(this, result.getErrorMessage(), Toast.LENGTH_LONG).show();
        }
    }
}