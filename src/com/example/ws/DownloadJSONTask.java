package com.example.ws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;

public class DownloadJSONTask extends AsyncTask<URL, Void, String> {
    @Override
    protected String doInBackground(URL... urls) {
        try {
            URL url = urls[0];
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String strJSON = readStream(conn.getInputStream());
            return strJSON;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String readStream(InputStream is) {
        try {
            BufferedReader reader = null;
            reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder strBuilder = new StringBuilder();
            String strLine = null;
            while ((strLine = reader.readLine()) != null) {
                strBuilder.append(strLine);
            }
            return strBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
