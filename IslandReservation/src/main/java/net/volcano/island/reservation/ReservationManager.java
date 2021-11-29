package net.volcano.island.reservation;

import lombok.extern.slf4j.Slf4j;
import net.volcano.island.reservation.configuration.ReservationProperties;
import net.volcano.island.reservation.exception.AuthenticationException;
import net.volcano.island.reservation.exception.ReservationNotFoundException;
import net.volcano.island.reservation.exception.ValidationException;
import net.volcano.island.reservation.model.ReservationBO;
import net.volcano.island.reservation.util.ReservationUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.util.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
@Service
public class ReservationManager {

    private Map<LocalDate, String> daysToReservationId = new HashMap<>();
    private Map<String, ReservationBO> reservationLookupMap = new HashMap<>();

    private final ReservationUtil reservationUtil;

    @Autowired
    public ReservationManager(ReservationProperties properties, ReservationUtil reservationUtil) {
        this.reservationUtil = reservationUtil;
        if (properties.isRestoreBackup()) {
            // Restore reservations from files
            restoreReservations();
        }
        if (daysToReservationId.isEmpty()) {
            LocalDate now = LocalDate.now();
            IntStream.rangeClosed(1, 30).forEach(i -> daysToReservationId.put(now.plusDays(i), null));
        }
    }

