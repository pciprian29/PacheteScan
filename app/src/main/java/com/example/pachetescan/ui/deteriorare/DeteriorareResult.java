package com.example.pachetescan.ui.deteriorare;

import androidx.annotation.Nullable;

import com.example.pachetescan.data.deteriorare.model.DeteriorareOperationResultModel;

import java.util.Map;

public class DeteriorareResult {
    @Nullable private DeteriorareOperationResultModel success;
    @Nullable private String errorMessage;
    @Nullable
    private Map<String, String> fieldErrors;

    public static DeteriorareResult success(DeteriorareOperationResultModel data) {
        DeteriorareResult r = new DeteriorareResult();
        r.success = data;
        return r;
    }

    public static DeteriorareResult validationError(Map<String, String> fieldErrors) {
        DeteriorareResult r = new DeteriorareResult();
        r.fieldErrors = fieldErrors;
        return r;
    }

    public static DeteriorareResult networkError(String errorMessage) {
        DeteriorareResult r = new DeteriorareResult();
        r.errorMessage = errorMessage;
        return r;
    }

    @Nullable
    public DeteriorareOperationResultModel getSuccess() {
        return success;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @Nullable
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
