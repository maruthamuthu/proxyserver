# Simple Proxy Server

## Please follow the below steps to build and run:

   1. Clone this repo
   2. run - **mvn clean install**
   3. run - **mvn exec:java -Dexec.mainClass="com.proxy.ProxyService"**
   
 Before running this, please make sure you have installed maven and redis (runinng on port 6379 - default port). 
 
 This project use the following libraries:
 
    1. SparkJava 
            The SparkJava is a web application frameworks and it's internally use the Eclipse jetty server (we can change this).
        Alternatively, we can use Jersy REST framework but SparkJava is very simple. So if we are building a very few API's then
        the SparkJava is better choice. 
    
    2. Redis
           The Redis is a in-memory key-value pair database. This is used to handle a client threshold with key expire.
           
    3. org.json
           This will provide the JSONObject and JSONArray implementaion, for handling the request parameters.
           
    4. Apache commons-pool
              This libray is used to create a Redis connection pool and it have a two dependent library (slj4).
              
    5. Jedis
               This is the java client library for Redis.

