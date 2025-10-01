package com.renfei.booking.cinema.service;

import com.renfei.booking.cinema.configuration.CinemaHallConfig;
import com.renfei.booking.cinema.exception.BookingException;
import com.renfei.booking.cinema.model.Booking;
import com.renfei.booking.cinema.strategy.DefaultPriorityStrategy;
import com.renfei.booking.cinema.strategy.impl.GICCustomSeatingStrategy;
import com.renfei.booking.cinema.strategy.impl.GICDefaultPriorityStrategy;
import com.renfei.booking.cinema.strategy.impl.GICDefaultSeatingStrategy;
import com.renfei.booking.cinema.utility.SeatUtil;
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
  public final Map<String, Booking> bookings = new HashMap<>();
  public final int[][] seatingMap;
  public final ReentrantLock bookingLock = new ReentrantLock();
  private final String movieTitle;
  private final int totalRows;
  private final int seatsPerRow;
  private final AtomicInteger bookingCounter = new AtomicInteger(0);
  private final DefaultPriorityStrategy defaultPriorityStrategy = new GICDefaultPriorityStrategy();
  private final int[] defaultColPriority;

  public CinemaHall(String movieTitle, int rows, int seatsPerRow) {
    int maxRows = CinemaHallConfig.MAX_ROWS;
    int maxSeatsPerRow = CinemaHallConfig.MAX_SEATS_PER_ROW;
    int minRows = CinemaHallConfig.MIN_ROWS;
    int minSeatsPerRow = CinemaHallConfig.MIN_SEATS_PER_ROW;
    if (movieTitle == null || movieTitle.trim().isEmpty()) {
      throw new BookingException("Movie title cannot be empty.");
    }
    if (rows > maxRows || seatsPerRow > maxSeatsPerRow) {
      throw new BookingException(
          "Max rows is " + maxRows + ", max seats per row is " + maxSeatsPerRow + ".");
    }
    if (rows < minRows || seatsPerRow < minSeatsPerRow) {
      throw new BookingException("Rows and seats per row must be at least 1.");
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
        return null;
      }
      List<int[]> selectedSeats =
          new GICDefaultSeatingStrategy()
              .selectSeats(
                  seatingMap, totalRows, seatsPerRow, numTickets, defaultColPriority, null);

      if (selectedSeats.size() == numTickets) {
        return finalizeBooking(null, numTickets, selectedSeats);
      }
      return null;
    } finally {
      bookingLock.unlock();
    }
  }

  public Booking bookCustom(int numTickets, String startPosition) {
    int[] start = SeatUtil.parseSeatPosition(startPosition, totalRows, seatsPerRow);
    if (start == null) {
      System.out.println("Invalid or out-of-bounds starting position: " + startPosition);
      return null;
    }

    bookingLock.lock();
    try {
      if (numTickets > getAvailableSeatsCount()) {
        return null;
      }

      List<int[]> selectedSeats =
          new GICCustomSeatingStrategy()
              .selectSeats(
                  seatingMap,
                  totalRows,
                  seatsPerRow,
                  numTickets,
                  defaultColPriority,
                  startPosition);

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
    StringBuilder sb = new StringBuilder();
    int seatSymbolWidth = 3; // " %2s"
    int totalWidth = seatsPerRow * seatSymbolWidth;
    String screenLabel = "S C R E E N";
    // Center the screen label above the seats
    int screenLabelPadding = Math.max(0, (totalWidth - screenLabel.length()) / 2);
    sb.append("  ").append(" ".repeat(screenLabelPadding)).append(screenLabel).append("\n");
    // Separator line aligned with seat numbers
    sb.append("    ").append("-".repeat(totalWidth - 2)).append("\n");
    Map<String, List<int[]>> allBookedSeats = new HashMap<>();
    bookings.forEach((id, booking) -> allBookedSeats.put(id, booking.getSelectedSeats()));
    for (int r = totalRows - 1; r >= 0; r--) {
      sb.append(SeatUtil.rowIndexToLabel(r)).append(" ");
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
        sb.append(String.format(" %2s", symbol));
      }
      sb.append("\n");
    }
    sb.append("  ");
    for (int c = 1; c <= seatsPerRow; c++) {
      if (c > 9) {
        sb.append(String.format(" %3d", c) );
      } else {
        sb.append(String.format(" %2d", c));
      }
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
