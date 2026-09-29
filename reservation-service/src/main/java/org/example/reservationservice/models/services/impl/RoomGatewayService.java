package org.example.reservationservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.reservationservice.clients.RoomClient;
import org.example.reservationservice.exceptions.RoomNotFoundException;
import org.example.reservationservice.exceptions.RoomServiceException;
import org.example.reservationservice.models.dto.responses.RoomResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomGatewayService {

    private final RoomClient roomClient;

    public RoomResponse getRoomById(Long roomId) {
        throw new UnsupportedOperationException();

    }
}
