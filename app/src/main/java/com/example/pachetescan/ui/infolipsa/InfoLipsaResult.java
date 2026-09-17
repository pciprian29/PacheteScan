package com.example.pachetescan.ui.infolipsa;

import androidx.annotation.Nullable;

import com.example.pachetescan.data.infolipsa.model.InfoLipsaOperationResultModel;

import java.util.Map;

public class InfoLipsaResult {

    @Nullable private InfoLipsaOperationResultModel success;
    @Nullable private String errorMessage;
    @Nullable
    private Map<String, String> fieldErrors;

    public static InfoLipsaResult success(InfoLipsaOperationResultModel data) {
        InfoLipsaResult r = new InfoLipsaResult();
        r.success = data;
        return r;
    }

    public static InfoLipsaResult validationError(Map<String, String> fieldErrors) {
        InfoLipsaResult r = new InfoLipsaResult();
        r.fieldErrors = fieldErrors;
        return r;
    }

    public static InfoLipsaResult networkError(String errorMessage) {
        InfoLipsaResult r = new InfoLipsaResult();
        r.errorMessage = errorMessage;
        return r;
    }

    @Nullable
    public InfoLipsaOperationResultModel getSuccess() {
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
