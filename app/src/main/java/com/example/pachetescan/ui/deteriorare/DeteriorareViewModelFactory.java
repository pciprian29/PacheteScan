package com.example.pachetescan.ui.deteriorare;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.pachetescan.data.deteriorare.DeteriorareDataSource;
import com.example.pachetescan.data.deteriorare.DeteriorareRepository;

public class DeteriorareViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;
    public DeteriorareViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(DeteriorareViewModel.class)) {
            return (T) new DeteriorareViewModel(DeteriorareRepository.getInstance(new DeteriorareDataSource(context)));
        }else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
