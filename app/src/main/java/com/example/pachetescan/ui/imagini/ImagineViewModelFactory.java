package com.example.pachetescan.ui.imagini;

import android.content.Context;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.pachetescan.data.imagini.ImagineDataSource;
import com.example.pachetescan.data.imagini.ImagineRepository;
import com.example.pachetescan.ui.imagini.ImagineViewModel;

import org.jetbrains.annotations.NotNull;

public class ImagineViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;

    public ImagineViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NotNull
    @Override
    public <T extends ViewModel> T create(@NotNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(com.example.pachetescan.ui.imagini.ImagineViewModel.class)) {
            return (T) new com.example.pachetescan.ui.imagini.ImagineViewModel(ImagineRepository.getInstance(new ImagineDataSource(context)));
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}