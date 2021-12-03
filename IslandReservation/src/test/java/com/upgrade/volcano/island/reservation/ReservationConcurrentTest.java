package com.upgrade.volcano.island.reservation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.upgrade.volcano.island.reservation.exception.ValidationException;
import com.upgrade.volcano.island.reservation.model.ReservationBO;
import com.upgrade.volcano.island.reservation.rest.model.ReservationDTO;
import lombok.Data;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ReservationConcurrentTest extends ReservationBaseTest {

    /*
     * 10 threads trying to add the same reservation, one succeeds 9 fails
     */
    @Test
    public void testConcurrencyAddReservation() throws Exception {
        int numberOfThreads = 10;
        ExecutorService service = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ExceptionCounter counter = new ExceptionCounter();
        AtomicReference<ReservationDTO> reservationSuccess = new AtomicReference<ReservationDTO>();

        for (int i = 0; i < numberOfThreads; i++) {
            service.submit(() -> {
                try {
                    String random = RandomStringUtils.randomAlphabetic(5);
                    ReservationDTO reservationDTO =
                            ReservationDTO.builder().email(random + "@domain.net").firstName(random).lastName(random)
                                    .startDate(now.plusDays(10)).endDate(now.plusDays(12)).build();
                    reservationSuccess.set(reservationMapper.map(reservationManager.reserveCampsite(reservationMapper.map(reservationDTO))));

                    // Let's try fetch available dates for every thread
                    List<LocalDate> listOfAvailableDates = getListOfDatesFromResponse(availableDatesGetRequest(status().isOk()));
                    // We should have 27 more available days
                    assertEquals(27, listOfAvailableDates.size());

                } catch (ValidationException e) {
                    if (e.getMessage().equals("The campsite for requested dates is occupied!")) {
                        counter.increment();
                    }
                } catch (Exception e) {
                    // verify that there were no other exceptions
                    counter.decrement();
                    e.printStackTrace();
                }
                latch.countDown();
            });
        }
        latch.await();
        // We should have 9 failed and one success
        Assert.assertEquals(9, counter.getCount());
        assertNotNull(reservationSuccess.get().getReservationId());
        assertEquals(now.plusDays(10), reservationSuccess.get().getStartDate());
        reservationDeleteRequest(reservationSuccess.get().getReservationId(), reservationSuccess.get().getEmail(), status().isOk());
    }

    @Data
    public class ExceptionCounter {
        private int count;

        public void increment() {
            int temp = count;
            count = temp + 1;
        }
        public void decrement() {
            int temp = count;
            count = temp - 1;
        }
    }
}
