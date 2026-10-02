package com.ibrahimhassen.barber_booking_agent.calendar;

import jakarta.persistence.*;

@Entity
@Table(name = "google_credentials")
public class GoogleCredential {

    @Id
    private Long id;

    @Column(nullable = false, length = 2048)
    private String refreshToken;

    public GoogleCredential() {}

    public GoogleCredential(Long id, String refreshToken) {
        this.id = id;
        this.refreshToken = refreshToken;
    }

    public Long getId() {
        return id;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
