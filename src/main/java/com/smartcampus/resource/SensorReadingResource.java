/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

/**
 *
 * @author sawiru
 */

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.storage.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;


@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // GET /api/v1/sensors/{sensorId}/readings — get all readings for a sensor
    @GET
    public Response getAllReadings() {
        Sensor sensor = DataStore.sensors.get(sensorId);

        if (sensor == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "404 Not Found");
            error.put("message", "Sensor with ID " + sensorId + " was not found.");
            return Response.status(404).entity(error).build();
        }

        List<SensorReading> readings = DataStore.readings.get(sensorId);

        if (readings == null) {
            readings = new ArrayList<SensorReading>();
        }

        return Response.ok(readings).build();
    }

    // POST /api/v1/sensors/{sensorId}/readings — add a new reading
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = DataStore.sensors.get(sensorId);

        // Check sensor exists
        if (sensor == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "404 Not Found");
            error.put("message", "Sensor with ID " + sensorId + " was not found.");
            return Response.status(404).entity(error).build();
        }

        // Check sensor is not under maintenance — throws 403 if it is
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId);
        }

        // Generate a unique ID and timestamp if not provided
        if (reading.getId() == null || reading.getId().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }

        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Save the reading
        List<SensorReading> readings = DataStore.readings.get(sensorId);
        if (readings == null) {
            readings = new ArrayList<SensorReading>();
            DataStore.readings.put(sensorId, readings);
        }
        readings.add(reading);

        // Side effect — update the sensor's currentValue with the new reading
        sensor.setCurrentValue(reading.getValue());

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("message", "Reading added successfully.");
        response.put("reading", reading);
        response.put("sensorCurrentValue", sensor.getCurrentValue());
        return Response.status(201).entity(response).build();
    }
}