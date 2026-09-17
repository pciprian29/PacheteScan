package com.example.pachetescan.data.pachet;

import android.content.Context;

import com.example.pachetescan.config.ServerConfig;
import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.pachet.model.CreatePachetRequestModel;
import com.example.pachetescan.data.pachet.model.PachetOperationResultModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PachetDataSource {

    private final Context context;
    public PachetDataSource(Context context) {
        this.context = context;
    }

    public Result<PachetOperationResultModel> createPachet(CreatePachetRequestModel request, String token) {
        HttpURLConnection conn = null;

        try{
            String baseUrl = ServerConfig.getBaseUrl(context);
            URL url = new URL(baseUrl + "/api/android/pachet");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charste-utf8");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            JSONObject body = buildRequestBody(request);

            try(OutputStream os = conn.getOutputStream()){
                byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            StringBuilder response = new StringBuilder();
            try(BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))){
                String line;
                while((line = br.readLine()) != null){
                    response.append(line);
                }
            }

            JSONObject json = new JSONObject(response.toString());
            PachetOperationResultModel result = new PachetOperationResultModel();

            if(status == 201){
                result.success = true;
                result.idPachet = json.optInt("idPachet");
                result.awb = json.getString("awb");
                return new Result.Success<>(result);
            }else if(status == 409){
                result.success = false;
                result.necesitaConfirmare = true;
                putStringErrors(json, result);
                return new Result.Success<>(result);
            }else if(status == 400){
                result.success = false;
                JSONObject errors = json.optJSONObject("errors");
                if(errors != null){
                    var keys = errors.keys();
                    while(keys.hasNext()){
                        String key = keys.next();
                        var arr = errors.optJSONArray(key);
                        if(arr != null && arr.length() > 0){
                            result.errors.put(key, arr.optString(0));
                        }
                    }
                }
                return new Result.Success<>(result);
            }else if(status == 401 || status == 403){
                return new Result.Error(new IOException("Unauthorized"));
            }else{
                return new Result.Error(new IOException("Unexpected error"));
            }
        }catch(SocketTimeoutException e){
            return new Result.Error(new IOException("Server timeout — please try again", e));
        }catch(JSONException e){
            return new Result.Error(new IOException("Invalid response format from server", e));
        }catch (IOException e){
            return new Result.Error(new IOException("Network error — check your connection", e));
        }catch(Exception e){
            return new Result.Error(new IOException("Eroare la crearea pachetului", e));
        }finally {
            if (conn != null){
                conn.disconnect();
            }
        }
    }

    public void putStringErrors(JSONObject json, PachetOperationResultModel result) throws JSONException {
        var keys = json.keys();
        while(keys.hasNext()){
            String key = keys.next();
            result.errors.put(key, json.getString(key));
        }
    }

    public JSONObject buildRequestBody(CreatePachetRequestModel request) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("idTipPachet", request.idTipPachet);
        body.put("descriere", request.descriere);
        body.put("greutateTeoretica", request.greutateTeoretica);
        body.put("greutateEfectiva", request.greutateEfectiva);

        body.put("tipExpeditor", request.tipExpeditor);
        body.put("emailExpeditor", request.emailExpeditor);
        body.put("adresaExpeditor", request.adresaExpeditor);
        body.put("numeExpeditor", request.numeExpeditor);
        body.put("prenumeExpeditor", request.prenumeExpeditor);
        body.put("confirmaSuprascriereExpeditor", request.confirmaSuprascriereExpeditor);

        body.put("tipDestinatar", request.tipDestinatar);
        body.put("emailDestinatar", request.emailDestinatar);
        body.put("adresaDestinatar", request.adresaDestinatar);
        body.put("numeDestinatar", request.numeDestinatar);
        body.put("prenumeDestinatar", request.prenumeDestinatar);
        body.put("confirmaSuprascriereDestinatar", request.confirmaSuprascriereDestinatar);

        body.put("idStatusPachet", request.idStatusPachet);
        return body;
    }
}
