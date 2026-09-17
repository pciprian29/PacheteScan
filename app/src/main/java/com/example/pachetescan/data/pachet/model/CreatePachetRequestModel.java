package com.example.pachetescan.data.pachet.model;

public class CreatePachetRequestModel {
    public int idTipPachet;
    public String descriere;
    public double greutateTeoretica;
    public double greutateEfectiva;

    public String tipExpeditor;
    public String emailExpeditor;
    public String adresaExpeditor;
    public String numeExpeditor;
    public String prenumeExpeditor;
    public boolean confirmaSuprascriereExpeditor;

    public String tipDestinatar;
    public String emailDestinatar;
    public String adresaDestinatar;
    public String numeDestinatar;
    public String prenumeDestinatar;
    public boolean confirmaSuprascriereDestinatar;

    public int idStatusPachet;
}
