package com.example.pachetescan.data.detalii_pachet;

import android.content.Context;

import com.example.pachetescan.config.ServerConfig;
import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.detalii_pachet.model.DeteriorareDetaliiModel;
import com.example.pachetescan.data.detalii_pachet.model.ImagineDetaliiModel;
import com.example.pachetescan.data.detalii_pachet.model.InfoLipsaDetaliiModel;
import com.example.pachetescan.data.detalii_pachet.model.PachetDetaliiCompleteResponse;
import com.example.pachetescan.data.detalii_pachet.model.PachetDetaliiModel;
import com.google.android.gms.tasks.CancellationToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PachetDetaliiDataSource {
    private final Context context;
    public PachetDetaliiDataSource(Context context) {
        this.context = context;
    }
    public Result<PachetDetaliiCompleteResponse> obtineDetalii(String awb, String token){
        HttpURLConnection conn = null;

        try{
            String baseUrl = ServerConfig.getBaseUrl(context);
            URL url = new URL(baseUrl + "/api/android/pachet/" + awb);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status <300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            if(is == null){
                return new Result.Error(new IOException("Fara raspuns de la server"));
            }

            StringBuilder response = new StringBuilder();
            try(BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))){
                String line;
                while((line = br.readLine()) != null){
                    response.append(line);
                }
            }

            if (status == 200) {
                return new Result.Success<>(parseazaRaspuns(response.toString()));
            } else if (status == 404) {
                return new Result.Error(new IOException("AWB inexistent"));
            } else if (status == 401 || status == 403) {
                return new Result.Error(new IOException("Unauthorized"));
            } else {
                return new Result.Error(new IOException("Unexpected error, status " + status));
            }

        } catch (SocketTimeoutException e) {
            return new Result.Error(new IOException("Server timeout - incearca din nou", e));
        } catch (JSONException e) {
            return new Result.Error(new IOException("Format de raspuns invalid de la server", e));
        } catch (IOException e) {
            return new Result.Error(new IOException("Eroare de retea - verifica conexiunea", e));
        } catch (Exception e) {
            return new Result.Error(new IOException("Eroare la obtinerea detaliilor", e));
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private PachetDetaliiCompleteResponse parseazaRaspuns(String raw) throws JSONException {
        JSONObject json = new JSONObject(raw);
        PachetDetaliiCompleteResponse model = new PachetDetaliiCompleteResponse();

        JSONObject p = json.getJSONObject("pachet");
        PachetDetaliiModel pachet = new PachetDetaliiModel();
        pachet.idPachet = p.optInt("idPachet");
        pachet.awb = p.optString("awb");
        pachet.descriere = p.optString("descriere");
        pachet.adresaExpeditor = p.optString("adresaExpeditor");
        pachet.adresaDestinatar = p.optString("adresaDestinatar");
        pachet.greutateTeoretica = p.optDouble("greutateTeoretica");
        pachet.greutateEfectiva = p.optDouble("greutateEfectiva");
        pachet.emailExpeditor = p.optString("emailExpeditor");
        pachet.emailDestinatar = p.optString("emailDestinatar");
        pachet.idTipPachet = p.optInt("idTipPachet");
        pachet.idStatusPachet = p.optInt("idStatusPachet");
        pachet.numarDeteriorari = p.optInt("numarDeteriorari");
        pachet.numarInfoLipsa = p.optInt("numarInfoLipsa");
        model.pachet = pachet;

        model.infoLipsa = new ArrayList<>();
        JSONArray infoLipsaArr = json.getJSONArray("infoLipsa");
        for (int i = 0; i < infoLipsaArr.length(); i++) {
            JSONObject item = infoLipsaArr.getJSONObject(i);
            InfoLipsaDetaliiModel il = new InfoLipsaDetaliiModel();
            il.idInfoLipsa = item.optInt("idInfoLipsa");
            il.campAfectat = item.optString("campAfectat");
            il.descriere = item.optString("descriere");
            il.idTipLipsa = item.optInt("idTipLipsa");
            il.imagini = parseazaImagini(item.getJSONArray("imagini"));
            model.infoLipsa.add(il);
        }

        model.deteriorari = new ArrayList<>();
        JSONArray deteriorariArr = json.getJSONArray("deteriorari");
        for (int i = 0; i < deteriorariArr.length(); i++) {
            JSONObject item = deteriorariArr.getJSONObject(i);
            DeteriorareDetaliiModel d = new DeteriorareDetaliiModel();
            d.idDeteriorare = item.optInt("idDeteriorare");
            d.locatieDeteriorare = item.optString("locatieDeteriorare");
            d.descriereDeteriorare = item.optString("descriereDeteriorare");
            d.imagini = parseazaImagini(item.getJSONArray("imagini"));
            model.deteriorari.add(d);
        }

        return model;
    }

    private List<ImagineDetaliiModel> parseazaImagini(JSONArray arr) throws JSONException {
        List<ImagineDetaliiModel> lista = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject imgJson = arr.getJSONObject(i);
            ImagineDetaliiModel img = new ImagineDetaliiModel();
            img.idImagine = imgJson.optInt("idImagine");
            img.url = imgJson.optString("url");
            img.dataCreare = imgJson.optString("dataCreare");
            lista.add(img);
        }
        return lista;
    }

}
