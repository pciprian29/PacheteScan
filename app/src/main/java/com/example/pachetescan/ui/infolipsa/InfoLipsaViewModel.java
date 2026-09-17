package com.example.pachetescan.ui.infolipsa;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.infolipsa.InfoLipsaRepository;
import com.example.pachetescan.data.infolipsa.model.CreateInfoLipsaRequestModel;
import com.example.pachetescan.data.infolipsa.model.InfoLipsaOperationResultModel;

public class InfoLipsaViewModel extends ViewModel {
    private final InfoLipsaRepository infoLipsaRepository;
    private final MutableLiveData<InfoLipsaResult> infoLipsaResult = new MutableLiveData<>();

    InfoLipsaViewModel(InfoLipsaRepository infoLipsaRepository){
        this.infoLipsaRepository = infoLipsaRepository;
    }

    public LiveData<InfoLipsaResult> getInfoLipsaResult(){
        return infoLipsaResult;
    }

    public void createInfoLipsa(CreateInfoLipsaRequestModel request, String token){
        infoLipsaRepository.createInfoLipsa(request, token, result -> {
            if(result instanceof Result.Success){
                InfoLipsaOperationResultModel data = ((Result.Success<InfoLipsaOperationResultModel>) result).getData();

                if(data.success){
                    infoLipsaResult.postValue(InfoLipsaResult.success(data));
                }else{
                    infoLipsaResult.postValue(InfoLipsaResult.validationError(data.errors));
                }
            }else{
                String msg = ((Result.Error) result).getError().getMessage();
                infoLipsaResult.postValue(InfoLipsaResult.networkError(msg));
            }
        });
    }
}
