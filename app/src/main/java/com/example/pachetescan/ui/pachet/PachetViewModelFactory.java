package com.example.pachetescan.ui.pachet;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.pachetescan.data.pachet.PachetDataSource;
import com.example.pachetescan.data.pachet.PachetRepository;

public class PachetViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;
    public PachetViewModelFactory(Context context) {
        this.context = context;
    }
    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(PachetViewModel.class)) {
            return (T) new PachetViewModel(PachetRepository.getInstance(new PachetDataSource(context)));
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}