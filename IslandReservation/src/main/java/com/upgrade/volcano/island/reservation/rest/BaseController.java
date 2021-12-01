package com.upgrade.volcano.island.reservation.rest;

import com.upgrade.volcano.island.reservation.rest.model.ReservationDTO;
import com.upgrade.volcano.island.reservation.exception.AuthenticationException;
import com.upgrade.volcano.island.reservation.exception.ReservationNotFoundException;
import com.upgrade.volcano.island.reservation.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public abstract class BaseController {
    @ExceptionHandler({ValidationException.class, ReservationNotFoundException.class, AuthenticationException.class})
    @ResponseBody
    public ResponseEntity<String> handleExecutionException(Exception ex) {
        if (ex instanceof ValidationException) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } else if (ex instanceof ReservationNotFoundException) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        } else if (ex instanceof AuthenticationException) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    protected void validateReservationDates(ReservationDTO reservationDTO) {
        validateReservationDates(reservationDTO.getStartDate(), reservationDTO.getEndDate());
    }

    protected void validateReservationDates(LocalDate startDate, LocalDate endDate) {
        String errMsg = null;
        LocalDate now = LocalDate.now();
        // Number of days between including start and end days
        long numDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        // Start date must be before end date
        if (startDate.isAfter(endDate)) {
            errMsg = "Reservation start date must be before end date!";
        }
        // Start date must be after today's date, no need to check end date since we checked that it is after start date
        else if (startDate.isBefore(now.plusDays(1))) {
            errMsg = "Reservation cannot start on the same day or in the past!";
        }
        // Number of days cannot exceed 3 days
        else if (numDays > 3) {
            errMsg = "Reservation cannot exceed 3 days!";
        }
        // Date range must be within next 30 days
        else if (startDate.isAfter(now.plusDays(30)) || endDate.isAfter(now.plusDays(30))) {
            errMsg = "Reservation allowed up to a month in advance!";
        }
        if (errMsg != null) {
            throw new ValidationException(errMsg);
        }
    }

    protected void validateRangeDates(LocalDate startDate, LocalDate endDate) {
        String errMsg = null;
        if(startDate != null && endDate != null){
            // Start date must be before end date
            if (startDate.isAfter(endDate)) {
                errMsg = "Range: start date must be before end date!";
            }
            // Validation can be extended
            if (errMsg != null) {
                throw new ValidationException(errMsg);
            }
        }
    }
}
