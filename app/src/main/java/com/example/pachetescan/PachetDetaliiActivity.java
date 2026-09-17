package com.example.pachetescan;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.TokenStorage;
import com.example.pachetescan.data.detalii_pachet.ImagineDownloader;
import com.example.pachetescan.data.detalii_pachet.model.DeteriorareDetaliiModel;
import com.example.pachetescan.data.detalii_pachet.model.ImagineDetaliiModel;
import com.example.pachetescan.data.detalii_pachet.model.InfoLipsaDetaliiModel;
import com.example.pachetescan.data.detalii_pachet.model.PachetDetaliiCompleteResponse;
import com.example.pachetescan.ui.detalii_pachet.PachetDetaliiResult;
import com.example.pachetescan.ui.detalii_pachet.PachetDetaliiViewModel;
import com.example.pachetescan.ui.detalii_pachet.PachetDetaliiViewModelFactory;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PachetDetaliiActivity extends AppCompatActivity {

    public static final String EXTRA_AWB = "awb";

    ProgressBar progressBarPrincipal;
    LinearLayout layoutContinut;
    TextView tvAwb;
    TextView tvDescriere;
    TextView tvExpeditor;
    TextView tvDestinatar;
    TextView tvGreutateTeoretica;
    TextView tvGreutateEfectiva;
    LinearLayout layoutDeteriorari;
    LinearLayout layoutInfoLipsa;
    TextView tvFaraDeteriorari;
    TextView tvFaraInfoLipsa;

    PachetDetaliiViewModel pachetDetaliiViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pachet_detalii);

        String awb = getIntent().getStringExtra(EXTRA_AWB);
        if (awb == null) {
            Toast.makeText(this, "AWB lipsa", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.topToolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBarPrincipal = findViewById(R.id.progressBarPrincipal);
        layoutContinut = findViewById(R.id.layoutContinut);
        tvAwb = findViewById(R.id.tvAwb);
        tvDescriere = findViewById(R.id.tvDescriere);
        tvExpeditor = findViewById(R.id.tvExpeditor);
        tvDestinatar = findViewById(R.id.tvDestinatar);
        tvGreutateTeoretica = findViewById(R.id.tvGreutateTeoretica);
        tvGreutateEfectiva = findViewById(R.id.tvGreutateEfectiva);
        layoutDeteriorari = findViewById(R.id.layoutDeteriorari);
        layoutInfoLipsa = findViewById(R.id.layoutInfoLipsa);
        tvFaraDeteriorari = findViewById(R.id.tvFaraDeteriorari);
        tvFaraInfoLipsa = findViewById(R.id.tvFaraInfoLipsa);

        pachetDetaliiViewModel = new ViewModelProvider(this, new PachetDetaliiViewModelFactory(this))
                .get(PachetDetaliiViewModel.class);

        pachetDetaliiViewModel.getPachetDetaliiResult().observe(this, this::handleRezultat);

        String token = TokenStorage.getToken(getApplicationContext());
        if (token == null) {
            Toast.makeText(this, "Sesiune expirata, log in din nou", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        pachetDetaliiViewModel.obtineDetalii(awb, token);
    }
    private void handleRezultat(PachetDetaliiResult result) {
        progressBarPrincipal.setVisibility(View.GONE);

        if (result.getSuccess() != null) {
            layoutContinut.setVisibility(View.VISIBLE);
            populeazaEcran(result.getSuccess());
        } else {
            Toast.makeText(this, result.getErrorMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void populeazaEcran(PachetDetaliiCompleteResponse data) {
        var p = data.pachet;

        tvAwb.setText(p.awb);
        tvDescriere.setText("Descriere: " + p.descriere);
        tvExpeditor.setText("Expeditor: " + p.emailExpeditor + " - " + p.adresaExpeditor);
        tvDestinatar.setText("Destinatar: " + p.emailDestinatar + " - " + p.adresaDestinatar);
        tvGreutateTeoretica.setText("Greutate teoretica: " + p.greutateTeoretica + " kg");
        tvGreutateEfectiva.setText("Greutate efectiva: " + p.greutateEfectiva + " kg");

        populeazaDeteriorari(data.deteriorari);
        populeazaInfoLipsa(data.infoLipsa);
    }

    private void populeazaDeteriorari(List<DeteriorareDetaliiModel> deteriorari) {
        layoutDeteriorari.removeAllViews();

        if (deteriorari == null || deteriorari.isEmpty()) {
            tvFaraDeteriorari.setVisibility(View.VISIBLE);
            return;
        }

        tvFaraDeteriorari.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (DeteriorareDetaliiModel d : deteriorari) {
            View item = inflater.inflate(R.layout.item_detaliu_sesizare, layoutDeteriorari, false);

            TextView titlu = item.findViewById(R.id.tvItemTitlu);
            TextView descriere = item.findViewById(R.id.tvItemDescriere);
            LinearLayout layoutImaginiItem = item.findViewById(R.id.layoutImaginiItem);

            titlu.setText("Locatie: " + d.locatieDeteriorare);
            descriere.setText("Descriere: " + d.descriereDeteriorare);

            populeazaButoaneImagini(layoutImaginiItem, d.imagini);

            layoutDeteriorari.addView(item);
        }
    }

    private void populeazaInfoLipsa(List<InfoLipsaDetaliiModel> infoLipsa) {
        layoutInfoLipsa.removeAllViews();

        if (infoLipsa == null || infoLipsa.isEmpty()) {
            tvFaraInfoLipsa.setVisibility(View.VISIBLE);
            return;
        }

        tvFaraInfoLipsa.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (InfoLipsaDetaliiModel il : infoLipsa) {
            View item = inflater.inflate(R.layout.item_detaliu_sesizare, layoutInfoLipsa, false);

            TextView titlu = item.findViewById(R.id.tvItemTitlu);
            TextView descriere = item.findViewById(R.id.tvItemDescriere);
            LinearLayout layoutImaginiItem = item.findViewById(R.id.layoutImaginiItem);

            titlu.setText("Camp afectat: " + il.campAfectat);
            descriere.setText(il.descriere);

            populeazaButoaneImagini(layoutImaginiItem, il.imagini);

            layoutInfoLipsa.addView(item);
        }
    }

    private void populeazaButoaneImagini(LinearLayout container, List<ImagineDetaliiModel> imagini) {
        container.removeAllViews();

        if (imagini == null || imagini.isEmpty()) {
            TextView tvFaraImagini = new TextView(this);
            tvFaraImagini.setText("Fara imagini");
            tvFaraImagini.setTextColor(getResources().getColor(android.R.color.darker_gray));
            container.addView(tvFaraImagini);
            return;
        }

        for (ImagineDetaliiModel img : imagini) {
            ImageView thumbnail = new ImageView(this);

            int dimensiune = dpToPx(60);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dimensiune, dimensiune);
            params.setMarginEnd(dpToPx(8));
            thumbnail.setLayoutParams(params);

            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
            thumbnail.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

            thumbnail.setOnClickListener(v -> arataImagineInDialog(img.url));

            container.addView(thumbnail);

            incarcaThumbnail(thumbnail, img.url + "/thumbnail");
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void incarcaThumbnail(ImageView imageView, String thumbnailUrl) {
        String token = TokenStorage.getToken(getApplicationContext());
        if (token == null) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        ImagineDownloader downloader = new ImagineDownloader(this);

        executor.execute(() -> {
            Result<Bitmap> rezultat = downloader.descarcaImagine(thumbnailUrl, token);

            mainHandler.post(() -> {
                if (rezultat instanceof Result.Success) {
                    Bitmap bitmap = ((Result.Success<Bitmap>) rezultat).getData();
                    imageView.setImageBitmap(bitmap);
                }
            });
            executor.shutdown();
        });
    }

    private void arataImagineInDialog(String urlRelativ) {
        String token = TokenStorage.getToken(getApplicationContext());
        if (token == null) {
            Toast.makeText(this, "Sesiune expirata", Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_imagine, null);
        ImageView imageView = dialogView.findViewById(R.id.imgDialogImagine);
        View progressBar = dialogView.findViewById(R.id.progressDialogImagine);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Inchide", null)
                .create();
        dialog.show();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        ImagineDownloader downloader = new ImagineDownloader(this);

        executor.execute(() -> {
            Result<Bitmap> rezultat = downloader.descarcaImagine(urlRelativ, token);

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);

                if (rezultat instanceof Result.Success) {
                    Bitmap bitmap = ((Result.Success<Bitmap>) rezultat).getData();
                    imageView.setImageBitmap(bitmap);
                    imageView.setVisibility(View.VISIBLE);
                } else {
                    String msg = ((Result.Error) rezultat).getError().getMessage();
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                }
            });

            executor.shutdown();
        });
    }
}