package com.proxy;

import com.proxy.redis.ThresholdMonitor;
import org.json.JSONException;
import org.json.JSONObject;
import spark.QueryParamsMap;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.regex.Pattern;

class ProxyHandler
{
    private static final Pattern ALLOWED_REQUEST_METHODS = Pattern.compile("^(POST|PUT|GET|DELETE)$");


    static ProxyResponse handleInvocation(QueryParamsMap queryParamsMap)
    {
        String clientID = queryParamsMap.get("client_id").value();
        String requestUrl = queryParamsMap.get("request_url").value();

        String method = queryParamsMap.get("method").value();

        if (isEmpty(clientID) || isEmpty(requestUrl) || isEmpty(method) || !ALLOWED_REQUEST_METHODS.matcher(method).matches())
        {
            return ProxyResponse.of(HttpServletResponse.SC_BAD_REQUEST, "Mandatory parameters are missing, please refer the API doc.");
        }

        String headerString = queryParamsMap.get("headers").value();
        JSONObject headers = null;
        if (!isEmpty(headerString))
        {
            try
            {
                headers = new JSONObject(headerString);
            }
            catch (JSONException e)
            {
                return ProxyResponse.of(HttpServletResponse.SC_BAD_REQUEST, "Invalid value passed for header, please refer the API doc.");
            }
        }
        String body = queryParamsMap.get("request_body").value();
        return invoke(clientID, requestUrl, method, headers, body);
    }

    private static boolean isEmpty(String val)
    {
        return (val == null || val.trim().length() == 0);
    }

    private static ProxyResponse invoke(String clientID, String urlString, String method, JSONObject headers, String requestBody)
    {
        // We can aslo validate a valid clientID if required.
        if (ThresholdMonitor.canAllow(clientID))
        {
            try
            {
                URL url = new URL(urlString);
                String protocol = url.getProtocol();

                if (!"https".equals(protocol))
                {
                    return ProxyResponse.of(HttpServletResponse.SC_PRECONDITION_FAILED, "HTTPS requests only accepted.");
                }
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                if (headers != null)
                {
                    Iterator<String> iterator = headers.keys();
                    iterator.forEachRemaining(key -> connection.setRequestProperty(key, headers.optString(key)));
                }

                connection.setConnectTimeout(5000); // 5 seconds
                connection.setReadTimeout(5000);
                connection.setRequestMethod(method);

                if ("POST".equals(method) || "PUT".equals(method))
                {
                    byte[] postData = requestBody.getBytes(StandardCharsets.UTF_8);
                    connection.setRequestProperty("charset", "UTF-8");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
                    try(DataOutputStream wr = new DataOutputStream(connection.getOutputStream()))
                    {
                        wr.write(postData);
                        wr.flush();
                    }
                }

                connection.connect();
                int statusCode = connection.getResponseCode();
                String output;

                if (statusCode == HttpServletResponse.SC_CREATED || statusCode == HttpServletResponse.SC_OK)
                {
                    output = convertStreamToString(connection.getInputStream());
                }
                else
                {
                    output = convertStreamToString(connection.getErrorStream());
                }

                return ProxyResponse.of(statusCode, output);

            }
            catch (MalformedURLException malformedException)
            {
                return ProxyResponse.of(HttpServletResponse.SC_BAD_REQUEST, "Invalid request URL.");
            }
            catch (IOException ioException)
            {
                if (ioException instanceof SocketTimeoutException)
                {
                    return ProxyResponse.of(HttpServletResponse.SC_REQUEST_TIMEOUT, "Can't connect/read the resource within the time limit.");
                }
                if (ioException instanceof UnknownHostException)
                {
                    return ProxyResponse.of(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Can't connect the (unknown) host");
                }
                return ProxyResponse.of(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Can't read/write the end server.");
            }
        }
        else
        {
            return ProxyResponse.of(429, "Too many requests.");
        }


    }

    private static String convertStreamToString(InputStream in) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        String content;
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(in)))
        {
            while((content = reader.readLine()) != null)
            {
                sb.append(content);
            }
        }
        return sb.toString();
    }
}
