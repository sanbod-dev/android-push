package com.sanbod.push.utils;

import com.sanbod.push.Config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class RestClientUtil {

    private int connectTimeout;
    private int readTimeout;

    /**
     * Default constructor with 15-second timeouts.
     */
    public RestClientUtil() {
        this.connectTimeout = 15000; // 15 seconds
        this.readTimeout = 15000;    // 15 seconds
    }

    /**
     * Constructor with custom timeouts.
     *
     * @param connectTimeout connection timeout in milliseconds
     * @param readTimeout    read timeout in milliseconds
     */
    public RestClientUtil(int connectTimeout, int readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    /**
     * Executes a GET request.
     *
     * @param urlString the URL to request
     * @param headers   optional headers to include in the request
     * @return the response as a String
     * @throws IOException if an I/O error occurs
     */
    public String get(String urlString, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            // Create URL and open connection
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);

            // Add headers if provided
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // Connect and get the response code
            int responseCode = connection.getResponseCode();

            // Choose input stream based on response code
            InputStream inputStream = (responseCode >= HttpURLConnection.HTTP_OK &&
                    responseCode < HttpURLConnection.HTTP_MULT_CHOICE)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            // Read the response
            reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString();

        } finally {
            // Clean up resources
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Executes a POST request.
     *
     * @param urlString the URL to post to
     * @param data      the POST data as a String (e.g., JSON or form data)
     * @param headers   optional headers to include in the request
     * @return the response as a String
     * @throws IOException if an I/O error occurs
     */
    public String post(String urlString, String data, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = null;
        BufferedWriter writer = null;
        BufferedReader reader = null;

        try {
            // Create URL and open connection
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setDoOutput(true); // Needed for POST requests

            // Add headers if provided
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            connection.setRequestProperty("Content-Type","application/json");

            if (data != null) {
                // Write POST data to the output stream
                OutputStream outputStream = connection.getOutputStream();
                writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                writer.write(data);
                writer.flush();
            }

            // Get response code and choose appropriate stream
            int responseCode = connection.getResponseCode();
            InputStream inputStream = (responseCode >= HttpURLConnection.HTTP_OK &&
                    responseCode < HttpURLConnection.HTTP_MULT_CHOICE)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            // Read the response
            reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString();

        } finally {
            // Clean up resources
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    public static RestClientUtil getRestClient(int timeout) {
        return new RestClientUtil(timeout, timeout);
    }

    public static String getAddress(Config config, String method) {
        return config.getBaseUrl() + method;
    }
}
