package com.upgrade.volcano.island.reservation.rest;

import com.upgrade.volcano.island.reservation.rest.model.ReservationDTO;
import com.upgrade.volcano.island.reservation.rest.model.mapper.ReservationMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.extern.slf4j.Slf4j;
import com.upgrade.volcano.island.reservation.ReservationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Validated
@RestController
public class ReservationController extends BaseController {

    private final ReservationMapper mapper;
    private final ReservationManager reservationManager;

    @Autowired
    public ReservationController(ReservationMapper mapper, ReservationManager reservationManager) {
        this.mapper = mapper;
        this.reservationManager = reservationManager;
    }

    @GetMapping("/api/campsite/reservation/availableDates")
    public ResponseEntity<List<LocalDate>> getAvailableCampsiteDates(@RequestParam(value = "startDate", required = false)
                                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                     @RequestParam(value = "endDate", required = false)
                                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        validateRangeDates(startDate, endDate);
        return ResponseEntity.ok(reservationManager.getAvailableDates(startDate, endDate));
    }

    @PostMapping("/api/campsite/reservations")
    public ResponseEntity<ReservationDTO> reserveCampsite(@RequestBody @Valid ReservationDTO reservationDTO) {
        validateReservationDates(reservationDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.map(reservationManager.reserveCampsite(mapper.map(reservationDTO))));
    }

    @PutMapping("/api/campsite/reservation/{reservationId}")
    public ResponseEntity<ReservationDTO> updateCampsiteReservation(@PathVariable String reservationId, @RequestBody @Valid ReservationDTO reservationDTO) {
        validateReservationDates(reservationDTO);
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.map(reservationManager.updateReservation(reservationId, mapper.map(reservationDTO))));
    }

    @GetMapping("/api/campsite/reservation/{reservationId}")
    public ResponseEntity<ReservationDTO> getCampsiteReservation(@PathVariable String reservationId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.map(reservationManager.getReservation(reservationId)));
    }

    @DeleteMapping("/api/campsite/reservation/{reservationId}/{userId}")
    ResponseEntity<Void> cancelReservation(@PathVariable String reservationId, @Email @PathVariable String userId) {
        reservationManager.cancelReservation(reservationId, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
