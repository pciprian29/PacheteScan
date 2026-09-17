package com.example.pachetescan.data.deteriorare;

import android.os.Handler;
import android.os.Looper;

import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.deteriorare.model.CreateDeteriorareRequestModel;
import com.example.pachetescan.data.deteriorare.model.DeteriorareOperationResultModel;


import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeteriorareRepository{
    private static volatile DeteriorareRepository instance;
    private final DeteriorareDataSource dataSource;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private DeteriorareRepository(DeteriorareDataSource dataSource) {
        this.dataSource = dataSource;
    }
    public static DeteriorareRepository getInstance(DeteriorareDataSource dataSource){
        if(instance == null){
            instance = new DeteriorareRepository(dataSource);
        }
        return instance;
    }

    public interface DeteriorareRepositoryCallback{
        void onResult(Result<DeteriorareOperationResultModel> result);
    }

    public void createDeteriorare(CreateDeteriorareRequestModel requestModel, String token, DeteriorareRepositoryCallback callback){
        executor.execute(() ->{
            Result<DeteriorareOperationResultModel> result;
            try{
                result = dataSource.createDeteriorare(requestModel, token);
            }catch(Exception e){
                result = new Result.Error(new IOException("Eroare", e));
            }
            Result<DeteriorareOperationResultModel> finalResult = result;
            mainHandler.post(() -> callback.onResult(finalResult));
        });
    }
}
