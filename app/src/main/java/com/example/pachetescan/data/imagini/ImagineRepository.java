package com.example.pachetescan.data.imagini;

import android.os.Handler;
import android.os.Looper;

import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.imagini.model.ImagineOperationResultModel;
import com.example.pachetescan.data.imagini.model.ImagineRequestModel;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImagineRepository {
    private static volatile ImagineRepository instance;
    private final ImagineDataSource dataSource;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ImagineRepository(ImagineDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static ImagineRepository getInstance(ImagineDataSource dataSource) {
        if (instance == null) {
            synchronized (ImagineRepository.class) {
                if (instance == null) {
                    instance = new ImagineRepository(dataSource);
                }
            }
        }
        return instance;
    }

    public interface ImagineRepositoryCallback {
        void onResult(Result<ImagineOperationResultModel> result);
    }

    public void uploadImagine(ImagineRequestModel requestModel, int idEntitate, String tipEndpoint,
                              String token, ImagineRepositoryCallback callback) {
        executor.execute(() -> {
            Result<ImagineOperationResultModel> result;
            try {
                result = dataSource.uploadImagine(requestModel, idEntitate, tipEndpoint, token);
            } catch (Exception e) {
                result = new Result.Error(new IOException("Eroare", e));
            }
            Result<ImagineOperationResultModel> finalResult = result;
            mainHandler.post(() -> callback.onResult(finalResult));
        });
    }
}