package com.example.pachetescan.ui.pachet;

import androidx.annotation.Nullable;

import com.example.pachetescan.data.pachet.model.PachetOperationResultModel;

import java.util.Map;

public class PachetResult{
    @Nullable private PachetOperationResultModel success;
    @Nullable private String errorMessage;
    @Nullable private Map<String, String> fieldErrors;
    private boolean necesitaConfirmare;

    public static PachetResult success(PachetOperationResultModel data) {
        PachetResult r = new PachetResult();
        r.success = data;
        return r;
    }

    public static PachetResult confirmareNecesara(Map<String, String> fieldErrors){
        PachetResult r = new PachetResult();
        r.fieldErrors = fieldErrors;
        r.necesitaConfirmare = true;
        return r;
    }

    public static PachetResult validationError(Map<String,String> fieldErrors) {
        PachetResult r = new PachetResult();
        r.fieldErrors = fieldErrors;
        return r;
    }

    public static PachetResult networkError(String errorMessage) {
        PachetResult r = new PachetResult();
        r.errorMessage = errorMessage;
        return r;
    }
    @Nullable public PachetOperationResultModel getSuccess() {
        return success;
    }
    @Nullable public String getErrorMessage() {
        return errorMessage;
    }
    @Nullable public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
    public Boolean getNecesitaConfirmare() {
        return necesitaConfirmare;
    }

}
