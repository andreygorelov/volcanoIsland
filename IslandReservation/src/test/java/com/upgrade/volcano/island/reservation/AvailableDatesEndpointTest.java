package com.upgrade.volcano.island.reservation;

import com.upgrade.volcano.island.reservation.rest.BaseController;
import com.upgrade.volcano.island.reservation.rest.model.ReservationDTO;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AvailableDatesEndpointTest extends ReservationBaseTest {
    @Test
    public void getAllAvailableDates() throws Exception {
        MvcResult mvcResult = availableDatesGetRequest(status().isOk());
        List<LocalDate> listOfAvailableDates = getListOfDatesFromResponse(mvcResult);
        assertEquals(30, listOfAvailableDates.size());
        // check first date starts tomorrow
        assertTrue(listOfAvailableDates.contains(now.plusDays(1)));
        // check last date
        assertTrue(listOfAvailableDates.contains(now.plusDays(30)));
    }

    @Test
    public void getAvailableDatesFromStartDate() throws Exception {
        MvcResult mvcResult = availableDatesRangeGetRequest(now.plusDays(15).toString(), null, status().isOk());
        List<LocalDate> listOfAvailableDates = getListOfDatesFromResponse(mvcResult);
        // 16 days including now + 15th day
        assertEquals(16, listOfAvailableDates.size());
        // now + 14th not included
        assertFalse(listOfAvailableDates.contains(now.plusDays(14)));
        // check first date starts tomorrow
        assertTrue(listOfAvailableDates.contains(now.plusDays(15)));
        // check last date
        assertTrue(listOfAvailableDates.contains(now.plusDays(30)));
    }

    @Test
    public void getAvailableDatesToEndDate() throws Exception {
        // up to now + 15
        MvcResult mvcResult = availableDatesRangeGetRequest(null, now.plusDays(15).toString(), status().isOk());
        List<LocalDate> listOfAvailableDates = getListOfDatesFromResponse(mvcResult);
        // 16 days including now + 15th day
        assertEquals(15, listOfAvailableDates.size());
        // check first date starts tomorrow
        assertTrue(listOfAvailableDates.contains(now.plusDays(1)));
        // check last date
        assertTrue(listOfAvailableDates.contains(now.plusDays(15)));
        // no dates provided passed now + 15
        assertFalse(listOfAvailableDates.contains(now.plusDays(16)));
    }

    @Test
    public void getAvailableDateInRange() throws Exception {
        // 10th to 15th (10, 11, 12, 13, 14, 15) 6 days
        MvcResult mvcResult = availableDatesRangeGetRequest(now.plusDays(10).toString(), now.plusDays(15).toString(), status().isOk());
        List<LocalDate> listOfAvailableDates = getListOfDatesFromResponse(mvcResult);
        // 16 days including now + 15th day
        assertEquals(6, listOfAvailableDates.size());
        // no dates before now + 10
        assertFalse(listOfAvailableDates.contains(now.plusDays(9)));
        // check first date starts tomorrow
        assertTrue(listOfAvailableDates.contains(now.plusDays(10)));
        // check last date
        assertTrue(listOfAvailableDates.contains(now.plusDays(15)));
        // no dates passed now + 16
        assertFalse(listOfAvailableDates.contains(now.plusDays(16)));
    }

    @Test
    public void getAvailableDatesInRangeWithGaps() throws Exception {
        // --- Add two reservations ----
        ReservationDTO reservationDTO =
                ReservationDTO.builder().email("pgtest@domain.net").firstName("Penny").lastName("Gorges")
                        .startDate(now.plusDays(15)).endDate(now.plusDays(17)).build();

        MvcResult mvcResult = reservationsPostRequest(reservationDTO, status().isCreated());
        ReservationDTO response1 = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response1.getReservationId());
        assertEquals("pgtest@domain.net", response1.getEmail());
        assertEquals(now.plusDays(15), response1.getStartDate());
        assertEquals(now.plusDays(17), response1.getEndDate());

        reservationDTO =
                ReservationDTO.builder().email("vctest@domain.net").firstName("Vlake").lastName("Chelton")
                        .startDate(now.plusDays(19)).endDate(now.plusDays(21)).build();

        mvcResult = reservationsPostRequest(reservationDTO, status().isCreated());
        ReservationDTO response2 = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response2.getReservationId());
        assertEquals("vctest@domain.net", response2.getEmail());
        assertEquals(now.plusDays(19), response2.getStartDate());
        assertEquals(now.plusDays(21), response2.getEndDate());

        // Get available dates from now + 15 to now + 12 (should be only one day available now = 18)
        mvcResult = availableDatesRangeGetRequest(now.plusDays(15).toString(), now.plusDays(21).toString(), status().isOk());
        List<LocalDate> listOfAvailableDates = getListOfDatesFromResponse(mvcResult);
        // 16 days including now + 15th day
        assertEquals(1, listOfAvailableDates.size());
        // Only one date
        assertTrue(listOfAvailableDates.contains(now.plusDays(18)));

        reservationDeleteRequest(response1.getReservationId(), response1.getEmail(), status().isOk());
        reservationDeleteRequest(response2.getReservationId(), response2.getEmail(), status().isOk());
    }
}
