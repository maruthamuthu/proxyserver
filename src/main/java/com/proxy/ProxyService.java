package com.proxy;

import static spark.Spark.init;
import static spark.Spark.port;
import static spark.Spark.post;

public class ProxyService
{
    public static void main(String[] args)
    {
        port(3000);

        post("/invoke", "application/json", (request, response) -> {
            ProxyResponse httpResponse = ProxyHandler.handleInvocation(request.queryMap());
            response.status(httpResponse.getStatusCode());
            response.type("application/json");
            return httpResponse.getResponse();
        });

        init();
    }
}
