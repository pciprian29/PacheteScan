package com.example.pachetescan.data.detalii_pachet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.pachetescan.config.ServerConfig;
import com.example.pachetescan.data.Result;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

public class ImagineDownloader {
    private final Context context;

    public ImagineDownloader(Context context) {
        this.context = context;
    }

    public Result<Bitmap> descarcaImagine(String urlRelativ, String token) {
        HttpURLConnection conn = null;

        try {
            String baseUrl = ServerConfig.getBaseUrl(context);
            URL url = new URL(baseUrl + urlRelativ);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int status = conn.getResponseCode();

            if (status != 200) {
                return new Result.Error(new IOException("Imagine indisponibila, status " + status));
            }

            try (InputStream is = conn.getInputStream()) {
                Bitmap bitmap = BitmapFactory.decodeStream(is);

                if (bitmap == null) {
                    return new Result.Error(new IOException("Nu s-a putut decoda imaginea"));
                }

                return new Result.Success<>(bitmap);
            }

        } catch (SocketTimeoutException e) {
            return new Result.Error(new IOException("Server timeout - incearca din nou", e));
        } catch (IOException e) {
            return new Result.Error(new IOException("Eroare de retea - verifica conexiunea", e));
        } catch (Exception e) {
            return new Result.Error(new IOException("Eroare la descarcarea imaginii", e));
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}