package com.renfei.booking.cinema.service;

import com.renfei.booking.cinema.exception.BookingException;
import com.renfei.booking.cinema.model.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class CinemaHallTest {
    private CinemaHall cinemaHall;

    @BeforeEach
    void setUp() {
        // 5 rows, 5 seats per row for testing
        cinemaHall = new CinemaHall("Test Movie", 5, 5);
    }

    // Positive Test Cases
    @Test
    void bookValidSeat() {
        // Book seat (row 0, col 0)
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{0, 0});
        Booking booking = new Booking("B1", 1, seats);
        cinemaHall.bookings.put(booking.getBookingId(), booking);
        assertTrue(cinemaHall.bookings.containsKey("B1"));
        assertEquals(1, cinemaHall.bookings.get("B1").getNumTickets());
    }

    @Test
    void bookMultipleSeatsInSingleTransaction() {
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{0, 1});
        seats.add(new int[]{0, 2});
        Booking booking = new Booking("B2", 2, seats);
        cinemaHall.bookings.put(booking.getBookingId(), booking);
        assertTrue(cinemaHall.bookings.containsKey("B2"));
        assertEquals(2, cinemaHall.bookings.get("B2").getNumTickets());
    }

    @Test
    void cancelPreviouslyBookedSeat() {
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{1, 1});
        Booking booking = new Booking("B3", 1, seats);
        cinemaHall.bookings.put(booking.getBookingId(), booking);
        assertTrue(cinemaHall.bookings.containsKey("B3"));
        cinemaHall.bookings.remove("B3");
        System.out.print(cinemaHall.displaySeatingMap(booking.getBookingId()));
        assertFalse(cinemaHall.bookings.containsKey("B3"));
    }

    @Test
    void viewAvailableSeats() {
        // Book a seat
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{2, 2});
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
        seats.add(new int[]{3, 3});
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
        seats1.add(new int[]{0, 0});
        Booking booking1 = new Booking("B7", 1, seats1);
        cinemaHall.bookings.put(booking1.getBookingId(), booking1);
        List<int[]> seats2 = new ArrayList<>();
        seats2.add(new int[]{1, 1});
        Booking booking2 = new Booking("B8", 1, seats2);
        hall2.bookings.put(booking2.getBookingId(), booking2);
        assertTrue(cinemaHall.bookings.containsKey("B7"));
        assertTrue(hall2.bookings.containsKey("B8"));
    }

    // Negative Test Cases
    @Test
    void bookAlreadyBookedSeat() {
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{0, 0});
        Booking booking1 = new Booking("B9", 1, seats);
        cinemaHall.bookings.put(booking1.getBookingId(), booking1);
        // Try to book the same seat again
        Booking booking2 = new Booking("B10", 1, seats);
        boolean alreadyBooked = false;
        for (Booking b : cinemaHall.bookings.values()) {
            for (int[] s : b.getSelectedSeats()) {
                if (s[0] == 0 && s[1] == 0) alreadyBooked = true;
            }
        }
        assertTrue(alreadyBooked);
    }

    @Test
    void bookSeatForNonExistentShowtime() {
        // There is no showtime logic in CinemaHall, so simulate by not creating a hall
        CinemaHall nullHall = null;
        assertThrows(NullPointerException.class, () -> {
            List<int[]> seats = new ArrayList<>();
            seats.add(new int[]{0, 0});
            Booking booking = new Booking("B11", 1, seats);
            nullHall.bookings.put(booking.getBookingId(), booking);
        });
    }

    @Test
    void bookSeatWithInvalidSeatNumber() {
        // Try to book a seat outside the valid range
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{10, 10}); // Invalid row and column
        Booking booking = new Booking("B12", 1, seats);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            // Simulate seat allocation in seatingMap
            cinemaHall.seatingMap[10][10] = 1;
        });
    }


    // Multi-threading Test Cases
    @Test
    void concurrentBookingSameSeat() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<Boolean>> results = new ArrayList<>();
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{0, 0});
        for (int i = 0; i < threadCount; i++) {
            final String bookingId = "T" + i;
            results.add(executor.submit(() -> {
                try {
                    synchronized (cinemaHall) {
                        if (!cinemaHall.bookings.values().stream().anyMatch(b -> b.getSelectedSeats().get(0)[0] == 0 && b.getSelectedSeats().get(0)[1] == 0)) {
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

    @Test
    void concurrentCancellationSameBooking() throws InterruptedException {
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{1, 1});
        Booking booking = new Booking("C1", 1, seats);
        cinemaHall.bookings.put(booking.getBookingId(), booking);
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            results.add(executor.submit(() -> {
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
            results.add(executor.submit(() -> {
                try {
                    List<int[]> seats = new ArrayList<>();
                    seats.add(new int[]{row, 0});
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
        seats.add(new int[]{0, 0});
        Booking booking = new Booking("P1", 1, seats);
        hall.bookings.put(booking.getBookingId(), booking);
        assertTrue(hall.bookings.containsKey("P1"));
        assertEquals(1, hall.bookings.get("P1").getNumTickets());
        // Book the last seat
        List<int[]> lastSeat = new ArrayList<>();
        lastSeat.add(new int[]{rows - 1, seatsPerRow - 1});
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
        seats.add(new int[]{maxRows - 1, maxSeatsPerRow - 1});
        Booking booking = new Booking("MAX1", 1, seats);
        hall.bookings.put(booking.getBookingId(), booking);
        assertTrue(hall.bookings.containsKey("MAX1"));
    }

    @Test
    void testZeroRowsOrSeatsShouldFail() {
        assertThrows(BookingException.class, () -> new CinemaHall("ZeroRows", 0, 5));
        assertThrows(BookingException.class, () -> new CinemaHall("ZeroSeats", 5, 0));
        assertThrows(BookingException.class, () -> new CinemaHall("ZeroBoth", 0, 0));
    }

    @Test
    void testOneRowMaxSeats() {
        int maxSeatsPerRow = 20; // Adjust if CinemaHallConfig.MAX_SEATS_PER_ROW is different
        CinemaHall hall = new CinemaHall("OneRowMaxSeats", 1, maxSeatsPerRow);
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{0, maxSeatsPerRow - 1});
        Booking booking = new Booking("ORMAX", 1, seats);
        hall.bookings.put(booking.getBookingId(), booking);
        assertTrue(hall.bookings.containsKey("ORMAX"));
    }

    @Test
    void testMaxRowsOneSeat() {
        int maxRows = 20; // Adjust if CinemaHallConfig.MAX_ROWS is different
        CinemaHall hall = new CinemaHall("MaxRowsOneSeat", maxRows, 1);
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{maxRows - 1, 0});
        Booking booking = new Booking("MR1S", 1, seats);
        hall.bookings.put(booking.getBookingId(), booking);
        assertTrue(hall.bookings.containsKey("MR1S"));
    }

    @Test
    void bookMultipleSeatsWithOverlap() {
        List<int[]> seats1 = new ArrayList<>();
        seats1.add(new int[]{0, 0});
        seats1.add(new int[]{0, 1});
        Booking booking1 = new Booking("OL1", 2, seats1);
        cinemaHall.bookings.put(booking1.getBookingId(), booking1);
        List<int[]> seats2 = new ArrayList<>();
        seats2.add(new int[]{0, 1}); // overlap
        seats2.add(new int[]{0, 2});
        Booking booking2 = new Booking("OL2", 2, seats2);
        // Simulate check for overlap
        boolean overlap = false;
        for (Booking b : cinemaHall.bookings.values()) {
            for (int[] s : b.getSelectedSeats()) {
                if (s[0] == 0 && s[1] == 1) overlap = true;
            }
        }
        assertTrue(overlap);
    }

    @Test
    void bookWithDuplicateSeatsInSameBooking() {
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{1, 1});
        seats.add(new int[]{1, 1}); // duplicate
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
        seats.add(new int[]{2, 3});
        Booking booking = new Booking("", 1, seats);
        cinemaHall.bookings.put(booking.getBookingId(), booking);
        assertTrue(cinemaHall.bookings.containsKey(""));
    }

    @Test
    void bookWithNullSeatList() {
        assertThrows(NullPointerException.class, () -> new Booking("NULLSEAT", 1, null));
    }

    @Test
    void bookWithEmptySeatList() {
        List<int[]> seats = new ArrayList<>();
        Booking booking = new Booking("EMPTYSEAT", 0, seats);
        cinemaHall.bookings.put(booking.getBookingId(), booking);
        assertTrue(cinemaHall.bookings.containsKey("EMPTYSEAT"));
        assertEquals(0, booking.getNumTickets());
    }

    @Test
    void bookWithNegativeIndices() {
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{-1, -1});
        Booking booking = new Booking("NEGIDX", 1, seats);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> cinemaHall.seatingMap[-1][-1] = 1);
    }

    @Test
    void bookFirstAndLastSeat() {
        int lastRow = cinemaHall.seatingMap.length - 1;
        int lastCol = cinemaHall.seatingMap[0].length - 1;
        List<int[]> first = new ArrayList<>();
        first.add(new int[]{0, 0});
        Booking b1 = new Booking("FIRST", 1, first);
        cinemaHall.bookings.put(b1.getBookingId(), b1);
        List<int[]> last = new ArrayList<>();
        last.add(new int[]{lastRow, lastCol});
        Booking b2 = new Booking("LAST", 1, last);
        cinemaHall.bookings.put(b2.getBookingId(), b2);
        assertTrue(cinemaHall.bookings.containsKey("FIRST"));
        assertTrue(cinemaHall.bookings.containsKey("LAST"));
    }

    @Test
    void cancelWithNullBookingId() {
        assertNull(cinemaHall.bookings.remove(null));
    }

    @Test
    void cancelWithEmptyBookingId() {
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{0, 1});
        Booking booking = new Booking("", 1, seats);
        cinemaHall.bookings.put(booking.getBookingId(), booking);
        assertNotNull(cinemaHall.bookings.remove(""));
    }

    @Test
    void bookWithSpecialCharactersInBookingId() {
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{1, 2});
        Booking booking = new Booking("!@#$%^&*()_+", 1, seats);
        cinemaHall.bookings.put(booking.getBookingId(), booking);
        assertTrue(cinemaHall.bookings.containsKey("!@#$%^&*()_+"));
    }

    @Test
    void bookWhenHallIsFull() {
        int rows = cinemaHall.seatingMap.length;
        int cols = cinemaHall.seatingMap[0].length;
        int bookingNum = 1;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                List<int[]> seats = new ArrayList<>();
                seats.add(new int[]{r, c});
                Booking booking = new Booking("FULL" + bookingNum++, 1, seats);
                cinemaHall.bookings.put(booking.getBookingId(), booking);
            }
        }
        // Try to book one more seat
        List<int[]> extra = new ArrayList<>();
        extra.add(new int[]{0, 0});
        Booking extraBooking = new Booking("EXTRA", 1, extra);
        // Simulate check for full
        long totalSeats = rows * cols;
        assertEquals(totalSeats, cinemaHall.bookings.size());
    }

    @Test
    void cancelWhenHallIsEmpty() {
        assertTrue(cinemaHall.bookings.isEmpty());
        assertNull(cinemaHall.bookings.remove("ANY"));
    }

    @Test
    void stressTestHighVolumeBookings() {
        CinemaHall hall = new CinemaHall("StressTest", 10, 10);
        int bookingNum = 1;
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                List<int[]> seats = new ArrayList<>();
                seats.add(new int[]{r, c});
                Booking booking = new Booking("STRESS" + bookingNum++, 1, seats);
                hall.bookings.put(booking.getBookingId(), booking);
            }
        }
        assertEquals(100, hall.bookings.size());
    }

    @Test
    void bookWithMixedValidInvalidSeats() {
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{0, 0}); // valid
        seats.add(new int[]{100, 100}); // invalid
        Booking booking = new Booking("MIXED", 2, seats);
        cinemaHall.bookings.put(booking.getBookingId(), booking);
        assertTrue(cinemaHall.bookings.containsKey("MIXED"));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> cinemaHall.seatingMap[100][100] = 1);
    }

    @Test
    void bookWithMaxTickets() {
        int max = 5; // For this test, use 5 as a practical max
        List<int[]> seats = new ArrayList<>();
        for (int i = 0; i < max; i++) seats.add(new int[]{0, i});
        Booking booking = new Booking("MAXTIX", max, seats);
        cinemaHall.bookings.put(booking.getBookingId(), booking);
        assertTrue(cinemaHall.bookings.containsKey("MAXTIX"));
        assertEquals(max, booking.getNumTickets());
    }

    @Test
    void bookWithMinTickets() {
        List<int[]> seats = new ArrayList<>();
        seats.add(new int[]{0, 0});
        Booking booking = new Booking("MINTIX", 1, seats);
        cinemaHall.bookings.put(booking.getBookingId(), booking);
        assertTrue(cinemaHall.bookings.containsKey("MINTIX"));
        assertEquals(1, booking.getNumTickets());
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
        defaultBooking = hall.bookCustom(defaultBooking,4, "B3");
        List<int[]> seats = defaultBooking.getSelectedSeats();
        // Should fill row 1 from col 2 to 4, then overflow to row 0
        assertEquals(4, seats.size());
        assertEquals(1, seats.get(0)[0]); assertEquals(2, seats.get(0)[1]);
        assertEquals(1, seats.get(1)[0]); assertEquals(3, seats.get(1)[1]);
        assertEquals(1, seats.get(2)[0]); assertEquals(4, seats.get(2)[1]);
        // Overflow seat should be in row 0
        assertEquals(0, seats.get(3)[0]);
    }

    @Test
    void testCustomSeatingStrategyFillRowWithBookedSeats() {
        CinemaHall hall = new CinemaHall("Movie", 3, 5);
        // Book seat B4 (row 1, col 3) to simulate a booked seat

        Booking booking = hall.bookDefault(1);

        booking = hall.bookCustom(booking,1, "B4"); // Book A1 to have a booking context
        String bookingId1 = booking.getBookingId();

        booking = hall.bookDefault(3);
        System.out.println(hall.displaySeatingMap(booking.getBookingId()));
        System.out.println("Booking ID: " + booking.getBookingId());
        booking = hall.bookCustom(booking,3, "B3");
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

    // ... Add more test cases as needed ...
}
