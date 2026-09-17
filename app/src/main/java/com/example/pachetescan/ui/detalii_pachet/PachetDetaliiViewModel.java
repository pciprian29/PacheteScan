package com.example.pachetescan.ui.detalii_pachet;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.detalii_pachet.PachetDetaliiRepository;
import com.example.pachetescan.data.detalii_pachet.model.PachetDetaliiCompleteResponse;

public class PachetDetaliiViewModel extends ViewModel {
    private final PachetDetaliiRepository pachetDetaliiRepository;
    private final MutableLiveData<PachetDetaliiResult> pachetDetaliiResult = new MutableLiveData<>();

    PachetDetaliiViewModel(PachetDetaliiRepository pachetDetaliiRepository){
        this.pachetDetaliiRepository = pachetDetaliiRepository;
    }

    public LiveData<PachetDetaliiResult> getPachetDetaliiResult(){
        return pachetDetaliiResult;
    }

    public void obtineDetalii(String awb, String token) {
        pachetDetaliiRepository.obtineDetalii(awb, token, result -> {
            if (result instanceof Result.Success) {
                PachetDetaliiCompleteResponse data = ((Result.Success<PachetDetaliiCompleteResponse>) result).getData();
                pachetDetaliiResult.postValue(PachetDetaliiResult.success(data));
            } else {
                String msg = ((Result.Error) result).getError().getMessage();
                pachetDetaliiResult.postValue(PachetDetaliiResult.networkError(msg));
            }
        });
    }
}