    /**
     * @return The list of available dates.
     */
    synchronized public List<LocalDate> getAvailableDates() {
        log.info("Attempting to retrieve all available dates.");
        return daysToReservationId.entrySet().stream()
                .filter(entry -> entry.getValue() == null)
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    /**
     * Reserving campsite if dates are available.
     *
     * @param reservationBO
     * @return The ReservationBO
     */
    synchronized public ReservationBO reserveCampsite(ReservationBO reservationBO) {
        return upsertReservation(reservationBO, null);
    }

    /**
     * Update existing reservation by removing existing and adding updated reservation.
     * If updated resrvation cannot be updated it will be re-added.
     *
     * @param newReservationBO
     * @return The ReservationBO
     */
    synchronized public ReservationBO updateReservation(String reservationId, ReservationBO newReservationBO) {
        ReservationBO oldReservationBO = reservationLookupMap.get(reservationId);
        if (oldReservationBO == null) {
            throw new ReservationNotFoundException("Reservation not found!");
        }
        // Check if reservation email matches / simple authentication
        if (!newReservationBO.getEmail().equals(oldReservationBO.getEmail())) {
            throw new AuthenticationException("Email doesn't match reservation registration email!");
        }
        // New reservation will have new reservationId
        newReservationBO.setReservationId(null);
        return upsertReservation(newReservationBO, oldReservationBO);
    }

    /**
     * Helper method to perform reservation additions and updates
     *
     * @param newReservationBO
     * @param oldReservationBO
     * @return The ReservationBO
     */
    private ReservationBO upsertReservation(ReservationBO newReservationBO, ReservationBO oldReservationBO) {
        LocalDate startDate = newReservationBO.getStartDate();
        LocalDate endDate = newReservationBO.getEndDate();

        // Cancel old reservation if it's update, so we can reuse any dates in old reservation
        if (oldReservationBO != null) {
            cancelReservation(oldReservationBO.getReservationId(), oldReservationBO.getEmail());
        }

        // Check if requested dates are available
        for (LocalDate date = startDate; date.isBefore(endDate.plusDays(1)); date = date.plusDays(1)) {
            if (daysToReservationId.get(date) != null) {
                if (oldReservationBO != null) {
                    // re-add old reservation
                    addReservation(oldReservationBO);
                }
                throw new ValidationException("The campsite for requested dates is occupied!");
            }
        }
        return addReservation(newReservationBO);
    }

    private ReservationBO addReservation(ReservationBO reservationBO) {
        // all good let's reserve
        String reservationId = reservationBO.getReservationId() != null ? reservationBO.getReservationId() : RandomStringUtils.randomAlphanumeric(8);
        for (LocalDate date = reservationBO.getStartDate(); date.isBefore(reservationBO.getEndDate().plusDays(1)); date = date.plusDays(1)) {
            daysToReservationId.put(date, reservationId);
        }
        reservationBO.setReservationId(reservationId);
        reservationLookupMap.put(reservationId, reservationBO);
        return reservationBO;
    }


    /**
     * Cancel reservation by removing days and reference for the resrvationId
     * Throws not found and email mismatch exceptions.
     *
     * @param reservationId
     * @param email
     */
    synchronized public void cancelReservation(String reservationId, String email) {
        log.info("Attempting to cancel reservation: {}", reservationId);
        ReservationBO reservationBO = reservationLookupMap.get(reservationId);
        if (reservationBO == null) {
            throw new ValidationException("Reservation not found!");
        }
        if (!email.equals(reservationBO.getEmail())) {
            throw new ValidationException("Email is not associated with reservation!");
        }

        // Remove reservation days and reservation reference from lookup map
        LocalDate startDate = reservationBO.getStartDate();
        LocalDate endDate = reservationBO.getEndDate();
        for (LocalDate date = startDate; date.isBefore(endDate.plusDays(1)); date = date.plusDays(1)) {
            daysToReservationId.put(date, null);
        }
        reservationLookupMap.remove(reservationId);
    }

    /**
     * Get existing reservation by reservationId
     *
     * @param reservationId
     * @return The reservation BO
     */
    public ReservationBO getReservation(String reservationId) {
        ReservationBO reservationBO = reservationLookupMap.get(reservationId);
        if (reservationBO == null) {
            throw new ReservationNotFoundException("We didn't find reservation with reservationId: " + reservationId);
        }
        return reservationBO;
    }

    /**
     * Runs cron job every day at midnight to remove-replace at least yesterday day and expired reservation.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    synchronized public void purgePassedReservations() {
        log.info("Attempting to remove expired reservations.");
        printMaps();
        // Including now
        LocalDate expiredDate = LocalDate.now();
        Map<LocalDate, String> daysToReservationIdReplacementMap = new HashMap<>();
        daysToReservationId.forEach((aDate, id) -> {
            if (aDate.isAfter(expiredDate)) {
                daysToReservationIdReplacementMap.put(aDate, id);
            } else {
                ReservationBO reservationBO = reservationLookupMap.get(id);
                // reservation is part of expired date remove reservation lookup reference
                if (reservationBO != null && reservationBO.getEndDate().isBefore(expiredDate)) {
                    reservationLookupMap.remove(id);
                }
            }
        });
        // Replace old map
        daysToReservationId = daysToReservationIdReplacementMap;
        // Add dates up to a month
        if (daysToReservationId.size() < 30) {
            int currSize = daysToReservationId.size();
            int target = currSize + 30;
            LocalDate now = LocalDate.now();
            IntStream.rangeClosed(30, target).forEach(i -> daysToReservationId.put(now.plusDays(i), null));
        }
    }

    /**
     * Runs cron job every hour to back up existing reservations.
     */
    @Scheduled(cron = "0 0 0/1 * * ?")
    public void backupReservations() {
        log.info("Attempting to backup reservations.");
        reservationUtil.backupReservations(daysToReservationId, reservationLookupMap);
    }

    @VisibleForTesting
    public void restoreReservations() {
        daysToReservationId = reservationUtil.restoreDaysToReservationIdMap();
        reservationLookupMap = reservationUtil.restoreReservationLookupMap();
        // Remove reservations in case it's been too long
        purgePassedReservations();
    }

    @VisibleForTesting
    public ReservationBO addTestData(ReservationBO reservationBO) {
        return addReservation(reservationBO);
    }

    @VisibleForTesting
    public void printMaps() {
        System.out.println("-----------------------------------------");
        System.out.println(daysToReservationId);
        System.out.println(reservationLookupMap);
        System.out.println("-----------------------------------------");
    }
}
