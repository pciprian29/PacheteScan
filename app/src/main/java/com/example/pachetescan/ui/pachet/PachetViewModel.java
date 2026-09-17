package com.example.pachetescan.ui.pachet;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.pachet.PachetRepository;
import com.example.pachetescan.data.pachet.model.CreatePachetRequestModel;
import com.example.pachetescan.data.pachet.model.PachetOperationResultModel;

public class    PachetViewModel extends ViewModel {

    private final PachetRepository pachetRepository;
    private final MutableLiveData<PachetResult> pachetResult = new MutableLiveData<>();

    PachetViewModel(PachetRepository pachetRepository) {
        this.pachetRepository = pachetRepository;
    }

    public LiveData<PachetResult> getPachetResult(){
        return pachetResult;
    }

    public void createPachet(CreatePachetRequestModel request, String token){
        pachetRepository.createPachet(request, token, result -> {
            if(result instanceof Result.Success){
                PachetOperationResultModel data = ((Result.Success<PachetOperationResultModel>) result).getData();

                if(data.success){
                    pachetResult.postValue(PachetResult.success(data));
                }else if(data.necesitaConfirmare){
                    pachetResult.postValue(PachetResult.confirmareNecesara(data.errors));
                }else{
                    pachetResult.postValue(PachetResult.validationError(data.errors));
                }
            }else{
                String msg = ((Result.Error) result).getError().getMessage();
                pachetResult.postValue(PachetResult.networkError(msg));
            }
        });
    }
}
