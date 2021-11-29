package net.volcano.island.reservation;

import net.volcano.island.reservation.rest.model.ReservationDTO;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ReservationAddTest extends ReservationBaseTest {

    @Test
    public void cantReserveOnTheSameDay() throws Exception {
        ReservationDTO reservationDTO =
                ReservationDTO.builder().email("dtest@domain.net").firstName("Dian").lastName("Rose")
                        .startDate(now).endDate(now.plusDays(1)).build();
        MvcResult mvcResult = reservationsPostRequest(reservationDTO, status().isBadRequest());
        assertMessageResponse("Reservation cannot start on the same day or in the past!", mvcResult);
    }

    @Test
    public void cantReservePastThirtyDays() throws Exception {
        ReservationDTO reservationDTO =
                ReservationDTO.builder().email("ptest@domain.net").firstName("Patty").lastName("Barry")
                        .startDate(now.plusDays(29)).endDate(now.plusDays(31)).build();
        MvcResult mvcResult = reservationsPostRequest(reservationDTO, status().isBadRequest());
        assertMessageResponse("Reservation allowed up to a month in advance!", mvcResult);
    }

    @Test
    public void cantReserveMoreThanThreeDays() throws Exception {
        ReservationDTO reservationDTO =
                ReservationDTO.builder().email("ltest@domain.net").firstName("Lana").lastName("Nontana")
                        .startDate(now.plusDays(10)).endDate(now.plusDays(14)).build();
        MvcResult mvcResult = reservationsPostRequest(reservationDTO, status().isBadRequest());
        assertMessageResponse("Reservation cannot exceed 3 days!", mvcResult);
    }

    @Test
    public void cantReserveEndDateBeforeStartDate() throws Exception {
        ReservationDTO reservationDTO =
                ReservationDTO.builder().email("batest@domain.net").firstName("Bony").lastName("Am")
                        .startDate(now.plusDays(10)).endDate(now.plusDays(9)).build();
        MvcResult mvcResult = reservationsPostRequest(reservationDTO, status().isBadRequest());
        assertMessageResponse("Reservation start date must be before end date!", mvcResult);
    }

    @Test
    public void reserveNextOneDay() throws Exception {
        ReservationDTO reservationDTO =
                ReservationDTO.builder().email("jwtest@domain.net").firstName("Jason").lastName("Wiever")
                        .startDate(now.plusDays(1)).endDate(now.plusDays(1)).build();
        MvcResult mvcResult = reservationsPostRequest(reservationDTO, status().isOk());
        ReservationDTO response = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response.getReservationId());
        assertEquals("jwtest@domain.net", response.getEmail());
        assertEquals(now.plusDays(1), response.getStartDate());
        // clean up cancel reservation
        reservationDeleteRequest(response.getReservationId(), response.getEmail(), status().isOk());
    }

    @Test
    public void reserveIncludingLastDay() throws Exception {
        ReservationDTO reservationDTO =
                ReservationDTO.builder().email("jtest@domain.net").firstName("Jason").lastName("Wiever")
                        .startDate(now.plusDays(29)).endDate(now.plusDays(30)).build();

        MvcResult mvcResult = reservationsPostRequest(reservationDTO, status().isOk());
        ReservationDTO response = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response.getReservationId());
        assertEquals("jtest@domain.net", response.getEmail());
        assertEquals(now.plusDays(29), response.getStartDate());

        // clean up cancel reservation
        reservationDeleteRequest(response.getReservationId(), response.getEmail(), status().isOk());
    }

    @Test
    public void cannotReserveOccupiedDays() throws Exception {
        // First request is successful campsite reserved for 3 days
        ReservationDTO reservationDTO =
                ReservationDTO.builder().email("jtest@domain.net").firstName("Jason").lastName("Wiever")
                        .startDate(now.plusDays(10)).endDate(now.plusDays(12)).build();

        MvcResult mvcResult = reservationsPostRequest(reservationDTO, status().isOk());
        ReservationDTO response = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response.getReservationId());
        assertEquals("jtest@domain.net", response.getEmail());
        assertEquals(now.plusDays(10), response.getStartDate());

        // now.plusDays(10)th day is occupied
        reservationDTO = ReservationDTO.builder().email("jtest@domain.net").firstName("Marsha").lastName("Feward")
                .startDate(now.plusDays(8)).endDate(now.plusDays(10)).build();
        mvcResult = reservationsPostRequest(reservationDTO, status().isBadRequest());
        assertMessageResponse("The campsite for requested dates is occupied!", mvcResult);

        // now.plusDays(11)th day is between 10th and 12th
        reservationDTO = ReservationDTO.builder().email("jtest@domain.net").firstName("Karry").lastName("Water")
                .startDate(now.plusDays(11)).endDate(now.plusDays(11)).build();
        mvcResult = reservationsPostRequest(reservationDTO, status().isBadRequest());
        assertMessageResponse("The campsite for requested dates is occupied!", mvcResult);

        // now.plusDays(12)th day is on the last day of the original reservation
        reservationDTO = ReservationDTO.builder().email("jtest@domain.net").firstName("Hanna").lastName("Darson")
                .startDate(now.plusDays(12)).endDate(now.plusDays(14)).build();
        mvcResult = reservationsPostRequest(reservationDTO, status().isBadRequest());
        assertMessageResponse("The campsite for requested dates is occupied!", mvcResult);

        // clean up cancel reservation
        reservationDeleteRequest(response.getReservationId(), response.getEmail(), status().isOk());
    }

    @Test
    public void reserveOneDayPerUserForTheWholeMonth() throws Exception {

        // Check initially all 30 days are available
        MvcResult mvcResult = reservationsGetRequest(status().isOk());
        List<LocalDate> listOfAvailableDates = getListOfDatesFromResponse(mvcResult);
        assertEquals(30, listOfAvailableDates.size());

        Map<String, String> reservations = new HashMap<>();
        // Populate 1 day per new reservation
        IntStream.rangeClosed(1, 30).forEach(i -> {
            ReservationDTO reservationDTO =
                    ReservationDTO.builder().email(i + "test@domain.net").firstName(i + "fname").lastName(i + "lname")
                            .startDate(now.plusDays(i)).endDate(now.plusDays(i)).build();
            try {
                MvcResult mvcResult2 = reservationsPostRequest(reservationDTO, status().isOk());
                ReservationDTO response = getReservationDTOFromResponse(mvcResult2);
                assertNotNull(response.getReservationId());
                assertEquals(i + "test@domain.net", response.getEmail());
                assertEquals(now.plusDays(i), response.getStartDate());
                reservations.put(response.getReservationId(), response.getEmail());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Check no available days left
        mvcResult = reservationsGetRequest(status().isOk());
        listOfAvailableDates = getListOfDatesFromResponse(mvcResult);
        assertEquals(0, listOfAvailableDates.size());

        // Cleanup
        reservations.forEach((id, email) -> {
            try {
                reservationDeleteRequest(id, email, status().isOk());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Check all 30 days are available
        mvcResult = reservationsGetRequest(status().isOk());
        listOfAvailableDates = getListOfDatesFromResponse(mvcResult);
        assertEquals(30, listOfAvailableDates.size());
    }
}
