package com.renfei.booking.cinema.controller;

import com.renfei.booking.cinema.service.CinemaHall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CinemaBookingSystemTest {
    private CinemaBookingSystem system;
    private CinemaHall hallMock;

    @BeforeEach
    void setUp() {
        hallMock = mock(CinemaHall.class);
        system = Mockito.spy(new CinemaBookingSystem(null));
        // Inject mock CinemaHall if needed
    }

    @Test
    void testInitializeSystemWithValidInput() {
        String input = "Inception 10 20\n";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);
        CinemaBookingSystem sys = new CinemaBookingSystem(null);
        assertTrue(sys.initializeSystem());
    }

    @Test
    void testInitializeSystemWithInvalidInput() {
        String input = "InvalidInput\n";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);
        CinemaBookingSystem sys = new CinemaBookingSystem(null);
        assertFalse(sys.initializeSystem());
    }

    @Test
    void testHandleMenuSelectionInvalid() {
        // Should print invalid selection message
        system.handleMenuSelection("invalid");
        // No exception means pass
    }

    @Test
    void testHandleExit() {
        system.handleExit();
        // running should be false after exit
        // Can't check scanner closed, but can check running
        // Use reflection if needed
    }
}
