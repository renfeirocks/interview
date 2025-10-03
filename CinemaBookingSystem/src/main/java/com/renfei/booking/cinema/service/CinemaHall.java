package com.renfei.booking.cinema.service;

import com.renfei.booking.cinema.configuration.CinemaHallConfig;
import com.renfei.booking.cinema.exception.ErrorMessageConstants;
import com.renfei.booking.cinema.exception.ErrorMessageStore;
import com.renfei.booking.cinema.model.Booking;
import com.renfei.booking.cinema.strategy.DefaultPriorityStrategy;
import com.renfei.booking.cinema.strategy.impl.GICCustomSeatingStrategy;
import com.renfei.booking.cinema.strategy.impl.GICDefaultPriorityStrategy;
import com.renfei.booking.cinema.strategy.impl.GICDefaultSeatingStrategy;
import com.renfei.booking.cinema.utility.SeatUtil;
import com.renfei.booking.cinema.service.SeatSelectionService;
import com.renfei.booking.cinema.utility.CinemaHallViewHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import static com.renfei.booking.cinema.configuration.CinemaHallConfig.GIC_BOOKING_ID;
import static com.renfei.booking.cinema.configuration.CinemaHallConfig.SCREEN;

/**
 * Manages the state and booking logic of the cinema Thread-safe operations are ensured using
 * ReentrantLock.
 */
public class CinemaHall {
    public final Map<String, Booking> bookings = new HashMap<>();
    public final int[][] seatingMap;
    public final ReentrantLock bookingLock = new ReentrantLock();
    public final int seatsPerRow;
    private final String movieTitle;
    private final int totalRows;
    private final AtomicInteger bookingCounter = new AtomicInteger(0);
    private final DefaultPriorityStrategy defaultPriorityStrategy = new GICDefaultPriorityStrategy();
    private final int[] defaultColPriority;
    private final SeatSelectionService seatSelectionService = new SeatSelectionService();

