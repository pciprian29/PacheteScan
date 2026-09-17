package com.example.pachetescan.ui.infolipsa;

import android.content.Context;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.pachetescan.data.infolipsa.InfoLipsaDataSource;
import com.example.pachetescan.data.infolipsa.InfoLipsaRepository;

import org.jetbrains.annotations.NotNull;

public class InfoLipsaViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;
    public InfoLipsaViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NotNull
    @Override
    public <T extends ViewModel> T create(@NotNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(InfoLipsaViewModel.class)) {
            return (T) new InfoLipsaViewModel(InfoLipsaRepository.getInstance(new InfoLipsaDataSource(context)));
        }else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
