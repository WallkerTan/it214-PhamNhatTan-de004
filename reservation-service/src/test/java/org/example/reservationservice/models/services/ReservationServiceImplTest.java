package org.example.reservationservice.models.services;

import org.example.reservationservice.exceptions.RoomNotFoundException;
import org.example.reservationservice.models.constants.ReservationStatus;
import org.example.reservationservice.models.dto.requests.CreateReservationDetailRequest;
import org.example.reservationservice.models.dto.requests.CreateReservationRequest;
import org.example.reservationservice.models.dto.responses.ReservationResponse;
import org.example.reservationservice.models.dto.responses.RoomResponse;
import org.example.reservationservice.models.entities.Reservation;
import org.example.reservationservice.models.entities.ReservationDetail;
import org.example.reservationservice.models.repositories.ReservationDetailRepository;
import org.example.reservationservice.models.repositories.ReservationRepository;
import org.example.reservationservice.models.services.impl.ReservationServiceImpl;
import org.example.reservationservice.models.services.impl.RoomGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationServiceImplTest {

    private ReservationRepository reservationRepository;
    private ReservationDetailRepository reservationDetailRepository;
    private RoomGatewayService roomGatewayService;
    private KafkaTemplate<String, Object> kafkaTemplate;
    private ReservationServiceImpl reservationService;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        reservationDetailRepository = mock(ReservationDetailRepository.class);
        roomGatewayService = mock(RoomGatewayService.class);
        kafkaTemplate = mock(KafkaTemplate.class);

        reservationService = new ReservationServiceImpl(
                reservationRepository,
                reservationDetailRepository,
                roomGatewayService,
                kafkaTemplate
        );
        ReflectionTestUtils.setField(reservationService, "reservationCreatedTopic", "resvervation-created");
    }

    @Test
    void createReservationSuccess() {
        CreateReservationDetailRequest detailRequest = new CreateReservationDetailRequest(1L, 3);
        CreateReservationRequest request = new CreateReservationRequest(
                "Nguyen Van A",
                "vana@example.com",
                List.of(detailRequest)
        );

        RoomResponse roomResponse = new RoomResponse(1L, "101", "Deluxe", 500000.0);
        when(roomGatewayService.getRoomById(1L)).thenReturn(roomResponse);

        Reservation savedReservation = Reservation.builder()
                .id(10L)
                .customerName("Nguyen Van A")
                .customerEmail("vana@example.com")
                .totalAmount(1500000.0)
                .status(ReservationStatus.PENDING)
                .build();
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        ReservationDetail savedDetail = ReservationDetail.builder()
                .id(100L)
                .reservation(savedReservation)
                .roomId(1L)
                .nights(3)
                .pricePerNight(500000.0)
                .build();
        when(reservationDetailRepository.save(any(ReservationDetail.class))).thenReturn(savedDetail);

        ReservationResponse response = reservationService.createReservation(request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Nguyen Van A", response.customerName());
        assertEquals("vana@example.com", response.customerEmail());
        assertEquals(ReservationStatus.PENDING, response.status());
        assertEquals(1500000.0, response.totalAmount());
        assertEquals(1, response.items().size());
        assertEquals(100L, response.items().get(0).id());
        assertEquals(1500000.0, response.items().get(0).lineTotal());

        verify(kafkaTemplate).send("resvervation-created", "vana@example.com");
    }

    @Test
    void createReservationThrowsWhenRoomNotFound() {
        CreateReservationDetailRequest detailRequest = new CreateReservationDetailRequest(999L, 2);
        CreateReservationRequest request = new CreateReservationRequest(
                "Nguyen Van B",
                "vanb@example.com",
                List.of(detailRequest)
        );

        when(roomGatewayService.getRoomById(999L)).thenThrow(new RoomNotFoundException(999L));

        assertThrows(RoomNotFoundException.class, () -> reservationService.createReservation(request));
    }
}
