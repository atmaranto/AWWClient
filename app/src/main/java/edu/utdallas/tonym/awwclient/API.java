package edu.utdallas.tonym.awwclient;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class API {
    public final static String endpoint = "http://roadblocktoawesome.com:31418";
    public final static String checkPath = "check";

    public final static int DEFAULT_OCCUPANCY = 0;

    public static void checkTemperature(ResponseHandler handler, float desiredTemp, float latitude, float longitude, int occupancy) {
        String url = checkPath + "?desiredTemp=" + desiredTemp + "&latitude=" + latitude + "&longitude=" + longitude;
        if(occupancy > 0) {
            url = url + "&occupancy=" + occupancy;
        }

        doMethod(handler, url, "GET");
    }

    public static void doMethod(ResponseHandler handler, String url, String method) {
        handler.url = url;
        handler.method = method;

        (new RequestTask()).execute(handler);
    }

    private static String readAllFrom(InputStream is) throws IOException {
        StringBuilder builder = new StringBuilder();
        byte[] buffer = new byte[1024];
        char[] charBuffer = new char[1024];

        int read = is.read(buffer);
        while(read != -1) {
            for(int i = 0; i < read; i++) {
                charBuffer[i] = (char)buffer[i];
            }

            builder.append(charBuffer, 0, read);
            read = is.read(buffer);
        }

        return builder.toString();
    }

    private static void doMethodAsync(ResponseHandler handler) {
        String url = endpoint + "/" + handler.url;
        String method = handler.method;

        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection)((new URL(url)).openConnection());
            connection.setRequestMethod(method);
            connection.connect();

            InputStream is = connection.getInputStream();

            String response = readAllFrom(is);

            try{
                float r = Float.valueOf(response);
                handler.floatResponse = r;
            }
            catch(NumberFormatException e) {
                handler.stringResponse = response;
            }
        }
        catch(Exception e){
            try {
                InputStream es = connection.getErrorStream();
                handler.stringResponse = readAllFrom(es);
            }
            catch(Exception e2) {
                e.printStackTrace();
                handler.stringResponse = e.getMessage();
            }
        }

        try {
            connection.disconnect();
        }
        catch(Exception e) {

        }
    }

    public static void checkTemperature(ResponseHandler handler, float desiedTemp, float latitude, float longitude) {
        checkTemperature(handler, desiedTemp, latitude, longitude, DEFAULT_OCCUPANCY);
    }

    private static class RequestTask extends AsyncTask<ResponseHandler,Void,ResponseHandler> {

        @Override
        protected ResponseHandler doInBackground(ResponseHandler... responseHandlers) {
            for(ResponseHandler handler : responseHandlers) {
                doMethodAsync(handler);
            }

            return responseHandlers[0];
        }

        @Override
        protected void onPostExecute(ResponseHandler responseHandler) {
            super.onPostExecute(responseHandler);

            responseHandler.onComplete();
            if(responseHandler.stringResponse != null) {
                responseHandler.onError();
            }
            else {
                responseHandler.onSuccess();
            }
        }
    }
}
