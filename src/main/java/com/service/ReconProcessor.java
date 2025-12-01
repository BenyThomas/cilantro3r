/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author MELLEJI
 */
public class ReconProcessor {

    public String gatewayRetryRefund(String requestBody, String url) throws Exception {
        String responseBody = "-1";
        String urlParameters = requestBody.trim();
        URL obj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/xml");
        connection.setRequestProperty("Content-Type", "application/xml");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Content-Length", String.valueOf(urlParameters.length()));
        try {
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            writer.write(urlParameters);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
//                    LOGGER.info("Error Code: " + responseCode);
//                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                    System.exit(0);
                case 401:
//                    LOGGER.info("Error Code: " + responseCode);
//                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
//                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 404:
//                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 400:
//                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 405:
//                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 502:
//                    LOGGER.info("Error Code: " + responseCode);
//                    LOGGER.info("502 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
//                    LOGGER.info("Error Code: " + responseCode);
//                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
//                    LOGGER.info("Error Code: " + responseCode);
//                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
//                        LOGGER.info("Connecting to GePG ", e);
                    }
                    break;
            }
            String inputLine;
            StringBuilder response = new StringBuilder();
            if (in != null) {
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
            }
            if (responseCode == 200) {
                responseBody = response.toString();
            } else {
//                LOGGER.warn(response.toString());
            }
//            LOGGER.info("Disconnecting...");
            connection.disconnect();
        } catch (IOException ex) {
//            LOGGER.info("Connecting to GePG ", ex);
        }
        return responseBody;
    }
}
