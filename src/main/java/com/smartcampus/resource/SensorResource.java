/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

/**
 *
 * @author sawiru
 */
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.storage.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class SensorResource {
    
    // GET /api/v1/sensors — get all sensors, optional ?type= filter
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> result = new ArrayList<>(DataStore.sensors.values());
        
        if (type != null && !type.isEmpty()) {
            List<Sensor> filtered = new ArrayList<Sensor>();
            for (Sensor s : result) {
                if(s.getType().equalsIgnoreCase(type)) {
                    filtered.add(s);
                }
            }
            return Response.ok(filtered).build();
        }
        return Response.ok(result).build();
    }
    
    // GET /api/v1/sensors/{sensorId} — get a specific sensor
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.sensors.get(sensorId);
        
        if (sensor == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "404 Not Found");
            error.put("message", "Sensor with ID " + sensorId + " was not found.");
            return Response.status(404).entity(error).build();
        }
        
        return Response.ok(sensor).build();
    }
    
    // POST /api/v1/sensors — register a new sensor
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        // Validate sensor ID
        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "400 Bad Request");
            error.put("message", "Sensor ID is required.");
            return Response.status(400).entity(error).build();
        }

        // Validate sensor type
        if (sensor.getType() == null || sensor.getType().isEmpty()) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "400 Bad Request");
            error.put("message", "Sensor type is required.");
            return Response.status(400).entity(error).build();
        }

        // Check sensor doesn't already exist
        if (DataStore.sensors.containsKey(sensor.getId())) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "409 Conflict");
            error.put("message", "A sensor with ID " + sensor.getId() + " already exists.");
            return Response.status(409).entity(error).build();
        }

        // Validate that the roomId actually exists — throws 422 if not
        if (sensor.getRoomId() == null || !DataStore.rooms.containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                "Room with ID '" + sensor.getRoomId() + "' does not exist. Cannot register sensor."
            );
        }

        // Add sensor to DataStore
        DataStore.sensors.put(sensor.getId(), sensor);

        // Also add sensorId to the room's sensorIds list
        DataStore.rooms.get(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        // Init an empty readings list for this sensor
        DataStore.readings.put(sensor.getId(), new ArrayList<>());

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("message", "Sensor registered successfully.");
        response.put("sensor", sensor);
        return Response.status(201).entity(response).build();
    }

    // Sub-resource locator — delegates to SensorReadingResource
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

}
