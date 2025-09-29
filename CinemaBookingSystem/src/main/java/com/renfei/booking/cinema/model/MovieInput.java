package com.renfei.booking.cinema.model;

public class MovieInput {
    public final String title;
    public final int rows;
    public final int seatsPerRow;
    public MovieInput(String title, int rows, int seatsPerRow) {
        this.title = title;
        this.rows = rows;
        this.seatsPerRow = seatsPerRow;
    }
}

/**
 * Represents a single booking in the cinema system.
 * This class is immutable after creation.
 */

