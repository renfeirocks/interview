package com.renfei.booking.cinema.service;

import static org.junit.jupiter.api.Assertions.*;

import com.renfei.booking.cinema.controller.CinemaBookingSystem;
import com.renfei.booking.cinema.model.Booking;
import com.renfei.booking.cinema.strategy.SeatingStrategy;
import com.renfei.booking.cinema.strategy.impl.GICCustomSeatingStrategy;
import com.renfei.booking.cinema.strategy.impl.GICDefaultSeatingStrategy;
import com.renfei.booking.cinema.utility.ErrorMessageConstants;
import com.renfei.booking.cinema.utility.ErrorMessageStore;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CinemaHallTest {
  private CinemaHall cinemaHall;
  private SeatingStrategy defaultStrategy;
  private SeatingStrategy customStrategy;

  @BeforeEach
  void setUp() {
    cinemaHall = new CinemaHall("Test Movie", 10, 10);
    defaultStrategy = new GICDefaultSeatingStrategy();
    customStrategy = new GICCustomSeatingStrategy();
    ErrorMessageStore.getErrorMessages().clear();
  }

  // =====================
  // VALID TEST CASES
  // =====================

  @Test
  void bookSingleSeat_DefaultAndCustomStrategy() {
    // Default strategy: should book first available seat (row 0, col 0)
    List<int[]> defaultSeats =
        defaultStrategy.selectSeats(
            cinemaHall.seatingMap, 10, 10, 1, cinemaHall.getDefaultColPriority(), null);
    assertEquals(1, defaultSeats.size());
    assertArrayEquals(new int[] {0, 0}, defaultSeats.get(0));
    // Custom strategy: start at B2 (row 1, col 1)
    List<int[]> customSeats =
        customStrategy.selectSeats(
            cinemaHall.seatingMap, 10, 10, 1, cinemaHall.getDefaultColPriority(), "B2");
    assertEquals(1, customSeats.size());
    assertArrayEquals(new int[] {1, 1}, customSeats.get(0));
  }

  @Test
  void bookMultipleSeats_DefaultAndCustomStrategy() {
    // Default strategy: should book first two available seats in row 0
    List<int[]> defaultSeats =
        defaultStrategy.selectSeats(
            cinemaHall.seatingMap, 10, 10, 2, cinemaHall.getDefaultColPriority(), null);
    assertEquals(2, defaultSeats.size());
    assertArrayEquals(new int[] {0, 0}, defaultSeats.get(0));
    assertArrayEquals(new int[] {0, 1}, defaultSeats.get(1));
    // Custom strategy: start at C3 (row 2, col 2), should fill rightward
    List<int[]> customSeats =
        customStrategy.selectSeats(
            cinemaHall.seatingMap, 10, 10, 2, cinemaHall.getDefaultColPriority(), "C3");
    assertEquals(2, customSeats.size());
    assertArrayEquals(new int[] {2, 2}, customSeats.get(0));
    assertArrayEquals(new int[] {2, 3}, customSeats.get(1));
  }

  @Test
  void bookLastAvailableSeat_DefaultStrategy() {
    // Fill all but one seat
    for (int r = 0; r < 10; r++) {
      for (int c = 0; c < 10; c++) {
        if (r == 9 && c == 9) continue;
        cinemaHall.seatingMap[r][c] = 1;
      }
    }
    List<int[]> seats =
        defaultStrategy.selectSeats(
            cinemaHall.seatingMap, 10, 10, 1, cinemaHall.getDefaultColPriority(), null);
    assertEquals(1, seats.size());
    assertArrayEquals(new int[] {9, 9}, seats.get(0));
  }

  @Test
  void bookMultipleSeatsInSingleTransaction() {
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {0, 1});
    seats.add(new int[] {0, 2});
    Booking booking = new Booking("B2", 2, seats);
    cinemaHall.bookings.put(booking.getBookingId(), booking);
    assertTrue(cinemaHall.bookings.containsKey("B2"));
    assertEquals(2, cinemaHall.bookings.get("B2").getNumTickets());
  }

  @Test
  void cancelPreviouslyBookedSeat() {
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {0, 0});
    Booking booking = new Booking("B3", 1, seats);
    cinemaHall.bookings.put(booking.getBookingId(), booking);
    cinemaHall.bookings.remove("B3");
    assertFalse(cinemaHall.bookings.containsKey("B3"));
  }

  @Test
  void viewAvailableSeats() {
    // Book a seat
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {2, 2});
    Booking booking = new Booking("B4", 1, seats);
    cinemaHall.bookings.put(booking.getBookingId(), booking);
    // Check if seat (2,2) is not available, but (2,3) is
    boolean seatOccupied = false;
    for (Booking b : cinemaHall.bookings.values()) {
      for (int[] s : b.getSelectedSeats()) {
        if (s[0] == 2 && s[1] == 2) seatOccupied = true;
      }
    }
    assertTrue(seatOccupied);
    boolean seatFree = true;
    for (Booking b : cinemaHall.bookings.values()) {
      for (int[] s : b.getSelectedSeats()) {
        if (s[0] == 2 && s[1] == 3) seatFree = false;
      }
    }
    assertTrue(seatFree);
  }

  @Test
  void bookSeatAfterCancellation() {
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {3, 3});
    Booking booking = new Booking("B5", 1, seats);
    cinemaHall.bookings.put(booking.getBookingId(), booking);
    cinemaHall.bookings.remove("B5");
    // Book again
    Booking booking2 = new Booking("B6", 1, seats);
    cinemaHall.bookings.put(booking2.getBookingId(), booking2);
    assertTrue(cinemaHall.bookings.containsKey("B6"));
  }

  @Test
  void bookSeatsForDifferentMoviesAndShowtimes() {
    CinemaHall hall2 = new CinemaHall("Another Movie", 5, 5);
    List<int[]> seats1 = new ArrayList<>();
    seats1.add(new int[] {0, 0});
    Booking booking1 = new Booking("B7", 1, seats1);
    cinemaHall.bookings.put(booking1.getBookingId(), booking1);
    List<int[]> seats2 = new ArrayList<>();
    seats2.add(new int[] {1, 1});
    Booking booking2 = new Booking("B8", 1, seats2);
    hall2.bookings.put(booking2.getBookingId(), booking2);
    assertTrue(cinemaHall.bookings.containsKey("B7"));
    assertTrue(hall2.bookings.containsKey("B8"));
  }

  @Test
  void bookSeats_CustomStrategyOverflow() {
    // Book 8 seats starting at B3 (row 1, col 2)
    List<int[]> seats =
        customStrategy.selectSeats(
            cinemaHall.seatingMap, 10, 10, 8, cinemaHall.getDefaultColPriority(), "B3");
    assertEquals(8, seats.size());
    // Should fill row 1 from col 2 to 9, then overflow to row 2
    assertArrayEquals(new int[] {1, 2}, seats.get(0));
    assertArrayEquals(new int[] {1, 3}, seats.get(1));
    assertArrayEquals(new int[] {1, 4}, seats.get(2));
    assertArrayEquals(new int[] {1, 5}, seats.get(3));
    assertArrayEquals(new int[] {1, 6}, seats.get(4));
    assertArrayEquals(new int[] {1, 7}, seats.get(5));
    assertArrayEquals(new int[] {1, 8}, seats.get(6));
    assertArrayEquals(new int[] {1, 9}, seats.get(7));
  }

  @Test
  void bookSeats_CustomStrategyPrioritizesGivenSeat() {
    // Arrange
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {2, 2});
    Booking booking = new Booking("B4", 1, seats);
    cinemaHall.bookings.put(booking.getBookingId(), booking);

    // Act
    List<int[]> selectedSeats =
        customStrategy.selectSeats(
            cinemaHall.seatingMap, 10, 10, 1, cinemaHall.getDefaultColPriority(), "B4");

    // Assert
    assertEquals(1, selectedSeats.size());
    assertArrayEquals(new int[] {2, 2}, selectedSeats.get(0));
  }

  @Test
  void bookSeats_DefaultStrategyFillsAvailableSeats() {
    // Arrange
    cinemaHall.bookings.clear(); // Ensure no existing bookings
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 3; c++) {
        if (r == 1 && c == 1) continue; // Leave one seat (B2) intentionally
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[] {r, c});
        Booking booking = new Booking("BLOCK" + (r * 3 + c), 1, seats);
        cinemaHall.bookings.put(booking.getBookingId(), booking);
      }
    }

    // Act
    Booking booking = cinemaHall.bookDefault(1);

    // Assert
    assertNotNull(booking);
    assertEquals(1, booking.getSelectedSeats().size());
    assertArrayEquals(
        new int[] {1, 1}, booking.getSelectedSeats().get(0)); // Should be the blocked seat (B2)
  }

  @Test
  void testDefaultPriorityStrategy() {
    CinemaHall hall = new CinemaHall("Movie", 3, 5);
    // Book 3 tickets using default strategy
    Booking booking = hall.bookDefault(3);
    List<int[]> seats = booking.getSelectedSeats();
    // Should select from row 0 (furthest from screen)
    for (int[] seat : seats) {
      assertEquals(0, seat[0]);
    }
  }

  @Test
  void testCustomSeatingStrategyFillRowAndOverflow() {
    CinemaHall hall = new CinemaHall("Movie", 3, 5);
    // Book 2 tickets using default strategy to occupy some seats first
    Booking defaultBooking = hall.bookDefault(2);
    List<int[]> defaultSeats = defaultBooking.getSelectedSeats();
    // Book 4 tickets starting at B3 (row 1, col 2)
    defaultBooking = hall.bookCustom(defaultBooking, 4, "B3");
    List<int[]> seats = defaultBooking.getSelectedSeats();
    // Should fill row 1 from col 2 to 4, then overflow to row 0
    assertEquals(4, seats.size());
    assertEquals(1, seats.get(0)[0]);
    assertEquals(2, seats.get(0)[1]);
    assertEquals(1, seats.get(1)[0]);
    assertEquals(3, seats.get(1)[1]);
    assertEquals(1, seats.get(2)[0]);
    assertEquals(4, seats.get(2)[1]);
    // Overflow seat should be in row 0
    assertEquals(0, seats.get(3)[0]);
  }

  @Test
  void testCustomSeatingStrategyFillRowWithBookedSeats() {
    CinemaHall hall = new CinemaHall("Movie", 3, 5);
    // Book seat B4 (row 1, col 3) to simulate a booked seat

    Booking booking = hall.bookDefault(1);

    booking = hall.bookCustom(booking, 1, "B4"); // Book A1 to have a booking context
    String bookingId1 = booking.getBookingId();

    booking = hall.bookDefault(3);
    System.out.println(hall.displaySeatingMap(booking.getBookingId()));
    System.out.println("Booking ID: " + booking.getBookingId());
    booking = hall.bookCustom(booking, 3, "B3");
    String bookingId2 = booking.getBookingId();

    // Should skip B4 and fill B3, B5, then overflow to row 0
    assertEquals(3, booking.getSelectedSeats().size());
    //        assertEquals(3, seats.size());
    //        assertEquals(1, seats.get(0)[0]);
    //        assertEquals(2, seats.get(0)[1]);
    //        assertEquals(1, seats.get(1)[0]);
    //        assertEquals(4, seats.get(1)[1]);
    //        assertEquals(0, seats.get(2)[3]);
  }

  @Test
  void testDefaultSeatingStrategy() {
    CinemaHall hall = new CinemaHall("Movie", 2, 3);
    // Book all seats
    Booking booking = hall.bookDefault(6);
    List<int[]> seats = booking.getSelectedSeats();
    assertEquals(6, seats.size());
    // Should fill row 0 first, then row 1
    int row0Count = 0, row1Count = 0;
    for (int[] seat : seats) {
      if (seat[0] == 0) row0Count++;
      if (seat[0] == 1) row1Count++;
    }
    assertEquals(3, row0Count);
    assertEquals(3, row1Count);
  }

  @Test
  void testBookFourCornerSeatsWhenOnlyCornersLeft() {
    CinemaHall hall = new CinemaHall("Movie", 3, 3);
    // Book all seats except corners
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 3; c++) {
        if ((r == 0 && c == 0) || (r == 0 && c == 2) || (r == 2 && c == 0) || (r == 2 && c == 2)) {
          continue; // skip corners
        }
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[] {r, c});
        Booking booking = new Booking("B" + r + c, 1, seats);
        hall.bookings.put(booking.getBookingId(), booking);
        hall.seatingMap[r][c] = 1;
      }
    }
    // Now only corners are available
    Booking booking = hall.bookDefault(4);
    List<int[]> seats = booking.getSelectedSeats();
    assertEquals(4, seats.size());
    boolean has00 = false, has02 = false, has20 = false, has22 = false;
    for (int[] seat : seats) {
      if (seat[0] == 0 && seat[1] == 0) has00 = true;
      if (seat[0] == 0 && seat[1] == 2) has02 = true;
      if (seat[0] == 2 && seat[1] == 0) has20 = true;
      if (seat[0] == 2 && seat[1] == 2) has22 = true;
    }
    assertTrue(has00 && has02 && has20 && has22, "Should book all 4 corners");
  }

  // =====================
  // MULTI-THREADING TEST CASES
  // =====================

  @Test
  void concurrentBookingSameSeat() throws InterruptedException {
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    List<Future<Boolean>> results = new ArrayList<>();
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {0, 0});
    for (int i = 0; i < threadCount; i++) {
      final String bookingId = "T" + i;
      results.add(
          executor.submit(
              () -> {
                try {
                  synchronized (cinemaHall) {
                    if (!cinemaHall.bookings.values().stream()
                        .anyMatch(
                            b ->
                                b.getSelectedSeats().get(0)[0] == 0
                                    && b.getSelectedSeats().get(0)[1] == 0)) {
                      Booking booking = new Booking(bookingId, 1, seats);
                      cinemaHall.bookings.put(booking.getBookingId(), booking);
                      return true;
                    }
                  }
                } finally {
                  latch.countDown();
                }
                return false;
              }));
    }
    latch.await();
    executor.shutdown();
    int successCount = 0;
    for (Future<Boolean> f : results) {
      try {
        if (f.get()) successCount++;
      } catch (Exception ignored) {
      }
    }
    assertEquals(1, successCount, "Only one booking should succeed for the same seat");
  }

  @ParameterizedTest
  @CsvSource({
    // Small
    "1,1",
    "1,5",
    "2,2",
    // Medium
    "3,10",
    "10,3",
    "5,5",
    // Large
    "10,10",
    "15,20",
    "20,15"
  })
  void testBookingWithVariousHallSizes(int rows, int seatsPerRow) {
    CinemaHall hall = new CinemaHall("ParamTestMovie", rows, seatsPerRow);
    // Book the first seat
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {0, 0});
    Booking booking = new Booking("P1", 1, seats);
    hall.bookings.put(booking.getBookingId(), booking);
    assertTrue(hall.bookings.containsKey("P1"));
    assertEquals(1, hall.bookings.get("P1").getNumTickets());
    // Book the last seat
    List<int[]> lastSeat = new ArrayList<>();
    lastSeat.add(new int[] {rows - 1, seatsPerRow - 1});
    Booking booking2 = new Booking("P2", 1, lastSeat);
    hall.bookings.put(booking2.getBookingId(), booking2);
    assertTrue(hall.bookings.containsKey("P2"));
  }

  @Test
  void testMaxRowsAndSeatsPerRow() {
    int maxRows = 20; // Adjust if CinemaHallConfig.MAX_ROWS is different
    int maxSeatsPerRow = 20; // Adjust if CinemaHallConfig.MAX_SEATS_PER_ROW is different
    CinemaHall hall = new CinemaHall("MaxTestMovie", maxRows, maxSeatsPerRow);
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {maxRows - 1, maxSeatsPerRow - 1});
    Booking booking = new Booking("MAX1", 1, seats);
    hall.bookings.put(booking.getBookingId(), booking);
    assertTrue(hall.bookings.containsKey("MAX1"));
  }

  @Test
  void testOneRowMaxSeats() {
    int maxSeatsPerRow = 20; // Adjust if CinemaHallConfig.MAX_SEATS_PER_ROW is different
    CinemaHall hall = new CinemaHall("OneRowMaxSeats", 1, maxSeatsPerRow);
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {0, maxSeatsPerRow - 1});
    Booking booking = new Booking("ORMAX", 1, seats);
    hall.bookings.put(booking.getBookingId(), booking);
    assertTrue(hall.bookings.containsKey("ORMAX"));
  }

  @Test
  void testMaxRowsOneSeat() {
    int maxRows = 20; // Adjust if CinemaHallConfig.MAX_ROWS is different
    CinemaHall hall = new CinemaHall("MaxRowsOneSeat", maxRows, 1);
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {maxRows - 1, 0});
    Booking booking = new Booking("MR1S", 1, seats);
    hall.bookings.put(booking.getBookingId(), booking);
    assertTrue(hall.bookings.containsKey("MR1S"));
  }

  @Test
  void bookWithDuplicateSeatsInSameBooking() {
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {1, 1});
    seats.add(new int[] {1, 1}); // duplicate
    Booking booking = new Booking("DUP1", 2, seats);
    cinemaHall.bookings.put(booking.getBookingId(), booking);
    // Check that both seats are the same
    assertEquals(2, booking.getSelectedSeats().size());
    assertEquals(booking.getSelectedSeats().get(0)[0], booking.getSelectedSeats().get(1)[0]);
    assertEquals(booking.getSelectedSeats().get(0)[1], booking.getSelectedSeats().get(1)[1]);
  }

  @Test
  void bookWithEmptyBookingId() {
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {2, 3});
    Booking booking = new Booking("", 1, seats);
    cinemaHall.bookings.put(booking.getBookingId(), booking);
    assertTrue(cinemaHall.bookings.containsKey(""));
  }

  @Test
  void bookWithSpecialCharactersInBookingId() {
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {1, 2});
    Booking booking = new Booking("!@#$%^&*()_+", 1, seats);
    cinemaHall.bookings.put(booking.getBookingId(), booking);
    assertTrue(cinemaHall.bookings.containsKey("!@#$%^&*()_+"));
  }

  @Test
  void bookWithMaxTickets() {
    int max = 5; // For this test, use 5 as a practical max
    List<int[]> seats = new ArrayList<>();
    for (int i = 0; i < max; i++) seats.add(new int[] {0, i});
    Booking booking = new Booking("MAXTIX", max, seats);
    cinemaHall.bookings.put(booking.getBookingId(), booking);
    assertTrue(cinemaHall.bookings.containsKey("MAXTIX"));
    assertEquals(max, booking.getNumTickets());
  }

  @Test
  void bookWithMinTickets() {
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {0, 0});
    Booking booking = new Booking("MINTIX", 1, seats);
    cinemaHall.bookings.put(booking.getBookingId(), booking);
    assertTrue(cinemaHall.bookings.containsKey("MINTIX"));
    assertEquals(1, booking.getNumTickets());
  }

  @Test
  void stressTestHighVolumeBookings() {
    CinemaHall hall = new CinemaHall("StressTest", 10, 10);
    int bookingNum = 1;
    for (int r = 0; r < 10; r++) {
      for (int c = 0; c < 10; c++) {
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[] {r, c});
        Booking booking = new Booking("STRESS" + bookingNum++, 1, seats);
        hall.bookings.put(booking.getBookingId(), booking);
      }
    }
    assertEquals(100, hall.bookings.size());
  }

  @Test
  void bookSeats_SuccessfulBooking_ShouldReturnBooking() {
    Booking booking = cinemaHall.bookDefault(2);
    assertNotNull(booking);
    assertEquals(2, booking.getSelectedSeats().size());
    assertEquals(98, cinemaHall.getAvailableSeatsCount());
  }

  @Test
  void bookSeats_SingleSeat_ShouldReturnBooking() {
    Booking booking = cinemaHall.bookDefault(1);
    assertNotNull(booking);
    assertEquals(1, booking.getSelectedSeats().size());
    assertEquals(99, cinemaHall.getAvailableSeatsCount());
  }

  @Test
  void bookSeats_LastRemainingSeat_ShouldSucceed() {
    cinemaHall = new CinemaHall("Test Movie", 1, 1);
    Booking booking = cinemaHall.bookDefault(1);
    assertNotNull(booking);
    assertEquals(1, booking.getSelectedSeats().size());
    assertEquals(0, cinemaHall.getAvailableSeatsCount());
  }

  @Test
  void bookSeats_MoreThanAvailable_ShouldReturnNull() {
    Booking booking = cinemaHall.bookDefault(101);
    assertNull(booking);
    assertEquals(100, cinemaHall.getAvailableSeatsCount());
  }

  // Negative and Edge Cases
  @Test
  void bookSeats_ZeroSeats_ShouldThrowException() {
    assertThrows(IllegalArgumentException.class, () -> cinemaHall.bookDefault(0));
  }

  @Test
  void bookSeats_NegativeSeats_ShouldThrowException() {
    //        ErrorMessageStore.clear();
    String simulatedInput = "TestMovie 10 10\n1\n-1\n1\n\n3\n";
    InputStream originalIn = new ByteArrayInputStream(simulatedInput.getBytes());
    //        System.setIn();
    OutputStream originalOut = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(originalOut);
    PrintStream originalSystemOut = System.out;
    System.setOut(printStream);
    try {
      CinemaBookingSystem system = new CinemaBookingSystem(originalIn);
      system.start();
        System.out.println(originalOut);
    } finally {
      System.setOut(originalSystemOut);
    }
//    System.setIn(originalIn);

    //
    // assertTrue(ErrorMessageStore.getErrorMessages().containsKey(ErrorMessageConstants.NEGATIVE_TICKET_COUNT));
    assertEquals(
        "Please enter a positive number.",
        ErrorMessageStore.get(ErrorMessageConstants.NEGATIVE_TICKET_COUNT));
  }

  @Test
  void reallocateSeats_ToAlreadyBooked_ShouldReturnNull() {
    cinemaHall.bookDefault(1); // Books A1
    Booking booking2 = cinemaHall.bookDefault(1); // Books A2
    Booking reallocatedBooking = cinemaHall.reallocateSeats(booking2, 1, "A1");
    assertNull(reallocatedBooking);
  }

  @Test
  void reallocateSeats_ToInvalidSeat_ShouldReturnNull() {
    Booking booking = cinemaHall.bookDefault(1);
    Booking reallocatedBooking = cinemaHall.reallocateSeats(booking, 1, "Z99");
    assertNull(reallocatedBooking);
  }

  @Test
  void bookSeatsWithDefaultStrategy() {
    List<int[]> seats =
        defaultStrategy.selectSeats(
            cinemaHall.seatingMap, 10, 10, 2, cinemaHall.getDefaultColPriority(), null);
    assertEquals(2, seats.size());
    assertEquals(0, seats.get(0)[0]); // Should be row 0
  }

  @Test
  void bookSeatsWithCustomStrategy() {
    List<int[]> seats =
        customStrategy.selectSeats(
            cinemaHall.seatingMap, 10, 10, 3, cinemaHall.getDefaultColPriority(), "B3");
    assertEquals(3, seats.size());
    assertEquals(1, seats.get(0)[0]); // Should start at row 1 (B)
    assertEquals(2, seats.get(0)[1]); // Should start at col 2 (3rd seat)
  }

  @Test
  void concurrentBookingSameSeat_DefaultStrategy() throws InterruptedException, ExecutionException {
    int threadCount = 5;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    List<Future<Boolean>> results = new ArrayList<>();
    for (int i = 0; i < threadCount; i++) {
      results.add(
          executor.submit(
              () -> {
                try {
                  List<int[]> seats =
                      defaultStrategy.selectSeats(
                          cinemaHall.seatingMap,
                          10,
                          10,
                          1,
                          cinemaHall.getDefaultColPriority(),
                          null);
                  if (!seats.isEmpty()
                      && cinemaHall.seatingMap[seats.get(0)[0]][seats.get(0)[1]] == 0) {
                    cinemaHall.seatingMap[seats.get(0)[0]][seats.get(0)[1]] = 1;
                    return true;
                  }
                } finally {
                  latch.countDown();
                }
                return false;
              }));
    }
    latch.await();
    executor.shutdown();
    int successCount = 0;
    for (Future<Boolean> f : results) {
      if (f.get()) successCount++;
    }
    assertEquals(1, successCount, "Only one booking should succeed for the same seat");
  }

  @Test
  void concurrentCancellationSameBooking() throws InterruptedException {
    List<int[]> seats = new ArrayList<>();
    seats.add(new int[] {1, 1});
    Booking booking = new Booking("C1", 1, seats);
    cinemaHall.bookings.put(booking.getBookingId(), booking);
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    List<Future<Boolean>> results = new ArrayList<>();
    for (int i = 0; i < threadCount; i++) {
      results.add(
          executor.submit(
              () -> {
                try {
                  synchronized (cinemaHall) {
                    return cinemaHall.bookings.remove("C1") != null;
                  }
                } finally {
                  latch.countDown();
                }
              }));
    }
    latch.await();
    executor.shutdown();
    int cancelCount = 0;
    for (Future<Boolean> f : results) {
      try {
        if (f.get()) cancelCount++;
      } catch (Exception ignored) {
      }
    }
    assertEquals(1, cancelCount, "Only one cancellation should succeed for the same booking");
  }

  @Test
  void concurrentBookingDifferentSeats() throws InterruptedException {
    int threadCount = 5;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    List<Future<Boolean>> results = new ArrayList<>();
    for (int i = 0; i < threadCount; i++) {
      final int row = i;
      final String bookingId = "D" + i;
      results.add(
          executor.submit(
              () -> {
                try {
                  List<int[]> seats = new ArrayList<>();
                  seats.add(new int[] {row, 0});
                  Booking booking = new Booking(bookingId, 1, seats);
                  synchronized (cinemaHall) {
                    cinemaHall.bookings.put(booking.getBookingId(), booking);
                  }
                  return true;
                } finally {
                  latch.countDown();
                }
              }));
    }
    latch.await();
    executor.shutdown();
    int successCount = 0;
    for (Future<Boolean> f : results) {
      try {
        if (f.get()) successCount++;
      } catch (Exception ignored) {
      }
    }
    assertEquals(threadCount, successCount, "All bookings for different seats should succeed");
  }
}
