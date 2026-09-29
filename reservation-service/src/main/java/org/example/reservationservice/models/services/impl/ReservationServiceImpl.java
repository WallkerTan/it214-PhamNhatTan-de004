package org.example.reservationservice.models.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.reservationservice.models.constants.ReservationStatus;
import org.example.reservationservice.models.dto.requests.CreateReservationDetailRequest;
import org.example.reservationservice.models.dto.requests.CreateReservationRequest;
import org.example.reservationservice.models.dto.responses.ReservationDetailResponse;
import org.example.reservationservice.models.dto.responses.ReservationResponse;
import org.example.reservationservice.models.dto.responses.RoomResponse;
import org.example.reservationservice.models.entities.Reservation;
import org.example.reservationservice.models.entities.ReservationDetail;
import org.example.reservationservice.models.repositories.ReservationDetailRepository;
import org.example.reservationservice.models.repositories.ReservationRepository;
import org.example.reservationservice.models.services.ReservationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationDetailRepository reservationDetailRepository;
    private final RoomGatewayService roomGatewayService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.reservation-created-topic:resvervation-created}")
    private String reservationCreatedTopic;

    @Override
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        double totalAmount = 0.0;

        record DetailItem(CreateReservationDetailRequest requestItem, RoomResponse roomResponse, double lineTotal) {}
        List<DetailItem> detailItems = new ArrayList<>();

        for (CreateReservationDetailRequest item : request.items()) {
            RoomResponse roomResponse = roomGatewayService.getRoomById(item.roomId());
            double pricePerNight = roomResponse.pricePerNight() != null ? roomResponse.pricePerNight() : 0.0;
            double lineTotal = pricePerNight * item.nights();
            totalAmount += lineTotal;
            detailItems.add(new DetailItem(item, roomResponse, lineTotal));
        }

        Reservation reservation = Reservation.builder()
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .totalAmount(totalAmount)
                .status(ReservationStatus.PENDING)
                .build();

        reservation = reservationRepository.save(reservation);

        List<ReservationDetailResponse> detailResponses = new ArrayList<>();
        for (DetailItem item : detailItems) {
            ReservationDetail detail = ReservationDetail.builder()
                    .reservation(reservation)
                    .roomId(item.roomResponse().roomId() != null ? item.roomResponse().roomId() : item.requestItem().roomId())
                    .nights(item.requestItem().nights())
                    .pricePerNight(item.roomResponse().pricePerNight())
                    .build();

            detail = reservationDetailRepository.save(detail);

            detailResponses.add(new ReservationDetailResponse(
                    detail.getId(),
                    detail.getRoomId(),
                    item.roomResponse().roomNumber(),
                    item.roomResponse().roomType(),
                    detail.getNights(),
                    detail.getPricePerNight(),
                    item.lineTotal()
            ));
        }

        try {
            kafkaTemplate.send(reservationCreatedTopic, reservation.getCustomerEmail());
            log.info("Sent Kafka reservation created notification to topic {} for email: {}", reservationCreatedTopic, reservation.getCustomerEmail());
        } catch (Exception e) {
            log.error("Failed to send Kafka message to topic {}: {}", reservationCreatedTopic, e.getMessage(), e);
        }

        return new ReservationResponse(
                reservation.getId(),
                reservation.getCustomerName(),
                reservation.getCustomerEmail(),
                reservation.getTotalAmount(),
                reservation.getStatus(),
                detailResponses
        );
    }
}
