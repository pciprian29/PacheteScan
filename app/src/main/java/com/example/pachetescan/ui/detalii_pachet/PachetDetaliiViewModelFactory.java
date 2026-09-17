package com.example.pachetescan.ui.detalii_pachet;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.pachetescan.data.detalii_pachet.PachetDetaliiDataSource;
import com.example.pachetescan.data.detalii_pachet.PachetDetaliiRepository;

public class PachetDetaliiViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;
    public PachetDetaliiViewModelFactory(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(PachetDetaliiViewModel.class)) {
            return (T) new PachetDetaliiViewModel(PachetDetaliiRepository.getInstance(new PachetDetaliiDataSource(context)));
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
