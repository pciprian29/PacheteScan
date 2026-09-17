package com.example.pachetescan.data.pachet.model;

import java.util.HashMap;
import java.util.Map;

public class PachetOperationResultModel {
    public boolean success;
    public boolean necesitaConfirmare;
    public int idPachet;
    public String awb;
    public Map<String, String> errors = new HashMap<>();
}
