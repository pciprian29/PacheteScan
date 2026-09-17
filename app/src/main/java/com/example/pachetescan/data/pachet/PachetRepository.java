package com.example.pachetescan.data.pachet;

import android.os.Looper;

import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.pachet.model.CreatePachetRequestModel;
import com.example.pachetescan.data.pachet.model.PachetOperationResultModel;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;

public class PachetRepository {
    public static volatile PachetRepository instance;
    public final PachetDataSource dataSource;
    public final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private PachetRepository(PachetDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static PachetRepository getInstance(PachetDataSource dataSource){
        if(instance == null){
            instance = new PachetRepository(dataSource);
        }
        return instance;
    }

    public interface PachetRepositoryCallback{
        void onResult(Result<PachetOperationResultModel> result);
    }

    public void createPachet(CreatePachetRequestModel requestModel, String token, PachetRepositoryCallback callback){
        executor.execute(() ->{
            Result<PachetOperationResultModel> result;
            try{
                result = dataSource.createPachet(requestModel, token);
            }catch(Exception e){
                result = new Result.Error(new IOException("Eroare", e));
            }
            Result<PachetOperationResultModel> finalResult = result;
            mainHandler.post(() -> callback.onResult(finalResult));
        });
    }
}