    public CinemaHall(String movieTitle, int rows, int seatsPerRow) {
        int maxRows = CinemaHallConfig.MAX_ROWS;
        int maxSeatsPerRow = CinemaHallConfig.MAX_SEATS_PER_ROW;
        int minRows = CinemaHallConfig.MIN_ROWS;
        int minSeatsPerRow = CinemaHallConfig.MIN_SEATS_PER_ROW;
        if (movieTitle == null || movieTitle.trim().isEmpty()) {
            ErrorMessageStore.put(ErrorMessageConstants.EMPTY_TITLE, "Movie title cannot be empty.");
            System.out.println("Movie title cannot be empty.");
        }
        if (rows > maxRows || seatsPerRow > maxSeatsPerRow) {
            String msg = "Max rows is " + maxRows + ", max seats per row is " + maxSeatsPerRow + ".";
            ErrorMessageStore.put(ErrorMessageConstants.DIMENSION_EXCEEDS_MAX, msg);
            System.out.println(msg);
        }
        if (rows < minRows || seatsPerRow < minSeatsPerRow) {
            String msg = "Rows and seats per row must be at least 1.";
            ErrorMessageStore.put(ErrorMessageConstants.DIMENSION_BELOW_MIN, msg);
            System.out.println(msg);
        }
        this.movieTitle = movieTitle;
        this.totalRows = rows;
        this.seatsPerRow = seatsPerRow;
        this.seatingMap = new int[rows][seatsPerRow];
        this.defaultColPriority = defaultPriorityStrategy.calculateDefaultColPriority(seatsPerRow);
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public int getAvailableSeatsCount() {
        return (int)
                IntStream.range(0, totalRows)
                        .mapToLong(r -> IntStream.of(seatingMap[r]).filter(seat -> seat == 0).count())
                        .sum();
    }

    public Booking bookDefault(int numTickets) {
        bookingLock.lock();
        try {
            if (numTickets > getAvailableSeatsCount()) {
                ErrorMessageStore.put(
                        ErrorMessageConstants.OVERBOOKING, "Requested more tickets than available.");
                return null;
            }
            List<int[]> selectedSeats =
                    seatSelectionService.selectDefaultSeats(
                        seatingMap, totalRows, seatsPerRow, numTickets, defaultColPriority);

            if (selectedSeats.size() == numTickets) {
                return finalizeBooking(null, numTickets, selectedSeats);
            } else {
                ErrorMessageStore.put(
                        ErrorMessageConstants.SEAT_SELECTION_FAILED,
                        "Could not find suitable seats for the requested number of tickets.");
                return null;
            }

        } finally {
            bookingLock.unlock();
        }
    }

    public Booking bookCustom(Booking booking, int numTickets, String startPosition) {
        int[] start = SeatUtil.parseSeatPosition(startPosition, totalRows, seatsPerRow);
        if (start == null) {
            String msg = "Invalid or out-of-bounds starting position: " + startPosition;
            ErrorMessageStore.put(ErrorMessageConstants.INVALID_START_POSITION, msg);
            System.out.println(msg);
            return null;
        }
        bookingLock.lock();
        try {
            if (numTickets > getAvailableSeatsCount()) {
                ErrorMessageStore.put(
                        ErrorMessageConstants.OVERBOOKING_CUSTOM,
                        "Requested more tickets than available (custom).");
                return null;
            }

            List<int[]> selectedSeats =
                    seatSelectionService.selectCustomSeats(
                        seatingMap,
                        totalRows,
                        seatsPerRow,
                        numTickets,
                        defaultColPriority,
                        startPosition);

            if (selectedSeats.size() == numTickets) {
                return finalizeBooking(booking.getBookingId(), numTickets, selectedSeats);
            } else {
                ErrorMessageStore.put(
                        ErrorMessageConstants.SEAT_SELECTION_FAILED,
                        "Could not find suitable seats for the requested number of tickets.");
                return null;
            }

        } finally {
            bookingLock.unlock();
        }
    }

    public String displaySeatingMap(String currentBookingId) {
        return CinemaHallViewHelper.displaySeatingMap(seatingMap, totalRows, seatsPerRow, bookings, currentBookingId);
    }

    private Booking finalizeBooking(String bookingId, int numTickets, List<int[]> selectedSeats) {
        if (bookingId == null) {
            bookingId = String.format(GIC_BOOKING_ID, bookingCounter.incrementAndGet());
        }
        // Mark new seats as booked
        for (int[] seat : selectedSeats) {
            seatingMap[seat[0]][seat[1]] = 1;
        }
        Booking newBooking = new Booking(bookingId, numTickets, selectedSeats);
        bookings.put(bookingId, newBooking);
        return newBooking;
    }

    public Booking getBooking(String bookingId) {
        return bookings.get(bookingId);
    }

    public Booking reallocateSeats(Booking booking, int tickets, String newPos) {
        System.out.println("Attempting to re-allocate seats...");

        // Temporary un-book (not exposed in public API, only for re-allocation logic)
        // A real system would use a transaction rollback mechanism.
        List<int[]> oldSeats = booking.getSelectedSeats();
        bookingLock.lock();
        try {
            // Mark old seats as available (0)
            for (int[] seat : oldSeats) {
                seatingMap[seat[0]][seat[1]] = 0;
            }

            Booking tempBooking = bookCustom(booking, tickets, newPos);
            if (tempBooking != null) {
                Booking finalBooking =
                        new Booking(booking.getBookingId(), tickets, tempBooking.getSelectedSeats());

                // Remove the temporary booking (GIC00XX+1)
                bookings.remove(tempBooking.getBookingId());
                // Update the original booking with new seats
                bookings.put(finalBooking.getBookingId(), finalBooking);
                booking = finalBooking; // Update reference for next loop/confirmation

                System.out.printf(
                        "Re-reserved %d %s tickets to new selection.\n", tickets, getMovieTitle());
                System.out.println("Selected seats:");
                System.out.print(displaySeatingMap(booking.getBookingId()));
            } else {
                // Re-allocation failed. Must restore the old booking.
                for (int[] seat : oldSeats) {
                    seatingMap[seat[0]][seat[1]] = 1; // Mark old seats as booked (1) again
                }
                String msg =
                        "Could not re-reserve seats with the starting position: "
                                + newPos
                                + ". Please try again.";
                ErrorMessageStore.put(ErrorMessageConstants.REALLOCATION_FAILED, msg);
                System.out.println(msg);
                // Display the seating map with the old selection highlighted.
                System.out.print(displaySeatingMap(booking.getBookingId()));
            }
        } finally {
            bookingLock.unlock();
        }
        return booking;
    }

    public int[] getDefaultColPriority() {
        return defaultColPriority;
    }
}
