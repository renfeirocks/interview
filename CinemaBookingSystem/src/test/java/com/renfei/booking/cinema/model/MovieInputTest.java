package com.renfei.booking.cinema.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MovieInputTest {

    @Test
    void testMovieInputConstructor() {
        MovieInput movieInput = new MovieInput("Inception", 10, 20);

        assertEquals("Inception", movieInput.title);
        assertEquals(10, movieInput.rows);
        assertEquals(20, movieInput.seatsPerRow);
    }
}
