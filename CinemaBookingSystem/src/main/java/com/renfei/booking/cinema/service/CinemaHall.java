package com.renfei.booking.cinema.service;

import com.renfei.booking.cinema.configuration.CinemaHallConfig;
import com.renfei.booking.cinema.model.Booking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

/**
 * Manages the state and booking logic of the cinema hall.
 * Thread-safe operations are ensured using ReentrantLock.
 */

public class CinemaHall {
    private final String movieTitle;
    private final int totalRows;
    private final int seatsPerRow;
    public final Map<String, Booking> bookings = new HashMap<>();
    public final int[][] seatingMap;
    public final ReentrantLock bookingLock = new ReentrantLock();
    private final AtomicInteger bookingCounter = new AtomicInteger(0);
    private final int[] defaultColPriority;


    public CinemaHall(String movieTitle, int rows, int seatsPerRow) {
        int maxRows = CinemaHallConfig.MAX_ROWS;
        int maxSeatsPerRow = CinemaHallConfig.MAX_SEATS_PER_ROW;
        if (rows > maxRows || seatsPerRow > maxSeatsPerRow) {
            throw new IllegalArgumentException("Max rows is " + maxRows + ", max seats per row is "+maxSeatsPerRow + ".");
        }
        this.movieTitle = movieTitle;
        this.totalRows = rows;
        this.seatsPerRow = seatsPerRow;
        this.seatingMap = new int[rows][seatsPerRow];
        this.defaultColPriority = calculateDefaultColPriority(seatsPerRow);
    }

    //Time complexity: O(n) where n is the number of seats per row, linear time
    private int[] calculateDefaultColPriority(int count) {
        int centerLeft = (count - 1) / 2; // e.g., 10 -> 4, 9 -> 4
        int centerRight = count / 2; // e.g., 10 -> 5, 9 -> 4

        List<Integer> priority = new ArrayList<>();
        if (count % 2 != 0) {
            // Odd number of seats, start from the single center seat
            priority.add(centerLeft);
            for (int i = 1; i <= centerLeft; i++) {
                priority.add(centerLeft - i); // Left outward
                priority.add(centerRight + i); // Right outward
            }
        } else {
            // Even number of seats, start from the two middle seats
            for (int i = 0; i < count / 2; i++) {
                priority.add(centerLeft - i); // Left outward
                priority.add(centerRight + i); // Right outward
            }
        }
        return priority.stream().mapToInt(i -> i).toArray();
    }


    public String getMovieTitle() {
        return movieTitle;
    }

    public int getAvailableSeatsCount() {
        return (int) IntStream.range(0, totalRows)
                .mapToLong(r -> IntStream.of(seatingMap[r]).filter(seat -> seat == 0).count())
                .sum();
    }

    private char rowIndexToLabel(int rowIndex) {
        return (char) ('A' + rowIndex);
    }

    private int[] parseSeatPosition(String position) {
        if (position == null || position.length() < 2 || position.length() > 3) return null;
        char rowLabel = Character.toUpperCase(position.charAt(0));
        int rowIndex = rowLabel - 'A';
        int colIndex;
        try {
            int colNumber = Integer.parseInt(position.substring(1));
            colIndex = colNumber - 1;
        } catch (NumberFormatException e) {
            return null;
        }
        if (rowIndex >= 0 && rowIndex < totalRows && colIndex >= 0 && colIndex < seatsPerRow) {
            return new int[]{rowIndex, colIndex};
        }
        return null;
    }

    public Booking bookDefault(int numTickets) {
        bookingLock.lock();
        try {
            if (numTickets > getAvailableSeatsCount()) {
                return null;
            }
            List<int[]> selectedSeats = new ArrayList<>();
            for (int r = totalRows - 1; r >= 0 && selectedSeats.size() < numTickets; r--) {
                for (int c : defaultColPriority) {
                    if (seatingMap[r][c] == 0) {
                        selectedSeats.add(new int[]{r, c});
                        if (selectedSeats.size() == numTickets) break;
                    }
                }
            }
            if (selectedSeats.size() == numTickets) {
                return finalizeBooking(numTickets, selectedSeats);
            }
            return null;
        } finally {
            bookingLock.unlock();
        }
    }

    public Booking bookCustom(int numTickets, String startPosition) {
        int[] start = parseSeatPosition(startPosition);
        if (start == null) {
            System.out.println("Invalid or out-of-bounds starting position: " + startPosition);
            return null;
        }
        bookingLock.lock();
        try {
            if (numTickets > getAvailableSeatsCount()) {
                return null;
            }
            List<int[]> selectedSeats = new ArrayList<>();
            int startRow = start[0];
            int startCol = start[1];
            for (int c = startCol; c < seatsPerRow && selectedSeats.size() < numTickets; c++) {
                if (seatingMap[startRow][c] == 0) {
                    selectedSeats.add(new int[]{startRow, c});
                }
            }
            if (selectedSeats.size() < numTickets) {
                int ticketsToAllocate = numTickets - selectedSeats.size();
                for (int r = startRow - 1; r >= 0 && ticketsToAllocate > 0; r--) {
                    for (int c : defaultColPriority) {
                        if (seatingMap[r][c] == 0) {
                            selectedSeats.add(new int[]{r, c});
                            ticketsToAllocate--;
                            if (ticketsToAllocate == 0) break;
                        }
                    }
                }
            }
            if (selectedSeats.size() == numTickets) {
                return finalizeBooking(numTickets, selectedSeats);
            }
            return null;
        } finally {
            bookingLock.unlock();
        }
    }

    private Booking finalizeBooking(int numTickets, List<int[]> selectedSeats) {
        String bookingId = String.format("GIC%04d", bookingCounter.incrementAndGet());
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

    public String displaySeatingMap(String currentBookingId) {
        StringBuilder sb = new StringBuilder();
        sb.append("S C R E E N\n");
        sb.append("-----------\n");
        Map<String, List<int[]>> allBookedSeats = new HashMap<>();
        bookings.forEach((id, booking) -> allBookedSeats.put(id, booking.getSelectedSeats()));
        for (int r = totalRows - 1; r >= 0; r--) {
            sb.append(rowIndexToLabel(r)).append(" ");
            for (int c = 0; c < seatsPerRow; c++) {
                char symbol = '.';
                boolean isBooked = false;
                String seatBookingId = null;
                if (seatingMap[r][c] == 1) {
                    isBooked = true;
                    for (Map.Entry<String, List<int[]>> entry : allBookedSeats.entrySet()) {
                        for (int[] seat : entry.getValue()) {
                            if (seat[0] == r && seat[1] == c) {
                                seatBookingId = entry.getKey();
                                break;
                            }
                        }
                        if (seatBookingId != null) break;
                    }
                }
                if (isBooked) {
                    if (currentBookingId != null && currentBookingId.equals(seatBookingId)) {
                        symbol = 'O';
                    } else {
                        symbol = '#';
                    }
                }
                sb.append(symbol).append(" ");
            }
            sb.append("\n");
        }
        sb.append("  ");
        for (int c = 1; c <= seatsPerRow; c++) {
            sb.append(c % 10).append(" ");
        }
        sb.append("\n");
        return sb.toString();
    }
}

