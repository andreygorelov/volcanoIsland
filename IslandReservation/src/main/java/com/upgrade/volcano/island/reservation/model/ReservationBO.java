package com.upgrade.volcano.island.reservation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ReservationBO {
    private String reservationId;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate startDate;
    private LocalDate endDate;
}
