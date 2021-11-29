package net.volcano.island.reservation.rest.model.mapper;

import net.volcano.island.reservation.rest.model.ReservationDTO;
import net.volcano.island.reservation.model.ReservationBO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    ReservationDTO map(ReservationBO source);
    ReservationBO map(ReservationDTO source);
}
