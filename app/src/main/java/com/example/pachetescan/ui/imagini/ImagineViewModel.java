package com.example.pachetescan.ui.imagini;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.imagini.ImagineRepository;
import com.example.pachetescan.data.imagini.model.ImagineOperationResultModel;
import com.example.pachetescan.data.imagini.model.ImagineRequestModel;

public class ImagineViewModel extends ViewModel {
    private final ImagineRepository imagineRepository;
    private final MutableLiveData<ImagineResult> imagineResult = new MutableLiveData<>();

    ImagineViewModel(ImagineRepository imagineRepository) {
        this.imagineRepository = imagineRepository;
    }

    public LiveData<ImagineResult> getImagineResult() {
        return imagineResult;
    }

    // folosita de AdaugaImagini (MODE_UPLOAD_IMEDIAT, fluxul InfoLipsa) - rezultatul ajunge prin LiveData
    public void uploadImagine(ImagineRequestModel requestModel, int idEntitate, String tipEndpoint, String token) {
        imagineRepository.uploadImagine(requestModel, idEntitate, tipEndpoint, token, result -> {
            if (result instanceof Result.Success) {
                ImagineOperationResultModel data = ((Result.Success<ImagineOperationResultModel>) result).getData();

                if (data.success) {
                    imagineResult.postValue(ImagineResult.success(data));
                } else {
                    imagineResult.postValue(ImagineResult.validationError(data.errors));
                }
            } else {
                String msg = ((Result.Error) result).getError().getMessage();
                imagineResult.postValue(ImagineResult.networkError(msg));
            }
        });
    }

    public interface UploadCallback {
        void onResult(ImagineResult result);
    }

    public void uploadImagineSincron(ImagineRequestModel requestModel, int idEntitate, String tipEndpoint,
                                     String token, UploadCallback callback) {
        imagineRepository.uploadImagine(requestModel, idEntitate, tipEndpoint, token, result -> {
            if (result instanceof Result.Success) {
                ImagineOperationResultModel data = ((Result.Success<ImagineOperationResultModel>) result).getData();

                if (data.success) {
                    callback.onResult(ImagineResult.success(data));
                } else {
                    callback.onResult(ImagineResult.validationError(data.errors));
                }
            } else {
                String msg = ((Result.Error) result).getError().getMessage();
                callback.onResult(ImagineResult.networkError(msg));
            }
        });
    }
}