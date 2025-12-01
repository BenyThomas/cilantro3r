/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;


import com.config.SYSENV;
import com.entities.CreateAccountRequest;
import com.entities.CreateCustomerRequest;
import com.entities.EMCReq;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.helper.MaiString;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * @author samichael
 */
@Service
public class HttpClientService {

    @Autowired
    ObjectMapper jacksonMapper;
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientService.class);
    @Autowired
    private RestTemplate restTemplate;

    //    public static String sendXMLRequest(String requestXML, String url) {
//        String responseBody = "-1";
//        String urlParameters = requestXML.trim();
//        LOGGER.info("PAYLOAD: " + urlParameters);
//        try {
//            LOGGER.info("Opening Connection: " + url);
//            URL obj = new URL(url);
//            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
//            connection.setRequestProperty("Content-Type", "text/xml");
//            connection.setRequestMethod("POST");
//            connection.setDoOutput(true);
//            DataOutputStream ds = new DataOutputStream(connection.getOutputStream());
//            ds.writeBytes(urlParameters);
//            ds.flush();
//            int responseCode = connection.getResponseCode();
//            BufferedReader in = null;
//            switch (responseCode) {
//                case 200:
//                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                    break;
//                case 301:
//                    LOGGER.info("Error Code: " + responseCode);
//                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
//                case 401:
//                    LOGGER.info("Error Code: " + responseCode);
//                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
//                    break;
//                case 403:
//                    LOGGER.info("Error Code: " + responseCode);
//                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                    break;
//                case 404:
//                    LOGGER.info("Error Code: " + responseCode);
//                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                    break;
//                case 400:
//                    LOGGER.info("Error Code: " + responseCode);
//                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                    break;
//                case 405:
//                    LOGGER.info("Error Code: " + responseCode);
//                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                    break;
//                case 503:
//                    LOGGER.info("Error Code: " + responseCode);
//                    LOGGER.info("503 Service Temporarily Unavailable");
//                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                    break;
//                case 202:
//                    LOGGER.info("Error Code: " + responseCode);
//                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
//                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                    break;
//                default:
//                    try {
//                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                    } catch (IOException e) {
//                        LOGGER.info("Error while sending request [" + e.getMessage() + "]");
//                    }
//                    break;
//            }
//            String inputLine;
//            StringBuilder response = new StringBuilder();
//            if (in != null) {
//                while ((inputLine = in.readLine()) != null) {
//                    response.append(inputLine);
//                }
//                in.close();
//            }
//            responseBody = response.toString();
//            // LOGGER.info("Response: " + prettyFormat(responseBody, 2));
//            LOGGER.info("Closing Connection: " + url);
//            LOGGER.info("Response : " + responseBody);
//            connection.disconnect();
//        } catch (IOException ex) {
//            LOGGER.info("{} {}", url, ex.getMessage());
//        }
//        return responseBody;
//    }

    public static String sendXMLRequest(String requestXML, String url) {
        try {
// Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

// Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

// Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

// Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        String responseBody = "-1";
        String urlParameters = requestXML.trim();
        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestProperty("Content-Type", "text/xml");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(60000);
            DataOutputStream ds = new DataOutputStream(connection.getOutputStream());
            ds.writeBytes(urlParameters);
            ds.flush();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                case 405:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 404:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 400:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 502:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("502 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Error while sending request [" + e.getMessage() + "]");
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
            responseBody = response.toString();
            LOGGER.info("URL[{}][{}] \nRequest: {} \nResponse: {}", url, responseCode, requestXML, responseBody);
            connection.disconnect();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(),ex);
            LOGGER.info("URL[{}][{}] \nRequest: {} \nResponse: {} \n Exception:{}", url, 500, requestXML, responseBody, ex.getMessage());

        }
        return responseBody;
    }

    public static String sendKprinterRequest(String requestXML, String url) {
        try {
        // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

        // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLSv1.3");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

        // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        String responseBody = "-1";
        String urlParameters = requestXML.trim();
        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestProperty("Content-Type", "text/xml");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(60000);
            DataOutputStream ds = new DataOutputStream(connection.getOutputStream());
            ds.writeBytes(urlParameters);
            ds.flush();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                case 405:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 404:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 400:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 502:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("502 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Error while sending request [" + e.getMessage() + "]");
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
            responseBody = response.toString();
            LOGGER.info("URL[{}][{}] \nRequest: {} \nResponse: {}", url, responseCode, requestXML, responseBody);
            connection.disconnect();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(),ex);
            LOGGER.info("URL[{}][{}] \nRequest: {} \nResponse: {} \n Exception:{}", url, 500, requestXML, responseBody, ex.getMessage());

        }
        return responseBody;
    }

    public static String sendTipsXMLRequest(String requestXML, String url) {
        List<Map<String, String>> parameters = MaiString.buildListFromQuery(requestXML);
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

// Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

// Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

// Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        String responseBody = "-1";
        String urlParameters = requestXML.trim();
        try {
            URL obj = new URL(url);
            LOGGER.info("Opening connection from TIPS to URL ... {}", url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(60000000);
//            LOGGER.info("Your trying to connect to tips remote connection with details: ... {}",connection);
            parameters.forEach((parameter) -> {
                connection.setRequestProperty(parameter.get("item"), parameter.get("value"));
//                LOGGER.info("{}:{}", parameter.get("item"), parameter.get("value"));
            });
            DataOutputStream ds = new DataOutputStream(connection.getOutputStream());
            ds.writeBytes(urlParameters);
            ds.flush();

            int responseCode = connection.getResponseCode();

            LOGGER.info("REMOTE URL CONNECTION responseCode... {}", responseCode);
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 404:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                case 400:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                case 405:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Error while sending request [" + e.getMessage() + "]");
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
            responseBody = response.toString();
            LOGGER.info("URL[{}][{}] \nRequest: {} \nResponse: {}", url, responseCode, requestXML, responseBody);
            in.close();
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.info("EXCEPTION IN GETTING TIPS TRANSACTION....{}", ex.getMessage());
        }
        return responseBody;

    }

    public static String sendLoanRepaymentXMLRequest(String requestXML, String url) {
        List<Map<String, String>> parameters = MaiString.buildListFromQuery(requestXML);
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

// Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

// Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

// Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        String responseBody = "-1";
        String urlParameters = requestXML.trim();
        try {
            URL obj = new URL(url);
            LOGGER.info("Opening connection to URL ... {}", url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Accept", "application/xml");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(60000000);
//            LOGGER.info("Your trying to connect to tips remote connection with details: ... {}",connection);
            parameters.forEach((parameter) -> {
                connection.setRequestProperty(parameter.get("item"), parameter.get("value"));
//                LOGGER.info("{}:{}", parameter.get("item"), parameter.get("value"));
            });
            DataOutputStream ds = new DataOutputStream(connection.getOutputStream());
            ds.writeBytes(urlParameters);
            ds.flush();

            int responseCode = connection.getResponseCode();

            LOGGER.info("REMOTE URL CONNECTION responseCode... {}", responseCode);
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 404:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                case 400:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                case 405:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Error while sending request [" + e.getMessage() + "]");
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
            responseBody = response.toString();
            LOGGER.info("URL[{}][{}] \nRequest: {} \nResponse: {}", url, responseCode, requestXML, responseBody);
            in.close();
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.info(null,ex);
            LOGGER.info("EXCEPTION IN GETTING TIPS TRANSACTION....{}", ex.getMessage());
        }
        LOGGER.info("response body from api.. {}",responseBody);
        return responseBody;

    }

    public static String sendXMLReqBasicAuth(String requestXML, String url, String username, String password) {
        String responseBody = "-1";
        String urlParameters = requestXML.trim();
//        LOGGER.info("PAYLOAD: " + requestXML);
//        LOGGER.info("PAYLOAD AUTH: " + username+" || "+ password);
        try {
//            LOGGER.info("======Opening Connection:" + url);
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestProperty("Content-Type", "text/xml");
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(120000);
            connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
            connection.setDoOutput(true);
            DataOutputStream ds = new DataOutputStream(connection.getOutputStream());
            ds.writeBytes(urlParameters);
            ds.flush();
//            LOGGER.info("======conn 1");
//            LOGGER.info("=======**connection {}", connection.getResponseCode());
//            LOGGER.info("======conn 2");
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 404:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 400:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 405:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Error while sending request [" + e.getMessage() + "]");
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
            responseBody = response.toString();
            // LOGGER.info("Response: " + prettyFormat(responseBody, 2));
            LOGGER.info("Closing Connection: " + url);
            LOGGER.info("Response : " + responseBody);
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.info(ex.getMessage());
        }
        return responseBody;
    }

    public static String getBasicAuthentication(String username, String password) {
        // connection.setRequestProperty("Authorization", "Basic " + getBasicAuthentication("xapi", "x@pi#811*"));
        String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return encoded;
    }

    public static String sendTxnToAPI(String requestBody, String url, String headers) {
        List<Map<String, String>> parameters = MaiString.buildListFromQuery(headers);
        String responseBody = "-1";
        String urlParameters = requestBody.trim();
        LOGGER.info("API Request: " + urlParameters);
        try {
            URL obj = new URL(url);
            LOGGER.info("Opening connection:" + url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("User-Agent", "PostmanRuntime/7.21.0");
            LOGGER.info("Start custom http header");
            parameters.forEach((parameter) -> {
                connection.setRequestProperty(parameter.get("item"), parameter.get("value"));
                LOGGER.info("{}:{}", parameter.get("item"), parameter.get("value"));
            });
            LOGGER.info("End custom http header");

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(urlParameters);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 404:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 400:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 405:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Error while sending request: {}", e);

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
                LOGGER.warn(response.toString());
            }
            LOGGER.info("Closing connection: {}", url);
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.info(null, ex);
        }
        LOGGER.info("API Response: " + responseBody);
        return responseBody;
    }

    public static String sendTxnToAPI(String requestBody, String url) {

        String responseBody = "-1";
        String urlParameters = requestBody.trim();
        LOGGER.info("API Request: " + urlParameters);
        try {
            URL obj = new URL(url);
            LOGGER.info("Opening connection:" + url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("User-Agent", "PostmanRuntime/7.21.0");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(urlParameters);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 404:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 400:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 405:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Error while sending request: {}", e);

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
                LOGGER.warn(response.toString());
            }
            LOGGER.info("Closing connection: {}", url);
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.info(null, ex);
        }
        LOGGER.info("API Response: " + responseBody);
        return responseBody;
    }

    public static String sendTxnToAPINoLogs(String requestBody, String url) {

        String responseBody = "-1";
        String urlParameters = requestBody.trim();
        LOGGER.debug("API Request: " + urlParameters);
        try {
            URL obj = new URL(url);
            LOGGER.debug("Opening connection:" + url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("User-Agent", "PostmanRuntime/7.21.0");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(urlParameters);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.debug("Error Code: " + responseCode);
                    LOGGER.debug("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                case 401:
                    LOGGER.debug("Error Code: " + responseCode);
                    LOGGER.debug("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                    LOGGER.debug("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 404:
                    LOGGER.debug("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 400:
                    LOGGER.debug("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 405:
                    LOGGER.debug("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.debug("Error Code: " + responseCode);
                    LOGGER.debug("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.debug("Error Code: " + responseCode);
                    LOGGER.debug("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.debug("Error while sending request: {}", e);

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
                LOGGER.debug(response.toString());
            }
            LOGGER.debug("Closing connection: {}", url);
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.debug(null, ex);
        }
        LOGGER.debug("API Response: " + responseBody);
        return responseBody;
    }

    public static String sendReqToAPIBcx(String requestBody, String url, String clientId) {

        String responseBody = "-1";
        String urlParameters = requestBody.trim();
        LOGGER.info("API Request: " + urlParameters);
        try {
            URL obj = new URL(url);
            LOGGER.info("Opening connection:" + url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("User-Agent", "PostmanRuntime/7.21.0");
            connection.setRequestProperty("CLIENT-ID", clientId);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(urlParameters);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 404:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 400:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 405:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Error while sending request: {}", e);

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
                LOGGER.warn(response.toString());
            }
            LOGGER.info("Closing connection: {}", url);
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.info(null, ex);
        }
        LOGGER.info("API Response: " + responseBody);
        return responseBody;
    }

    public static String sendGePGReq(String requestBody, String url) throws Exception {
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
        connection.setRequestProperty("Gepg-Com", "tpb.psp.in");
        connection.setRequestProperty("Gepg-Code", "PSP009");
        connection.setRequestProperty("Content-Length", String.valueOf(urlParameters.length()));
        try {
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(urlParameters);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                    break;
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 404:
                    LOGGER.info("Error Code: " + responseCode);
                    System.out.println("404-ERROR !!!!!!");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 400:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 405:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 502:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("502 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Connecting to GePG ", e);
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
                LOGGER.warn(response.toString());
            }
            LOGGER.info("Disconnecting...");
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.info("Connecting to GePG ", ex);
        }
        return responseBody;
    }

    public static String sendTigoPushLogin(String requestBody, String url) throws Exception {
        String responseBody = "-1";
        String urlParameters = requestBody.trim();
        URL obj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Cache-Control", "no-cache");

        try {
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(urlParameters);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    System.out.println("Error Code: " + responseCode);
                    System.out.println("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                    System.exit(0);
                case 401:
                    System.out.println("Error Code: " + responseCode);
                    System.out.println("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                case 405:
                case 400:
                case 404:
                    System.out.println("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    System.out.println("Error Code: " + responseCode);
                    System.out.println("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    System.out.println("Error Code: " + responseCode);
                    System.out.println("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("Error while sending request [" + e.getMessage() + "]");
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
                LOGGER.warn(response.toString());
            }
            System.out.println("Disconnecting...");
            connection.disconnect();
        } catch (IOException ex) {
            ex.printStackTrace();
            LOGGER.info(ex.getMessage());
        }
        return responseBody;
    }

    public static String sendTigoPushSend(String requestBody, String url, String tokenType, String accessToken) throws Exception {
        String responseBody = "-1";
        String urlParameters = requestBody.trim();
        URL obj = new URL(url);
        System.out.println("Open connection...");
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Username", "TPB");
        connection.setRequestProperty("Password", "UbQlOdD");
        connection.setRequestProperty("Authorization", tokenType + " " + accessToken);

        try {
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(urlParameters);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    System.out.println("Error Code: " + responseCode);
                    System.out.println("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                    System.exit(0);
                case 401:
                    System.out.println("Error Code: " + responseCode);
                    System.out.println("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                case 404:
                case 400:
                case 405:
                    System.out.println("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    System.out.println("Error Code: " + responseCode);
                    System.out.println("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    System.out.println("Error Code: " + responseCode);
                    System.out.println("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("Error while sending request [" + e.getMessage() + "]");
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
                LOGGER.warn(response.toString());
            }
            System.out.println("Disconnecting...");
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.info(ex.getMessage());
        }
        return responseBody;
    }

    public static String sendJsonRequest(String requestBody, String url) {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = (String hostname, SSLSession session) -> true;
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOGGER.info(null, e);
        }
        LOGGER.info("Going to send to URL: " + url);
        LOGGER.info("Request Body: " + requestBody);
        String responseBody = "-1";
        String urlParameters = requestBody.trim();
        try {
            URL obj = new URL(url);
            LOGGER.info("Open connection..." + url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Cache-Control", "no-cache");
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(urlParameters);
            }
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.warn("Error Code: " + responseCode);
                    LOGGER.warn("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                    break;
                case 401:
                    LOGGER.warn("Error Code: " + responseCode);
                    LOGGER.warn("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                case 404:
                case 400:
                case 405:
                    LOGGER.warn("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.warn("Error Code: " + responseCode);
                    LOGGER.warn("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.warn("Error Code: " + responseCode);
                    LOGGER.warn("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.warn("Error while sending request [" + e.getMessage() + "]");
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
            } else if (responseCode ==409) {
                responseBody = response.toString();
                LOGGER.info("FULL RESPONSE RETURNED: {}", response);
            } else {
                LOGGER.warn(response.toString());
            }

            LOGGER.info("Disconnecting..." + url);
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.info(ex.getMessage());
        }
        LOGGER.info("responseBody: " + responseBody);
        return responseBody;
    }



    public static String sendSMS(String smsBody, String msisdn, String sessionId, String smsGatewayUrl) {
        Random rand = new Random();
        String response = "-1";
        try {
            String msgToBeSent = smsBody.replace("&", "and");
            msgToBeSent = msgToBeSent.replace("'", "");
            String xmlMsg = "<methodCall>"
                    + "<methodName>TPB.SENDSMS</methodName>"
                    + "<params>"
                    + "<param><value><string>" + msisdn + "</string></value></param>"
                    + "<param><value><string>" + msgToBeSent + "</string></value></param>"
                    + "<param><value><string>" + sessionId + "</string></value></param>"
                    + "</params>"
                    + "</methodCall>";
            response = sendXMLRequest(xmlMsg, smsGatewayUrl);
        } catch (Exception e) {
            LOGGER.info(null, e);
        }
        return response;
    }

    public static String sendFormData(String requestBody, String url) {
        String responseBody = "-1";
        try {
            String urlParameters = requestBody.trim();
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Cache-Control", "no-cache");

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(urlParameters);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                    System.exit(0);
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 404:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 400:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 405:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Error while sending request [" + e.getMessage() + "]");
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
                LOGGER.warn(response.toString());
            }
            LOGGER.info("Disconnecting with response: {}", responseBody);
            connection.disconnect();
        } catch (IOException ex) {
            ex.printStackTrace();
            LOGGER.info(ex.getMessage());
        }
        return responseBody;
    }

    public static String sendRIMData(CreateCustomerRequest requestBody, String url) {
        String responseBody = "-1";
        String urlParameters = new Gson().toJson(requestBody);
        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Cache-Control", "no-cache");

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(urlParameters);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                    System.exit(0);
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                case 404:
                case 400:
                case 405:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Error while sending request [" + e.getMessage() + "]");
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
                LOGGER.warn(response.toString());
            }
            LOGGER.info("Disconnecting with response: {}", responseBody);
            connection.disconnect();
        } catch (IOException ex) {
            ex.printStackTrace();
            LOGGER.info(ex.getMessage());
        }
        return responseBody;
    }

    public static String approveRIM(String url) {
        String responseBody = "-1";
        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Cache-Control", "no-cache");

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                    System.exit(0);
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                case 404:
                case 400:
                case 405:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Error while sending request [" + e.getMessage() + "]");
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
                LOGGER.warn(response.toString());
            }
            LOGGER.info("Disconnecting with response: {}", responseBody);
            connection.disconnect();
        } catch (IOException ex) {
            ex.printStackTrace();
            LOGGER.info(ex.getMessage());
        }
        return responseBody;
    }

    public static String createAccount(CreateAccountRequest requestBody, String url) {
        String responseBody = "-1";
        String urlParameters = new Gson().toJson(requestBody);
        LOGGER.info("Going to send to URL: " + url);
        LOGGER.info("Request Body: " + requestBody);
        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Cache-Control", "no-cache");

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(urlParameters);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                    System.exit(0);
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                case 404:
                case 400:
                case 405:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Error while sending request [" + e.getMessage() + "]");
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
                LOGGER.warn(response.toString());
            }
            LOGGER.info("Disconnecting with response: {}", responseBody);
            connection.disconnect();
        } catch (IOException ex) {
            ex.printStackTrace();
            LOGGER.info(ex.getMessage());
        }
        return responseBody;
    }

    public static String enrollMobileChannel(EMCReq requestBody, String url) {
        String responseBody = "-1";
        String urlParameters = new Gson().toJson(requestBody);
        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Cache-Control", "no-cache");

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(urlParameters);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                    System.exit(0);
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                case 404:
                case 400:
                case 405:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Error while sending request [" + e.getMessage() + "]");
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
                LOGGER.warn(response.toString());
            }
            LOGGER.info("Disconnecting with response: {}", responseBody);
            connection.disconnect();
        } catch (IOException ex) {
            ex.printStackTrace();
            LOGGER.info(ex.getMessage());
        }
        return responseBody;
    }


    //    public static String sendXMLRequestToBot(String requestXML, String url, String reference, String senderBank, String reciverBank, String botToken, String certificate) {
//        try {
//// Create a trust manager that does not validate certificate chains
//            TrustManager[] trustAllCerts;
//            trustAllCerts = new TrustManager[]{new X509TrustManager() {
//                @Override
//                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//                    return null;
//                }
//
//                @Override
//                public void checkClientTrusted(X509Certificate[] certs, String authType) {
//                }
//
//                @Override
//                public void checkServerTrusted(X509Certificate[] certs, String authType) {
//                }
//            }
//            };
//
//// Install the all-trusting trust manager
//            SSLContext sc = SSLContext.getInstance("TLSv1.2");
//            sc.init(null, trustAllCerts, new java.security.SecureRandom());
//            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//
//// Create all-trusting host name verifier
//            HostnameVerifier allHostsValid = (String hostname, SSLSession session) -> true;
//
//// Install the all-trusting host verifier
//            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
//        } catch (NoSuchAlgorithmException | KeyManagementException e) {
//        }
//
//        String responseBody = "-1";
//        String urlParameters = requestXML.trim();
//        LOGGER.info("PAYLOAD: " + urlParameters);
//        try {
//            LOGGER.info("Opening Connection: " + url);
//            URL obj = new URL(url);
//            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
//            connection.setRequestProperty("Content-Type", "text/plain");
//            connection.setRequestProperty("payload_type", "TEXT");
//            connection.setRequestProperty("sender", senderBank);
//            connection.setRequestProperty("consumer", reciverBank);
//            connection.setRequestProperty("Authorization", botToken);
//            connection.setRequestProperty("msgid", senderBank + reference);
//            connection.setRequestMethod("POST");
//            connection.setDoOutput(true);
//            connection.setConnectTimeout(60000);
//            DataOutputStream ds = new DataOutputStream(connection.getOutputStream());
//            ds.writeBytes(urlParameters);
//            ds.flush();
//            int responseCode = connection.getResponseCode();
//            BufferedReader in = null;
//            switch (responseCode) {
//                case 200:
//                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                    break;
//                case 301:
//                    LOGGER.info("Error Code: " + responseCode);
//                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
//                case 401:
//                    LOGGER.info("Error Code: " + responseCode);
//                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
//                    break;
//                case 403:
//                    LOGGER.info("Error Code: " + responseCode);
//                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                    break;
//                case 404:
//                    LOGGER.info("Error Code: " + responseCode);
//                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                    break;
//                case 400:
//                    LOGGER.info("Error Code: " + responseCode);
//                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                    break;
//                case 405:
//                    LOGGER.info("Error Code: " + responseCode);
//                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                    break;
//                case 503:
//                    LOGGER.info("Error Code: " + responseCode);
//                    LOGGER.info("503 Service Temporarily Unavailable");
//                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                    break;
//                case 202:
//                    LOGGER.info("Error Code: " + responseCode);
//                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
//                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                    break;
//                default:
//                    try {
//                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                } catch (IOException e) {
//                    LOGGER.info("Error while sending request [" + e.getMessage() + "]");
//                }
//                break;
//            }
//            String inputLine;
//            StringBuilder response = new StringBuilder();
//            if (in != null) {
//                while ((inputLine = in.readLine()) != null) {
//                    response.append(inputLine);
//                }
//                in.close();
//            }
//            responseBody = response.toString();
//            LOGGER.info("Closing Connection: " + url);
//            LOGGER.info("Response : " + responseBody);
//            connection.disconnect();
//        } catch (IOException ex) {
//            LOGGER.info(null, ex);
//
//            LOGGER.info("{} {}", url, ex.getMessage());
//        }
//        return responseBody;
//    }
//
    public static String sendXMLRequestToBot(String signedRequest, String url, String reference, String senderBank, String receiverBank, String botToken, String certificatePath, String certPassword) throws Exception {
        String response = "-1";
        LOGGER.info("PAYLOAD:=> {}", signedRequest);
        CloseableHttpClient httpClient = null;
        InputStream inputStream = null;
        try {
            LOGGER.info("OPENING CONNECTION TO: " + url);
            File f = new File(certificatePath);
            // below code establishes 2-way SSL connection on sso.pjm.com with client certificate
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            KeyStore keystore = KeyStore.getInstance("JKS");
            inputStream = new FileInputStream(f);

            keystore.load(inputStream, certPassword.toCharArray());
            SSLContext sslContext = SSLContexts.custom().loadKeyMaterial(keystore,
                            certPassword.toCharArray())
                    .loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
            SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", sslConnectionFactory).build();
            BasicHttpClientConnectionManager connManager = new BasicHttpClientConnectionManager(registry);
            httpClient
                    = HttpClients.custom().setConnectionManager(connManager).setSSLSocketFactory(sslConnectionFactory)
                    .build();
            String ssourl = url;

            HttpPost httpPost = new HttpPost(ssourl);
            httpPost.addHeader("Content-Type", "text/plain");
            httpPost.addHeader("payload_type", "TEXT");
            httpPost.addHeader("sender", senderBank);
            httpPost.addHeader("consumer", receiverBank);
            httpPost.addHeader("Authorization", botToken);
            httpPost.addHeader("msgid", senderBank + reference);
            StringEntity se = new StringEntity(signedRequest);
            httpPost.setEntity(se);
            HttpResponse httpResponse = httpClient.execute(httpPost);
            response = EntityUtils.toString(httpResponse.getEntity());
            int statuscode = httpResponse.getStatusLine().getStatusCode();
            if (statuscode != 200 || statuscode != 202) {
                LOGGER.error("ERROR SENDING PAYLOAD TO BOT: ERROR CODE:{} DETAILS {} ", statuscode, response);
                //throw new RuntimeException("error on sso login status code: " + statuscode);
            }
        } catch (ClientProtocolException e) {
            LOGGER.error("ERROR SENDING REQUEST TO BOT: ", e);
            //throw new RuntimeException("unable to create sso token ", e);
        } catch (IOException e) {
            LOGGER.error("ERROR SENDING REQUEST TO BOT:", e);
        } finally {
            if (httpClient != null) {
                httpClient.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return response;
    }

    public static Map<String, String> sendTxnToBrinjal(String requestBody, String url, String headers, String MethodType) throws Exception {
        List<Map<String, String>> parameters = MaiString.buildListFromQuery(headers);
        Map<String, String> responseFromApi = new HashMap<>();
        LOGGER.info("PAYLOAD :=> {}", requestBody);
        InputStream inputStream = null;
        HttpClient httpClient = null;
        try {
            LOGGER.info("OPENING CONNECTION TO: " + url);
            httpClient = HttpClients.createDefault();

            StringEntity se = new StringEntity(requestBody);
            if (MethodType.equalsIgnoreCase("POST")) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.addHeader("Accept", "application/json");
                httpPost.addHeader("Content-Type", "application/json");
                httpPost.addHeader("Cache-Control", "no-cache");
                parameters.forEach((parameter) -> {
                    httpPost.addHeader(parameter.get("item"), parameter.get("value"));
                    LOGGER.info("{}:{}", parameter.get("item"), parameter.get("value"));
//                            customHeaders = customHeaders + parameter.get("item") + ":" + parameter.get("value");
                });
                httpPost.setEntity(se);
                HttpResponse httpResponse = httpClient.execute(httpPost);
                String response = EntityUtils.toString(httpResponse.getEntity());
                int statuscode = httpResponse.getStatusLine().getStatusCode();
//                LOGGER.error("RESPONSE FROM BRINJAL, RESPONSE CODE:{} DETAILS {} ", statuscode, response);
                responseFromApi.put("httpResponseCode", String.valueOf(statuscode));
                responseFromApi.put("responseBody", response);
            } else if (MethodType.equalsIgnoreCase("GET")) {
                HttpGet httpPost = new HttpGet(url);
                httpPost.addHeader("Accept", "application/json");
                httpPost.addHeader("Content-Type", "application/json");
                httpPost.addHeader("Cache-Control", "no-cache");
                parameters.forEach((parameter) -> {
                    httpPost.addHeader(parameter.get("item"), parameter.get("value"));
                    LOGGER.info("{}:{}", parameter.get("item"), parameter.get("value"));
                });
//                httpPost.addHeader("signature", signature);

//                httpPost.setEntity(se);
                HttpResponse httpResponse = httpClient.execute(httpPost);
                String response = EntityUtils.toString(httpResponse.getEntity());
//                httpPost.addHeader("signature", signature);
                int statuscode = httpResponse.getStatusLine().getStatusCode();
                LOGGER.error("RESPONSE FROM BRINJAL, RESPONSE CODE:{} DETAILS {} ", statuscode, response);
                //throw new RuntimeException("error on sso login status code: " + statuscode);
                responseFromApi.put("httpResponseCode", String.valueOf(statuscode));
                responseFromApi.put("responseBody", response);
            } else if (MethodType.equalsIgnoreCase("PUT")) {
                HttpPut httpPost = new HttpPut(url);
                httpPost.addHeader("Accept", "application/json");
                httpPost.addHeader("Content-Type", "application/json");
                httpPost.addHeader("Cache-Control", "no-cache");
                parameters.forEach((parameter) -> {
                    httpPost.addHeader(parameter.get("item"), parameter.get("value"));
                    LOGGER.info("{}:{}", parameter.get("item"), parameter.get("value"));
                });
//                httpPost.addHeader("signature", signature);

                httpPost.setEntity(se);
                HttpResponse httpResponse = httpClient.execute(httpPost);
                String response = EntityUtils.toString(httpResponse.getEntity());
                int statuscode = httpResponse.getStatusLine().getStatusCode();
//                LOGGER.error("RESPONSE FROM BRINJAL, RESPONSE CODE:{} DETAILS {} ", statuscode, response);
                //throw new RuntimeException("error on sso login status code: " + statuscode);
                responseFromApi.put("httpResponseCode", String.valueOf(statuscode));
                responseFromApi.put("responseBody", response);
            }
        } catch (ClientProtocolException e) {
            responseFromApi.put("httpResponseCode", "999");
            responseFromApi.put("responseBody", "-1");
            LOGGER.error("ERROR SENDING REQUEST TO BRINJAL: ", e);
            //throw new RuntimeException("unable to create sso token ", e);
        } catch (IOException e) {
            responseFromApi.put("httpResponseCode", "999");
            responseFromApi.put("responseBody", "-1");
            LOGGER.error("ERROR SENDING REQUEST TO BRINJAL:", e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return responseFromApi;
    }

    public static String sendXMLRequest(String requestXML, String url, String headers) {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        List<Map<String, String>> parameters = MaiString.buildListFromQuery(headers);

        String responseBody = "-1";
        String urlParameters = requestXML.trim();
        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestProperty("Content-Type", "text/xml");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(60000);
            LOGGER.info("Start custom http header");
            parameters.forEach((parameter) -> {
                connection.setRequestProperty(parameter.get("item"), parameter.get("value"));
                LOGGER.info("{}:{}", parameter.get("item"), parameter.get("value"));
            });
            LOGGER.info("End custom http header");
            DataOutputStream ds = new DataOutputStream(connection.getOutputStream());
            ds.writeBytes(urlParameters);
            ds.flush();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            switch (responseCode) {
                case 200:
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    break;
                case 301:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("This tells a browser that the resource it asked for has permanently moved to a new location. The response should also include the location. It also tells the browser which URL to use the next time it wants to fetch it.");
                case 401:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("A 401 status code indicates that before a resource can be accessed, the client must be authorised by the server.");
                    break;
                case 403:
                case 405:
                case 404:
                case 400:
                    LOGGER.info("Error Code: " + responseCode);
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 502:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("502 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 503:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("503 Service Temporarily Unavailable");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                case 202:
                    LOGGER.info("Error Code: " + responseCode);
                    LOGGER.info("202 Accepted. The request has been accepted for processing, but the processing has not been completed");
                    in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    break;
                default:
                    try {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (IOException e) {
                        LOGGER.info("Error while sending request [" + e.getMessage() + "]");
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
            responseBody = response.toString();
            LOGGER.info("URL[{}][{}] \nRequest: {} \nResponse: {}", url, responseCode, requestXML, responseBody);
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.info("URL[{}][{}] \nRequest: {} \nResponse: {} \n Exception:{}", url, 500, requestXML, responseBody, ex.getMessage());
        }
        return responseBody;
    }

}
