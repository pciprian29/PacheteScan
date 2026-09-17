package com.example.pachetescan.ui.detalii_pachet;

import androidx.annotation.Nullable;

import com.example.pachetescan.data.detalii_pachet.PachetDetaliiRepository;
import com.example.pachetescan.data.detalii_pachet.model.PachetDetaliiCompleteResponse;

public class PachetDetaliiResult {
    @Nullable
    private PachetDetaliiCompleteResponse success;
    @Nullable
    private String errorMessage;

    public static PachetDetaliiResult success(PachetDetaliiCompleteResponse data) {
        PachetDetaliiResult r = new PachetDetaliiResult();
        r.success = data;
        return r;
    }

    public static PachetDetaliiResult networkError(String errorMessage) {
        PachetDetaliiResult r = new PachetDetaliiResult();
        r.errorMessage = errorMessage;
        return r;
    }

    @Nullable
    public PachetDetaliiCompleteResponse getSuccess() {
        return success;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }
}
