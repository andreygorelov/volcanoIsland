package com.upgrade.volcano.island.reservation;

import com.upgrade.volcano.island.reservation.rest.model.ReservationDTO;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ReservationCancellationTest extends ReservationBaseTest{

    @Test
    public void cancelReservation() throws Exception {
        // Can't cancel, reservation doesn't exists
        MvcResult mvcResult = reservationDeleteRequest("45kjlk3", "noone@domain.net", status().isBadRequest());
        assertMessageResponse("Reservation not found!", mvcResult);

        // First request is successful campsite reserved for 2 days
        ReservationDTO reservationDTO =
                ReservationDTO.builder().email("gwest@domain.net").firstName("Gander").lastName("Vandal")
                        .startDate(now.plusDays(5)).endDate(now.plusDays(6)).build();

        mvcResult = reservationsPostRequest(reservationDTO, status().isCreated());
        ReservationDTO response = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response.getReservationId());
        assertEquals("gwest@domain.net", response.getEmail());
        assertEquals(now.plusDays(5), response.getStartDate());

        // Can't delete - Valid user (email + reservationId)
        mvcResult = reservationDeleteRequest(response.getReservationId(), "noone@domain.net", status().isBadRequest());
        assertMessageResponse("Email is not associated with reservation!", mvcResult);

        // Check that 2 days are occupied
        mvcResult = availableDatesGetRequest(status().isOk());
        List<LocalDate> listOfAvailableDates = getListOfDatesFromResponse(mvcResult);
        assertEquals(28, listOfAvailableDates.size());

        // Cancel reservation
        reservationDeleteRequest(response.getReservationId(), response.getEmail(), status().isOk());

        // Check that there are now all 30 days available
        mvcResult = availableDatesGetRequest(status().isOk());
        listOfAvailableDates = getListOfDatesFromResponse(mvcResult);
        assertEquals(30, listOfAvailableDates.size());
    }
}
