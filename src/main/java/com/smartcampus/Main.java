/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus;

/**
 *
 * @author sawiru
 * ID Number : w2120775
 */

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.net.URI;


public class Main {
    
    public static final String BASE_URI = "http://localhost:8080/";
    
    public static void main(String[] args) throws Exception {
        ResourceConfig config = new ResourceConfig()
                .packages("com.smartcampus.resource", "com.smartcampus.exception")
                .register(JacksonFeature.class);

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create("http://localhost:8080/api/v1/"), config);

        System.out.println("Smart Campus API running at : http://localhost:8080/api/v1");
        System.out.println("Press ENTER to stop...");
        System.in.read();
        server.stop();    }
}
