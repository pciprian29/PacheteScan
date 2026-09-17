package com.example.pachetescan.data.detalii_pachet;

import android.os.Handler;
import android.os.Looper;

import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.detalii_pachet.model.PachetDetaliiCompleteResponse;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PachetDetaliiRepository {
    private static volatile PachetDetaliiRepository instance;
    private final PachetDetaliiDataSource dataSource;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private PachetDetaliiRepository(PachetDetaliiDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static PachetDetaliiRepository getInstance(PachetDetaliiDataSource dataSource) {
        if (instance == null) {
            synchronized (PachetDetaliiRepository.class) {
                if (instance == null) {
                    instance = new PachetDetaliiRepository(dataSource);
                }
            }
        }
        return instance;
    }

    public interface Callback{
        void onResult(Result<PachetDetaliiCompleteResponse> result);
    }

    public void obtineDetalii(String awb, String token, Callback callback) {
        executor.execute(() -> {
        Result<PachetDetaliiCompleteResponse> result;
        try{
            result = dataSource.obtineDetalii(awb, token);
        }catch(Exception e){
            result = new Result.Error(new IOException("Eroare", e));
        }
        Result<PachetDetaliiCompleteResponse> finalResult = result;
        mainHandler.post(() -> callback.onResult(finalResult));
        });
    }
}
