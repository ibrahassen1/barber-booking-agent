package com.ibrahimhassen.barber_booking_agent.calendar;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleCredentialRepository
        extends JpaRepository<GoogleCredential, Long> {
}
