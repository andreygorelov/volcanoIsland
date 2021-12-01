package com.upgrade.volcano.island.reservation.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "campsite.reservation")
public class ReservationProperties {
    private boolean restoreBackup;
    private boolean backup;
}