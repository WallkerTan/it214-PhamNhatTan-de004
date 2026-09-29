package org.example.reservationservice.models.services;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.example.reservationservice.clients.RoomClient;
import org.example.reservationservice.exceptions.RoomNotFoundException;
import org.example.reservationservice.exceptions.RoomServiceException;
import org.example.reservationservice.models.dto.responses.RoomResponse;
import org.example.reservationservice.models.services.impl.RoomGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomGatewayServiceTest {

    private RoomClient roomClient;
    private RoomGatewayService roomGatewayService;

    @BeforeEach
    void setUp() {
        roomClient = mock(RoomClient.class);
        roomGatewayService = new RoomGatewayService(roomClient);
    }

    @Test
    void getRoomByIdSuccess() {
        RoomResponse expected = new RoomResponse(1L, "101", "Deluxe", 500000.0);
        when(roomClient.getRoomById(1L)).thenReturn(expected);

        RoomResponse actual = roomGatewayService.getRoomById(1L);

        assertNotNull(actual);
        assertEquals(expected.roomId(), actual.roomId());
        assertEquals(expected.roomNumber(), actual.roomNumber());
    }

    @Test
    void getRoomByIdThrowsRoomNotFoundExceptionWhenNotFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/rooms/99", Collections.emptyMap(), null, new RequestTemplate());
        when(roomClient.getRoomById(99L)).thenThrow(new FeignException.NotFound("Not found", request, null, null));

        assertThrows(RoomNotFoundException.class, () -> roomGatewayService.getRoomById(99L));
    }

    @Test
    void getRoomByIdThrowsRoomServiceExceptionOnGenericError() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/rooms/99", Collections.emptyMap(), null, new RequestTemplate());
        when(roomClient.getRoomById(99L)).thenThrow(new FeignException.InternalServerError("Server error", request, null, null));

        assertThrows(RoomServiceException.class, () -> roomGatewayService.getRoomById(99L));
    }
}
