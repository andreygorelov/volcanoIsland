package net.volcano.island.reservation.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Email;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ReservationDTO implements Serializable {
    private String reservationId;
    @NonNull
    private String firstName;
    @NonNull
    private String lastName;
    @Email
    private String email;
    @DateTimeFormat(pattern="yyyy-MM-dd")
    private LocalDate startDate;
    @DateTimeFormat(pattern="yyyy-MM-dd")
    private LocalDate endDate;
}