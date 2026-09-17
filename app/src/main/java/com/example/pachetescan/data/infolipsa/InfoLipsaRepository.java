package com.example.pachetescan.data.infolipsa;

import android.os.Handler;
import android.os.Looper;

import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.infolipsa.model.CreateInfoLipsaRequestModel;
import com.example.pachetescan.data.infolipsa.model.InfoLipsaOperationResultModel;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InfoLipsaRepository {
    private static volatile InfoLipsaRepository instance;
    private final InfoLipsaDataSource dataSource;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private InfoLipsaRepository(InfoLipsaDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static InfoLipsaRepository getInstance(InfoLipsaDataSource dataSource) {
        if (instance == null) {
            synchronized (InfoLipsaRepository.class) {
                if (instance == null) {
                    instance = new InfoLipsaRepository(dataSource);
                }
            }
        }
        return instance;
    }

    public interface InfoLipsaRepositoryCallback {
        void onResult(Result<InfoLipsaOperationResultModel> result);
    }

    public void createInfoLipsa(CreateInfoLipsaRequestModel request, String token, InfoLipsaRepositoryCallback callback){
        executor.execute(()->{
            Result<InfoLipsaOperationResultModel> result;
            try{
                result = dataSource.createInfoLipsa(request, token);
            }catch(Exception e){
                result = new Result.Error(new IOException("Eroare", e));
            }
            Result<InfoLipsaOperationResultModel> finalResult = result;
            mainHandler.post(() -> callback.onResult(finalResult));
        });
    }

}
