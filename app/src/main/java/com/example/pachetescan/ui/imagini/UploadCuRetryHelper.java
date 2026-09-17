package com.example.pachetescan.ui.imagini;

import android.os.Handler;
import android.util.Log;

import com.example.pachetescan.data.imagini.model.ImagineRequestModel;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class UploadCuRetryHelper {
    private static final int MAX_INCERCARI = 3; // 1 initiala + 2 retry
    private static final long DELAY_INTRE_INCERCARI_MS = 1500;

    public interface RezultatCoada {
        void onProgres(int curent, int total);
        void onFinalizat(List<File> reusite, List<File> esuate);
    }

    public static void proceseazaCoada(
            List<File> poze, String awb, int idEntitate, String tipEndpoint, String token,
            ImagineViewModel viewModel, Handler mainHandler, RezultatCoada callback) {

        List<File> reusite = new ArrayList<>();
        List<File> esuate = new ArrayList<>();

        proceseazaUrmatoarea(new ArrayDeque<>(poze), reusite, esuate, 0, poze.size(),
                awb, idEntitate, tipEndpoint, token, viewModel, mainHandler, callback);
    }

    private static void proceseazaUrmatoarea(
            Deque<File> ramase, List<File> reusite, List<File> esuate,
            int index, int total, String awb, int idEntitate, String tipEndpoint, String token,
            ImagineViewModel viewModel, Handler mainHandler, RezultatCoada callback) {

        if (ramase.isEmpty()) {
            callback.onFinalizat(reusite, esuate);
            return;
        }

        File poza = ramase.poll();
        callback.onProgres(index + 1, total);

        incearcaUpload(poza, awb, idEntitate, tipEndpoint, token, viewModel, 1, mainHandler, succes -> {
            if (succes) {
                reusite.add(poza);
            } else {
                esuate.add(poza);
            }
            proceseazaUrmatoarea(ramase, reusite, esuate, index + 1, total,
                    awb, idEntitate, tipEndpoint, token, viewModel, mainHandler, callback);
        });
    }

    private interface RezultatIncercare {
        void onGata(boolean succes);
    }

    private static void incearcaUpload(
            File poza, String awb, int idEntitate, String tipEndpoint, String token,
            ImagineViewModel viewModel, int incercareCurenta, Handler mainHandler, RezultatIncercare callback) {

        ImagineRequestModel request = new ImagineRequestModel();
        request.fisier = poza;
        request.awb = awb;

        viewModel.uploadImagineSincron(request, idEntitate, tipEndpoint, token, rezultat -> {
            boolean succes = rezultat.getSuccess() != null;

            if (!succes) {
                String motiv = rezultat.getErrorMessage() != null
                        ? rezultat.getErrorMessage()
                        : (rezultat.getFieldErrors() != null ? rezultat.getFieldErrors().toString() : "necunoscut");
                Log.e("UploadCuRetryHelper", "Incercare " + incercareCurenta + " esuata pentru " + poza.getName() + ": " + motiv);
            }

            if (succes || incercareCurenta >= MAX_INCERCARI) {
                callback.onGata(succes);
            } else {
                mainHandler.postDelayed(() ->
                                incearcaUpload(poza, awb, idEntitate, tipEndpoint, token,
                                        viewModel, incercareCurenta + 1, mainHandler, callback),
                        DELAY_INTRE_INCERCARI_MS);
            }
        });
    }
}