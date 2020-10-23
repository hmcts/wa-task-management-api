package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class SystemDateProvider {

    public LocalDate now() {
        return LocalDate.now();
    }

    public LocalDateTime nowWithTime() {
        return LocalDateTime.now();
    }

    public Timestamp getTimestamp() {
        return Timestamp.valueOf(LocalDateTime.now());
    }
}

