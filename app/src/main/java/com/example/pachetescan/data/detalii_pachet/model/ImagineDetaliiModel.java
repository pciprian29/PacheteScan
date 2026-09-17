package com.example.pachetescan.data.detalii_pachet.model;

public class ImagineDetaliiModel {
    public int idImagine;
    public String url;
    public String dataCreare;

    public String getThumbUrl(){
        return url + "/thumbnail";
    }
}
