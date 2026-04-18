/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

/**
 *
 * @author sawiru
 */

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.storage.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {
    
    // GET/api/v1/rooms - returns all rooms
    @GET
    public Response getAllRooms() {
        List<Room> allRooms = new ArrayList<>(DataStore.rooms.values());
        return Response.ok(allRooms).build();
    }
    
    // POST /api/v1/rooms - create a new room
    @POST
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        if (room.getId() == null || room.getId().isEmpty()) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "400 Bad Request");
            error.put("message", "Room ID is required.");
            return Response.status(400).entity(error).build();
    }
        if (DataStore.rooms.containsKey(room.getId())) {
        Map<String, String> error = new HashMap<String, String>();
        error.put("error", "409 Conflict");
        error.put("message", "A room with ID " + room.getId() + " already exists.");
        return Response.status(409).entity(error).build();
    }
        
        DataStore.rooms.put(room.getId(), room);

        // Build the location header
        java.net.URI location = uriInfo.getAbsolutePathBuilder()
                .path(room.getId())
                .build();

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("message", "Room created successfully.");
        response.put("room", room);
        return Response.created(location).entity(response).build();
}

// GET /api/v1/rooms/{roomId} — get a specific room
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = DataStore.rooms.get(roomId);

        if (room == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "404 Not Found");
            error.put("message", "Room with ID " + roomId + " was not found.");
            return Response.status(404).entity(error).build();
        }

        return Response.ok(room).build();
    }
    
    // DELETE /api/v1/rooms/{roomId} — delete a room (only if no sensors assigned)
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.rooms.get(roomId);

        if (room == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "404 Not Found");
            error.put("message", "Room with ID " + roomId + " was not found.");
            return Response.status(404).entity(error).build();
        }

        // Safety check — block deletion if sensors are still assigned
        List<String> sensorIds = room.getSensorIds();
        if (sensorIds != null && !sensorIds.isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }

        DataStore.rooms.remove(roomId);

        Map<String, String> response = new HashMap<String, String>();
        response.put("message", "Room " + roomId + " has been deleted successfully.");
        return Response.ok(response).build();
    }
}
