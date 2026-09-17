package com.example.pachetescan.ui.deteriorare;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.deteriorare.DeteriorareRepository;
import com.example.pachetescan.data.deteriorare.model.CreateDeteriorareRequestModel;
import com.example.pachetescan.data.deteriorare.model.DeteriorareOperationResultModel;

public class DeteriorareViewModel extends ViewModel {
    private final DeteriorareRepository deteriorareRepository;
    private final MutableLiveData<DeteriorareResult> deteriorareResult = new MutableLiveData<>();

    DeteriorareViewModel(DeteriorareRepository deteriorareRepository){
        this.deteriorareRepository = deteriorareRepository;
    }

    public LiveData<DeteriorareResult> getDeteriorareResult(){
        return deteriorareResult;
    }

    public void createDeteriorare(CreateDeteriorareRequestModel request, String token){
        deteriorareRepository.createDeteriorare(request, token, result -> {
            if(result instanceof Result.Success){
                DeteriorareOperationResultModel data = ((Result.Success<DeteriorareOperationResultModel>) result).getData();

                if(data.success){
                    deteriorareResult.postValue(DeteriorareResult.success(data));
                }else{
                    deteriorareResult.postValue(DeteriorareResult.validationError(data.errors));
                }

            }else{
                String msg = ((Result.Error) result).getError().getMessage();
                deteriorareResult.postValue(DeteriorareResult.networkError(msg));
            }
        });
    }
}
