package com.renfei.booking.cinema.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.renfei.booking.cinema.configuration.CinemaHallConfig;
import com.renfei.booking.cinema.controller.CinemaBookingSystem;
import com.renfei.booking.cinema.exception.ErrorMessageConstants;
import com.renfei.booking.cinema.exception.ErrorMessageStore;
import com.renfei.booking.cinema.model.Booking;
import com.renfei.booking.cinema.strategy.SeatingStrategy;
import com.renfei.booking.cinema.strategy.impl.GICCustomSeatingStrategy;
import com.renfei.booking.cinema.strategy.impl.GICDefaultSeatingStrategy;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

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

    List<int[]> defaultSeats =
        defaultStrategy.selectSeats(
            cinemaHall.seatingMap, 10, 10, 1, cinemaHall.getDefaultColPriority(), null);
    assertEquals(1, defaultSeats.size());
    assertArrayEquals(new int[] {0, 4}, defaultSeats.get(0));
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
    assertArrayEquals(new int[] {0, 4}, defaultSeats.get(0));
    assertArrayEquals(new int[] {0, 5}, defaultSeats.get(1));
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
    void reallocateSeats_SuccessfulReallocation() {
        Booking booking = cinemaHall.bookDefault(1);
        Booking reallocatedBooking = cinemaHall.reallocateSeats(booking, 1, "C5");
        List<int[]> selectedSeats = reallocatedBooking.getSelectedSeats();
        System.out.println(cinemaHall.displaySeatingMap(reallocatedBooking.getBookingId()));

        assertEquals(2, selectedSeats.get(0)[0]); // Row C is index 2
        assertEquals(4, selectedSeats.get(0)[1]); // Seat 5 is index 4

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
    //    Booking newBooking = hall.bookDefault(4);
    defaultBooking = hall.bookCustom(defaultBooking, 4, "B3");
    List<int[]> seats = defaultBooking.getSelectedSeats();
    System.out.println(hall.displaySeatingMap(defaultBooking.getBookingId()));
    // Should fill row 1 from col 2 to 4, then overflow to row 0
    System.out.println("seatsPerRow: " + seats.get(1).length);
    assertEquals(4, seats.size());
    assertEquals(1, hall.seatingMap[1][2]);
    assertEquals(1, hall.seatingMap[1][3]);
    assertEquals(1, hall.seatingMap[1][4]);
    assertEquals(1, hall.seatingMap[2][2]);
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
  void bookWithMaxTickets() {
    int max = 5; // For this test, use 5 as a practical max
    List<int[]> seats = new ArrayList<>();
    for (int i = 0; i < max; i++) seats.add(new int[] {0, i});
    Booking booking = new Booking("GIC0001", max, seats);
    cinemaHall.bookings.put(booking.getBookingId(), booking);
    assertTrue(cinemaHall.bookings.containsKey("GIC0001"));
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
  void testHandleCheckBookingsInvalidId() {
      //        ErrorMessageStore.clear();
      String simulatedInput = "TestMovie 10 10\n1\n1\n1\n\n2\nGIC0000\n3\n";
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
    assertEquals(
        "Error: Booking id not found: GIC0000",
        ErrorMessageStore.get(ErrorMessageConstants.BOOKING_ID_NOT_FOUND));
  }

    @Test
    void testPromptForTicketsBookingsAllBooked() {
        //        ErrorMessageStore.clear();
        String simulatedInput = "TestMovie 10 10\n1\n100\n\n1\n1\n\n3\n";
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
        assertEquals(
                "Sorry, all tickets have been booked.",
                ErrorMessageStore.get(ErrorMessageConstants.ALL_TICKETS_BOOKED));
    }

    @Test
    void testPromptForTicketsBookingsOverBooked() {
        //        ErrorMessageStore.clear();
        String simulatedInput = "TestMovie 10 10\n1\n99\n\n1\n3\n\n3\n";
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
        assertEquals(
                "Sorry, there are only 1 seats available.\n",
                ErrorMessageStore.get(ErrorMessageConstants.NOT_ENOUGH_SEATS));
    }

    @Test
    void testPromptForTicketsBookingsNumberFormatError() {
        //        ErrorMessageStore.clear();
        String simulatedInput = "TestMovie 10 10\n1\nA\n\n\n3\n";
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
        assertEquals(
                "Invalid input. Please enter a number.",
                ErrorMessageStore.get(ErrorMessageConstants.INVALID_TICKET_INPUT));
    }

  @Test
  void testMovieTitleNullOrEmpty() {
    assertDoesNotThrow(() -> new CinemaHall(null, 5, 5));
    assertDoesNotThrow(() -> new CinemaHall("   ", 5, 5));
    assertEquals(
        "Movie title cannot be empty.", ErrorMessageStore.get(ErrorMessageConstants.EMPTY_TITLE));
  }

  @Test
  void testRowsOrSeatsExceedMax() {
    int maxRows = CinemaHallConfig.MAX_ROWS;
    int maxSeats = CinemaHallConfig.MAX_SEATS_PER_ROW;
    assertDoesNotThrow(() -> new CinemaHall("Test", maxRows + 1, 5));
    assertDoesNotThrow(() -> new CinemaHall("Test", 5, maxSeats + 1));
    String msg = "Max rows is " + maxRows + ", max seats per row is " + maxSeats + ".";
    assertEquals(msg, ErrorMessageStore.get(ErrorMessageConstants.DIMENSION_EXCEEDS_MAX));
  }

  @Test
  void testRowsOrSeatsBelowMin() {
    int minRows = CinemaHallConfig.MIN_ROWS;
    int minSeats = CinemaHallConfig.MIN_SEATS_PER_ROW;
    assertDoesNotThrow(() -> new CinemaHall("Test", minRows - 1, 5));
    assertDoesNotThrow(() -> new CinemaHall("Test", 5, minSeats - 1));
    assertEquals(
        "Rows and seats per row must be at least 1.",
        ErrorMessageStore.get(ErrorMessageConstants.DIMENSION_BELOW_MIN));
  }

  @Test
  void testStartPositionOutOfBoundsAndInvalidReturnsEmptyList() {
    SeatingStrategy strategy = new GICCustomSeatingStrategy();
    int[][] seatingMap = new int[5][5];
    int totalRows = 5;
    int seatsPerRow = 5;
    int numTickets = 2;
    int[] defaultColPriority = {0, 1, 2, 3, 4};

    // Row out of bounds
    List<int[]> result1 =
        strategy.selectSeats(
            seatingMap, totalRows, seatsPerRow, numTickets, defaultColPriority, "Z1");
    assertTrue(result1.isEmpty());

    // Column out of bounds
    List<int[]> result2 =
        strategy.selectSeats(
            seatingMap, totalRows, seatsPerRow, numTickets, defaultColPriority, "A6");
    assertTrue(result2.isEmpty());

    List<int[]> result3 =
        strategy.selectSeats(
            seatingMap, totalRows, seatsPerRow, numTickets, defaultColPriority, null);
    assertTrue(result3.isEmpty());
  }

    @Test
    void bookCustom_Overbooking_ShouldReturnNullAndSetError() {
        // Fill all but 2 seats
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                if (r == 9 && (c == 8 || c == 9)) continue;
                cinemaHall.seatingMap[r][c] = 1;
            }
        }
        Booking booking = new Booking("B9", 1, List.of(new int[] {9, 8}));
        cinemaHall.bookings.put(booking.getBookingId(), booking);
        // Only 1 seat left, try to book 2
        Booking result = cinemaHall.bookCustom(booking, 3, "J10");
        assertNull(result);
        assertEquals(
                "Requested more tickets than available (custom).",
                ErrorMessageStore.get(ErrorMessageConstants.OVERBOOKING_CUSTOM));
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
    assertEquals(
        "Please enter a positive number.",
        ErrorMessageStore.get(ErrorMessageConstants.NEGATIVE_TICKET_COUNT));
  }

  @Test
  void reallocateSeats_ToInvalidSeat_ShouldReturnNull() {
    Booking booking = cinemaHall.bookDefault(1);
    Booking reallocatedBooking = cinemaHall.reallocateSeats(booking, 1, "Z99");

    assertEquals(
        "Could not re-reserve seats with the starting position: Z99. Please try again.",
        ErrorMessageStore.get(ErrorMessageConstants.REALLOCATION_FAILED));
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
                  Booking booking = cinemaHall.bookDefault(1);
                  System.out.println(cinemaHall.displaySeatingMap(booking.getBookingId()));
                  return booking != null;
                } finally {
                  latch.countDown();
                }
              }));
    }
    latch.await();
    executor.shutdown();
    int successCount = 0;
    for (Future<Boolean> f : results) {
      if (f.get()) successCount++;
    }
    assertEquals(5, successCount, "All bookings should succeed for different seats");
    // Check that the seat booked follows defaultColPriority strategy
    int[] colPriority = cinemaHall.getDefaultColPriority();
    int bookedCol = -1;
    for (int col : colPriority) {
      if (cinemaHall.seatingMap[0][col] == 1) {
        bookedCol = col;
        assertEquals(
            1,
            cinemaHall.seatingMap[0][bookedCol],
            "Booked seat should follow defaultColPriority strategy (row 0, preferred column)");
      }
    }
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
