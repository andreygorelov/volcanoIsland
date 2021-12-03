package com.upgrade.volcano.island.reservation;

import com.upgrade.volcano.island.reservation.rest.model.ReservationDTO;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ReservationUpdateTest extends ReservationBaseTest {

    @Test
    public void cannotChangeReservationOnOccupiedDays() throws Exception {
        // User one registers for now.plusDays(2) to now.plusDays(4) - success
        ReservationDTO reservationDTO1 =
                ReservationDTO.builder().email("lmtest@domain.net").firstName("Lally").lastName("Vinok")
                        .startDate(now.plusDays(2)).endDate(now.plusDays(4)).build();

        MvcResult mvcResult = reservationsPostRequest(reservationDTO1, status().isCreated());
        ReservationDTO response1 = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response1.getReservationId());
        assertEquals("lmtest@domain.net", response1.getEmail());
        assertEquals(now.plusDays(2), response1.getStartDate());

        // User two registers for now.plusDays(5) to now.plusDays(7) - success
        ReservationDTO reservationDTO2 =
                ReservationDTO.builder().email("fntest@domain.net").firstName("Forge").lastName("Nikle")
                        .startDate(now.plusDays(5)).endDate(now.plusDays(7)).build();

        mvcResult = reservationsPostRequest(reservationDTO2, status().isCreated());
        ReservationDTO response2 = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response2.getReservationId());
        assertEquals("fntest@domain.net", response2.getEmail());
        assertEquals(now.plusDays(5), response2.getStartDate());

        // User one tries to update to now.plusDays(4) to now.plusDays(5) - fails overlapping with user two dates
        reservationDTO1.setReservationId(response1.getReservationId());
        reservationDTO1.setStartDate(now.plusDays(4));
        reservationDTO1.setEndDate(now.plusDays(5));
        mvcResult = reservationPutRequest(response1.getReservationId(), reservationDTO1, status().isBadRequest());
        assertMessageResponse("The campsite for requested dates is occupied!", mvcResult);

        // User one tries to update to now.plusDays(6) to now.plusDays(7) - fails overlapping with user two dates
        reservationDTO1.setStartDate(now.plusDays(6));
        reservationDTO1.setEndDate(now.plusDays(7));
        mvcResult = reservationPutRequest(response1.getReservationId(), reservationDTO1, status().isBadRequest());
        assertMessageResponse("The campsite for requested dates is occupied!", mvcResult);

        // User one tries to update to now.plusDays(7) to now.plusDays(8) - fails overlapping with user two dates
        reservationDTO1.setStartDate(now.plusDays(7));
        reservationDTO1.setEndDate(now.plusDays(8));
        mvcResult = reservationPutRequest(response1.getReservationId(), reservationDTO1, status().isBadRequest());
        assertMessageResponse("The campsite for requested dates is occupied!", mvcResult);

        // User one tries to update to now.plusDays(8) to now.plusDays(9) - success no overlapping
        reservationDTO1.setStartDate(now.plusDays(8));
        reservationDTO1.setEndDate(now.plusDays(9));
        mvcResult = reservationPutRequest(response1.getReservationId(), reservationDTO1, status().isOk());
        ReservationDTO response3 = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response3.getReservationId());
        assertEquals("lmtest@domain.net", response3.getEmail());
        assertEquals(now.plusDays(8), response3.getStartDate());

        // And Old reservation is automatically canceled
        mvcResult = reservationGetRequest(response1.getReservationId(), status().isNotFound());
        assertMessageResponse("We didn't find reservation with reservationId: " + response1.getReservationId(), mvcResult);

        // clean up cancel reservation
        reservationDeleteRequest(response3.getReservationId(), response1.getEmail(), status().isOk());
        reservationDeleteRequest(response2.getReservationId(), response2.getEmail(), status().isOk());
    }


    @Test
    public void cannotChangeReservationWrongEmail() throws Exception {
        // First request is successful campsite reserved for 2 days
        ReservationDTO reservationDTO =
                ReservationDTO.builder().email("mltest@domain.net").firstName("Mikki").lastName("Linaj")
                        .startDate(now.plusDays(11)).endDate(now.plusDays(12)).build();

        MvcResult mvcResult = reservationsPostRequest(reservationDTO, status().isCreated());
        ReservationDTO response = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response.getReservationId());
        assertEquals("mltest@domain.net", response.getEmail());
        assertEquals(now.plusDays(11), response.getStartDate());

        // can't update, wrong user provided
        reservationDTO.setReservationId(response.getReservationId());
        reservationDTO.setEmail("randomUser@domain.net");
        reservationDTO.setStartDate(now.plusDays(28));
        reservationDTO.setEndDate(now.plusDays(28));
        mvcResult = reservationPutRequest(response.getReservationId(), reservationDTO, status().isUnauthorized());
        assertMessageResponse("Email doesn't match reservation registration email!", mvcResult);

        // clean up cancel reservation
        reservationDeleteRequest(response.getReservationId(), response.getEmail(), status().isOk());
    }

    @Test
    public void updateReservationSuccess() throws Exception {
        // User registers for now.plusDays(3) to now.plusDays(5) - success
        ReservationDTO reservationDTO1 =
                ReservationDTO.builder().email("mdtest@domain.net").firstName("Maylor").lastName("Drift")
                        .startDate(now.plusDays(3)).endDate(now.plusDays(4)).build();

        MvcResult mvcResult = reservationsPostRequest(reservationDTO1, status().isCreated());
        ReservationDTO response1 = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response1.getReservationId());
        assertEquals("mdtest@domain.net", response1.getEmail());
        assertEquals(now.plusDays(3), response1.getStartDate());

        // User updates for now.plusDays(4) to now.plusDays(5) - success
        reservationDTO1.setReservationId(response1.getReservationId());
        reservationDTO1.setStartDate(now.plusDays(4));
        reservationDTO1.setEndDate(now.plusDays(5));
        mvcResult = reservationPutRequest(response1.getReservationId(), reservationDTO1, status().isOk());
        response1 = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response1.getReservationId());
        assertEquals("mdtest@domain.net", response1.getEmail());
        assertEquals(now.plusDays(4), response1.getStartDate());

        // User updates back to now.plusDays(3) to now.plusDays(4) - success
        reservationDTO1.setReservationId(response1.getReservationId());
        reservationDTO1.setStartDate(now.plusDays(3));
        reservationDTO1.setEndDate(now.plusDays(4));
        mvcResult = reservationPutRequest(response1.getReservationId(), reservationDTO1, status().isOk());
        response1 = getReservationDTOFromResponse(mvcResult);
        assertNotNull(response1.getReservationId());
        assertEquals("mdtest@domain.net", response1.getEmail());
        assertEquals(now.plusDays(3), response1.getStartDate());

        reservationDeleteRequest(response1.getReservationId(), response1.getEmail(), status().isOk());
    }
}
