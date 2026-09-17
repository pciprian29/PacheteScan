package com.example.pachetescan.ui.imagini;

import androidx.annotation.Nullable;

import com.example.pachetescan.data.imagini.model.ImagineOperationResultModel;
import com.example.pachetescan.data.infolipsa.model.InfoLipsaOperationResultModel;

import java.util.Map;

public class ImagineResult {

    @Nullable private ImagineOperationResultModel success;
    @Nullable private String errorMessage;
    @Nullable
    private Map<String, String> fieldErrors;

    public static ImagineResult success(ImagineOperationResultModel data) {
        ImagineResult r = new ImagineResult();
        r.success = data;
        return r;
    }

    public static ImagineResult validationError(Map<String, String> fieldErrors) {
        ImagineResult r = new ImagineResult();
        r.fieldErrors = fieldErrors;
        return r;
    }

    public static ImagineResult networkError(String errorMessage) {
        ImagineResult r = new ImagineResult();
        r.errorMessage = errorMessage;
        return r;
    }

    @Nullable
    public ImagineOperationResultModel getSuccess() {
        return success;
    }

    @Nullable
    public String getErrorMessage(){
        return errorMessage;
    }

    @Nullable
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
