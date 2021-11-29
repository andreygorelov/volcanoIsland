package net.volcano.island.reservation.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class Reservation {
    private String reservationId;
    private String email;
    private LocalDate startDate;
    private LocalDate endDate;
}
