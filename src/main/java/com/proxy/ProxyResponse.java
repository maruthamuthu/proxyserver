package com.proxy;

import org.json.JSONObject;

class ProxyResponse
{
    private int statusCode;
    private String response;

    private ProxyResponse(int statusCode, String response)
    {
        this.statusCode = statusCode;
        this.response = response;
    }

    int getStatusCode()
    {
        return statusCode;
    }

    String getResponse()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", statusCode);
        jsonObject.put("response", response);
        return jsonObject.toString();
        //return response;
    }

    static ProxyResponse of(int statusCode, String response)
    {
        return new ProxyResponse(statusCode, response);
    }
}
