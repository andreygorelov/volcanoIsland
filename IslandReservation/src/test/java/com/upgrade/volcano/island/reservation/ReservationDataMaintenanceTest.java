package com.upgrade.volcano.island.reservation;

import com.upgrade.volcano.island.reservation.rest.model.ReservationDTO;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for reservation data backing up, restore, and remove tasks
 */
public class ReservationDataMaintenanceTest extends ReservationBaseTest {

    @Test
    public void backupAndRestoreReservations() throws Exception {
        // --- Add two reservations ----
        ReservationDTO reservationDTO =
                ReservationDTO.builder().email("buest@domain.net").firstName("Buck").lastName("Up")
                        .startDate(now.plusDays(15)).endDate(now.plusDays(17)).build();

        MvcResult mvcResult = reservationsPostRequest(reservationDTO, status().isCreated());
        ReservationDTO response1 = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response1.getReservationId());
        assertEquals("buest@domain.net", response1.getEmail());
        assertEquals(now.plusDays(15), response1.getStartDate());

        reservationDTO =
                ReservationDTO.builder().email("rtest@domain.net").firstName("Res").lastName("Tor")
                        .startDate(now.plusDays(3)).endDate(now.plusDays(5)).build();

        mvcResult = reservationsPostRequest(reservationDTO, status().isCreated());
        ReservationDTO response2 = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response2.getReservationId());
        assertEquals("rtest@domain.net", response2.getEmail());
        assertEquals(now.plusDays(3), response2.getStartDate());

        // --- Back up all reservations ---
        reservationManager.backupReservations();

        // Delete both reservations / outage / service crash
        reservationDeleteRequest(response1.getReservationId(), response1.getEmail(), status().isOk());
        reservationDeleteRequest(response2.getReservationId(), response2.getEmail(), status().isOk());

        // Check both reservations cannot be found
        mvcResult = reservationGetRequest(response1.getReservationId(), status().isNotFound());
        assertMessageResponse("We didn't find reservation with reservationId: " + response1.getReservationId(), mvcResult);
        mvcResult = reservationGetRequest(response2.getReservationId(), status().isNotFound());
        assertMessageResponse("We didn't find reservation with reservationId: " + response2.getReservationId(), mvcResult);

        // Check no reservations left all 30 days are available
        mvcResult = availableDatesGetRequest(status().isOk());
        List<LocalDate> listOfAvailableDates = getListOfDatesFromResponse(mvcResult);
        assertEquals(30, listOfAvailableDates.size());

        // --- Restore reservations from backup ---
        reservationManager.restoreReservations();

        // Reservation should be restored and there are 20 days available days left
        mvcResult = availableDatesGetRequest(status().isOk());
        listOfAvailableDates = getListOfDatesFromResponse(mvcResult);
        assertEquals(24, listOfAvailableDates.size());

        // Both reservations should be present
        reservationGetRequest(response1.getReservationId(), status().isOk());
        reservationGetRequest(response2.getReservationId(), status().isOk());

        // clean up
        reservationDeleteRequest(response1.getReservationId(), response1.getEmail(), status().isOk());
        reservationDeleteRequest(response2.getReservationId(), response2.getEmail(), status().isOk());
    }

    @Test
    public void testExpiredDaysAndReservationsRemoval() throws Exception {
        // --- Reservation tomorrow (first day)
        ReservationDTO reservationDTO =
                ReservationDTO.builder().email("buest@domain.net").firstName("Bill").lastName("Dolins")
                        .startDate(now.plusDays(1)).endDate(now.plusDays(1)).build();

        MvcResult mvcResult = reservationsPostRequest(reservationDTO, status().isCreated());
        ReservationDTO response1 = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response1.getReservationId());
        assertEquals("buest@domain.net", response1.getEmail());
        assertEquals(now.plusDays(1), response1.getStartDate());

        // Reservation on the last day with in a month
        reservationDTO =
                ReservationDTO.builder().email("rtest@domain.net").firstName("Ron").lastName("Chovi")
                        .startDate(now.plusDays(30)).endDate(now.plusDays(30)).build();

        mvcResult = reservationsPostRequest(reservationDTO, status().isCreated());
        ReservationDTO response2 = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response2.getReservationId());
        assertEquals("rtest@domain.net", response2.getEmail());
        assertEquals(now.plusDays(30), response2.getStartDate());

        mvcResult = availableDatesGetRequest(status().isOk());
        List<LocalDate> listOfAvailableDates = getListOfDatesFromResponse(mvcResult);
        assertEquals(28, listOfAvailableDates.size());

        // Add expired reservations that only happen after: a day passed
        // - a day passed
        // - service crushed and restarted a day or more later and reservation restored from backup
        Map<String, String> map = new HashMap<>();
        for (LocalDate date = now.minusDays(1); date.isAfter(now.minusDays(6)); date = date.minusDays(1)) {
            reservationDTO =
                    ReservationDTO.builder().email(date + "test@domain.net").firstName(date + "fName").lastName(date + "lName")
                            .startDate(date).endDate(date).build();
            reservationDTO = reservationMapper.map(reservationManager.addTestData(reservationMapper.map(reservationDTO)));
            map.put(reservationDTO.getReservationId(), reservationDTO.getEmail());
        }

        // Verify we have old reservation/dates in our storage
        map.forEach((key, value) -> {
            try {
                ReservationDTO tempDTO = getReservationDTOFromResponse(reservationGetRequest(key, status().isOk()));
                assertEquals(key, tempDTO.getReservationId());
                assertEquals(value, tempDTO.getEmail());
                assertTrue(tempDTO.getStartDate().isBefore(now));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Cron job runs every day at midnight, however will run it via calling a method
        reservationManager.purgePassedReservations();

        // First 2 non expired reservation are still retrievable
        ReservationDTO response11 = getReservationDTOFromResponse(reservationGetRequest(response1.getReservationId(), status().isOk()));
        assertEquals(response1.getReservationId(), response11.getReservationId());
        assertEquals(response1.getEmail(), response11.getEmail());
        assertEquals(response1.getStartDate(), response11.getStartDate());

        ReservationDTO response22 = getReservationDTOFromResponse(reservationGetRequest(response2.getReservationId(), status().isOk()));
        assertEquals(response2.getReservationId(), response22.getReservationId());
        assertEquals(response2.getEmail(), response22.getEmail());
        assertEquals(response2.getStartDate(), response22.getStartDate());

        // Check no expired dates/reservations left
        map.forEach((key, value) -> {
            try {
                getReservationDTOFromResponse(reservationGetRequest(key, status().isNotFound()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // clean up
        reservationDeleteRequest(response1.getReservationId(), response1.getEmail(), status().isOk());
        reservationDeleteRequest(response2.getReservationId(), response2.getEmail(), status().isOk());
    }
}
