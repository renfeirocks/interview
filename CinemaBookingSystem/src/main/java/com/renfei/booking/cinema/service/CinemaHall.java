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
 * Manages the state and booking logic of the cinema Thread-safe operations are ensured using
 * ReentrantLock.
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
      throw new IllegalArgumentException(
          "Max rows is " + maxRows + ", max seats per row is " + maxSeatsPerRow + ".");
    }
    this.movieTitle = movieTitle;
    this.totalRows = rows;
    this.seatsPerRow = seatsPerRow;
    this.seatingMap = new int[rows][seatsPerRow];
    this.defaultColPriority = calculateDefaultColPriority(seatsPerRow);
  }

  // Time complexity: O(n) where n is the number of seats per row, linear time
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
    return (int)
        IntStream.range(0, totalRows)
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
      return new int[] {rowIndex, colIndex};
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
            selectedSeats.add(new int[] {r, c});
            if (selectedSeats.size() == numTickets) break;
          }
        }
      }
      if (selectedSeats.size() == numTickets) {
        return finalizeBooking(null, numTickets, selectedSeats);
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

      // 1. Same row, rightward allocation
      for (int c = startCol; c < seatsPerRow && selectedSeats.size() < numTickets; c++) {
        if (seatingMap[startRow][c] == 0) {
          selectedSeats.add(new int[] {startRow, c});
        }
      }

      // 2. Overflow to next rows (closer to screen), using default rules
      if (selectedSeats.size() < numTickets) {
        int ticketsToAllocate = numTickets - selectedSeats.size();

        // Row order: startRow-1 to 0 (rows closer to the screen)
        // Use defaultColPriority for overflow
        for (int r = startRow - 1; r >= 0 && ticketsToAllocate > 0; r--) {
          for (int c : defaultColPriority) {
            if (seatingMap[r][c] == 0) {
              selectedSeats.add(new int[] {r, c});
              ticketsToAllocate--;
              if (ticketsToAllocate == 0) break;
            }
          }
        }
      }

      if (selectedSeats.size() == numTickets) {
        return finalizeBooking(null, numTickets, selectedSeats);
      }
      return null; // Failed to find contiguous seats/overflow
    } finally {
      bookingLock.unlock();
    }
  }

  private Booking finalizeBooking(String bookingId, int numTickets, List<int[]> selectedSeats) {
    if (bookingId == null) {
      bookingId = String.format("GIC%04d", bookingCounter.incrementAndGet());
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

  public String displaySeatingMap(String currentBookingId) {

    // shifting the screen label to the center
    StringBuilder sb = new StringBuilder();
    String screenLabel = "S C R E E N";
    int totalWidth = seatsPerRow * 2;
    int padding = Math.max(0, (totalWidth - screenLabel.length()) / 2);
    sb.append(" ".repeat(padding)).append(screenLabel).append("\n");
    sb.append(" ".repeat(padding)).append("-".repeat(screenLabel.length())).append("\n");
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

      Booking tempBooking = bookCustom(tickets, newPos);
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
        System.out.println(
            "Could not re-reserve seats with the starting position: "
                + newPos
                + ". Please try again.");
        // Display the seating map with the old selection highlighted.
        System.out.print(displaySeatingMap(booking.getBookingId()));
      }
    } finally {
      bookingLock.unlock();
    }
    return booking;
  }
}
