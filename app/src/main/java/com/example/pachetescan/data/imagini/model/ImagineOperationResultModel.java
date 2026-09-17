package com.example.pachetescan.data.imagini.model;

import java.util.HashMap;
import java.util.Map;

public class ImagineOperationResultModel {
    public boolean success;
    public int idImagine;
    public String numeFisier;
    public Map<String, String> errors = new HashMap<>();
}
