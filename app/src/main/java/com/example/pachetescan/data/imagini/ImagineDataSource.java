package com.example.pachetescan.data.imagini;

import android.content.Context;
import android.util.Log;

import com.example.pachetescan.BuildConfig;
import com.example.pachetescan.config.ServerConfig;
import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.imagini.model.ImagineOperationResultModel;
import com.example.pachetescan.data.imagini.model.ImagineRequestModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ImagineDataSource {
    private final Context context;
    public final String TAG = "ImagineDataSource";

    public ImagineDataSource(Context context) {
        this.context = context;
    }

    public Result<ImagineOperationResultModel> uploadImagine(
            ImagineRequestModel requestModel, int idEntitate, String tipEndpoint, String token) {

        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        HttpURLConnection conn = null;

        File sourceFile = requestModel.fisier;

        try {
            String baseUrl = ServerConfig.getBaseUrl(context);
            URL url = new URL(baseUrl + "/api/" + tipEndpoint + "/" + idEntitate + "/imagini");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {

                // camp text: Awb
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"Awb\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes((requestModel.awb != null ? requestModel.awb : "") + lineEnd);

                // camp fisier: Fisier
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"Fisier\"; filename=\""
                        + sourceFile.getName() + "\"" + lineEnd);
                dos.writeBytes("Content-Type: image/png" + lineEnd);
                dos.writeBytes(lineEnd);

                try (FileInputStream fileInputStream = new FileInputStream(sourceFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytesRead);
                    }
                }

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            }

            int status = conn.getResponseCode();

            InputStream is = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            if (is == null) {
                return new Result.Error(new IOException("Raspuns gol de la server (status " + status + ")"));
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            JSONObject json = new JSONObject(response.toString());
            ImagineOperationResultModel result = new ImagineOperationResultModel();

            if (status == 201) {
                result.success = true;
                result.idImagine = json.optInt("idImagine");
                result.numeFisier = json.optString("numeFisier");
                return new Result.Success<>(result);
            } else if (status == 400) {
                result.success = false;
                JSONObject errors = json.optJSONObject("errors");
                if (errors != null) {
                    var keys = errors.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        var arr = errors.optJSONArray(key);
                        if (arr != null && arr.length() > 0) {
                            result.errors.put(key, arr.optString(0));
                        }
                    }
                }
                return new Result.Success<>(result);
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
            if (BuildConfig.DEBUG) Log.e("ImagineDataSource", "IOException la upload", e);
            return new Result.Error(new IOException("Eroare de retea - verifica conexiunea", e));
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Eroare la incarcarea imaginii", e);
            return new Result.Error(new IOException("Eroare la incarcarea imaginii", e));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}