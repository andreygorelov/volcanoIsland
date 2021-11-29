package net.volcano.island.reservation.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.volcano.island.reservation.model.ReservationBO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class ReservationUtil {

    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
    public final static String daysToReservationIdFile = "daysToReservationIdFile.txt";
    public final static String reservationLookupMapFile = "reservationLookupMapFile.txt";

    public ReservationUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Backup Reservations to file in case of service crashes
     */
    public void backupReservations(Map<LocalDate, String> daysToReservationId, Map<String, ReservationBO> reservationLookupMap) {
        if (daysToReservationId != null && !daysToReservationId.isEmpty()) {
            writeToFile(daysToReservationIdFile, daysToReservationId);
            writeToFile(reservationLookupMapFile, reservationLookupMap);
        }
    }

    /**
     * Write a generic map to a file key:value format
     *
     * @param fileName
     * @param map
     */
    private void writeToFile(String fileName, Map<?, ?> map) {
        File file = new File(fileName);
        try (BufferedWriter bf = new BufferedWriter(new FileWriter(file))) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String strJson = entry.getValue() != null && !(entry.getValue() instanceof String) ?
                        objectMapper.writeValueAsString(entry.getValue()) : (String) entry.getValue();
                bf.write(entry.getKey() + "::" + strJson);
                bf.newLine();
            }
            bf.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Restore map from back up file that must be key:value format
     *
     * @return Map<String, ReservationBO>
     */
    public Map<String, ReservationBO> restoreReservationLookupMap() {
        Map<String, ReservationBO> map = new HashMap<>();
        File file = new File(reservationLookupMapFile);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] entry = line.split("::");
                map.put(entry[0], entry[1] == null ? null : objectMapper.readValue(entry[1], ReservationBO.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Restore map from back up file that must be key:value format
     *
     * @return Map<LocalDate, String>
     */
    public Map<LocalDate, String> restoreDaysToReservationIdMap() {
        Map<LocalDate, String> map = new HashMap<>();
        File file = new File(daysToReservationIdFile);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] entry = line.split("::");
                map.put(LocalDate.parse(entry[0], DATE_FORMATTER), entry[1].equals("null") ? null : entry[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}
