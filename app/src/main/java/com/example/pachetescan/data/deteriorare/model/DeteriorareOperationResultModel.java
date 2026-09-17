package com.example.pachetescan.data.deteriorare.model;

import java.util.HashMap;
import java.util.Map;

public class DeteriorareOperationResultModel {
    public boolean success;
    public int idDeteriorare;
    public String awb;
    public Map<String, String> errors = new HashMap<>();
}
