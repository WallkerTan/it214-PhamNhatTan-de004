package org.example.reservationservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.reservationservice.clients.RoomClient;
import org.example.reservationservice.exceptions.RoomNotFoundException;
import org.example.reservationservice.exceptions.RoomServiceException;
import org.example.reservationservice.models.dto.responses.RoomResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomGatewayService {

    private final RoomClient roomClient;

    @CircuitBreaker(name = "roomService", fallbackMethod = "fallbackGetRoomById")
    public RoomResponse getRoomById(Long roomId) {
        try {
            RoomResponse response = roomClient.getRoomById(roomId);
            if (response == null) {
                throw new RoomNotFoundException(roomId);
            }
            return response;
        } catch (FeignException.NotFound e) {
            throw new RoomNotFoundException(roomId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new RoomNotFoundException(roomId);
            }
            throw new RoomServiceException("Room service error: " + e.getMessage(), e);
        } catch (RoomNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RoomServiceException("Unable to call room service: " + e.getMessage(), e);
        }
    }

    public RoomResponse fallbackGetRoomById(Long roomId, Throwable t) {
        if (t instanceof RoomNotFoundException) {
            throw (RoomNotFoundException) t;
        }
        if (t instanceof FeignException.NotFound || (t instanceof FeignException && ((FeignException) t).status() == 404)) {
            throw new RoomNotFoundException(roomId);
        }
        if (t instanceof RoomServiceException) {
            throw (RoomServiceException) t;
        }
        log.error("Room service fallback invoked for roomId {}: {}", roomId, t.getMessage());
        throw new RoomServiceException("Room service is unavailable: " + t.getMessage(), t);
    }
}
