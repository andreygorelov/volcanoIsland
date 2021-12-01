package com.upgrade.volcano.island.reservation.rest.model.mapper;

import com.upgrade.volcano.island.reservation.model.ReservationBO;
import com.upgrade.volcano.island.reservation.rest.model.ReservationDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    ReservationDTO map(ReservationBO source);

    ReservationBO map(ReservationDTO source);
}
