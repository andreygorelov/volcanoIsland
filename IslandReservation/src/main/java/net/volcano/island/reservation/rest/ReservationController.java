package net.volcano.island.reservation.rest;

import lombok.extern.slf4j.Slf4j;
import net.volcano.island.reservation.ReservationManager;
import net.volcano.island.reservation.rest.model.ReservationDTO;
import net.volcano.island.reservation.rest.model.mapper.ReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Email;
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
    public ResponseEntity<List<LocalDate>> getAvailableCampsiteDates() {
        System.currentTimeMillis();
        log.info("Retrieving available dates");
        return ResponseEntity.status(HttpStatus.OK).body(reservationManager.getAvailableDates());
    }

    @PostMapping( "/api/campsite/reservations")
    public ResponseEntity<ReservationDTO> reserveCampsite(@RequestBody @Valid ReservationDTO reservationDTO) {
       validateReservationDates(reservationDTO);
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.map(reservationManager.reserveCampsite(mapper.map(reservationDTO))));
    }

    @PutMapping("/api/campsite/reservation/{reservationId}")
    public ResponseEntity<ReservationDTO> updateCampsiteReservation(@PathVariable String reservationId, @RequestBody @Valid ReservationDTO reservationDTO) {
        validateReservationDates(reservationDTO);
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.map(reservationManager.updateReservation(reservationId, mapper.map(reservationDTO))));
    }

    @GetMapping("/api/campsite/reservation/{reservationId}")
    public ResponseEntity<?> getCampsiteReservation(@PathVariable String reservationId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.map(reservationManager.getReservation(reservationId)));
    }

    @DeleteMapping("/api/campsite/reservation/{reservationId}/{email}")
    ResponseEntity<Void> cancelReservation(@PathVariable String reservationId, @Email @PathVariable String email) {
        reservationManager.cancelReservation(reservationId, email);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
