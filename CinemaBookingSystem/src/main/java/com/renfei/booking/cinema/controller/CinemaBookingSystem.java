package com.renfei.booking.cinema.controller;

import com.renfei.booking.cinema.model.Booking;
import com.renfei.booking.cinema.model.MovieInput;
import com.renfei.booking.cinema.service.CinemaHall;
import java.util.Scanner;

public class CinemaBookingSystem {
  private final Scanner scanner = new Scanner(System.in);
  private CinemaHall hall;
  private volatile boolean running = true;

  public void start() {
    if (!initializeSystem()) {
      System.out.println("Failed to initialize system. Exiting.");
      return;
    }
    while (running) {
      displayMenu();
      handleMenuSelection(scanner.nextLine().trim());
    }
  }

  private boolean initializeSystem() {
    System.out.println(
        "Please define movie title and seating map in [Title] [Row] [SeatsPerRow] format:");
    System.out.print("> ");
    MovieInput input = parseMovieInput(scanner.nextLine().trim());
    if (input == null) {
      System.out.println("Invalid format. Expected: [Title] [Row] [SeatsPerRow].");
      return false;
    }
    try {
      hall = new CinemaHall(input.title, input.rows, input.seatsPerRow);
      return true;
    } catch (IllegalArgumentException e) {
      System.out.println("Error in dimensions: " + e.getMessage());
      return false;
    }
  }

  private MovieInput parseMovieInput(String line) {
    String[] parts = line.split(" ");
    if (parts.length < 3) return null;
    String title = String.join(" ", java.util.Arrays.copyOfRange(parts, 0, parts.length - 2));
    try {
      int rows = Integer.parseInt(parts[parts.length - 2]);
      int seatsPerRow = Integer.parseInt(parts[parts.length - 1]);
      return new MovieInput(title, rows, seatsPerRow);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private void displayMenu() {
    System.out.printf(
        "\nWelcome to GIC Cinemas\n[1] Book tickets for %s (%d seats available)\n[2] Check bookings\n[3] Exit\nPlease enter your selection:\n> ",
        hall.getMovieTitle(), hall.getAvailableSeatsCount());
  }

  private void handleMenuSelection(String selection) {
    switch (selection) {
      case "1":
        handleBookTickets();
        break;
      case "2":
        handleCheckBookings();
        break;
      case "3":
        handleExit();
        break;
      default:
        System.out.println("Invalid selection. Please enter 1, 2, or 3.");
    }
  }

  private void handleBookTickets() {
    int tickets = promptForTickets();
    if (tickets < 0) return;

    Booking booking = hall.bookDefault(tickets);
    if (booking == null) {
      System.out.println("Error: Could not reserve seats. Returning to main menu.");
      return;
    }
    confirmBooking(booking, tickets);
  }

  private int promptForTickets() {
    while (true) {
      System.out.println("Enter number of tickets to book, or blank to go back:");
      System.out.print("> ");
      String input = scanner.nextLine().trim();
      if (input.isEmpty()){
          return -1;
      }
      try {
        int tickets = Integer.parseInt(input);
        if (tickets <= 0) {
          System.out.println("Please enter a positive number.");
        } else if (tickets > hall.getAvailableSeatsCount()) {
          System.out.printf("Sorry, only %d seats available.\n", hall.getAvailableSeatsCount());
        } else {
          return tickets;
        }
      } catch (NumberFormatException e) {
        System.out.println("Invalid input. Please enter a number.");
      }
    }
  }

    private void confirmBooking(Booking booking, int tickets) {
        System.out.printf(
                "Successfully reserved %d %s tickets.\nBooking id: %s\nSelected seats:\n%s",
                tickets,
                hall.getMovieTitle(),
                booking.getBookingId(),
                hall.displaySeatingMap(booking.getBookingId()));

        while (true) {
            System.out.println("Enter blank to accept, or new seating position:");
            System.out.print("> ");
            String newPos = scanner.nextLine().trim();
            if (newPos.isEmpty()) {
                System.out.printf("Booking id: %s confirmed.\n", booking.getBookingId());
                break;
            }
            booking = hall.reallocateSeats(booking, tickets, newPos);
//            if (!hall.reallocateSeats(booking, tickets, newPos)) {
//                System.out.println("Could not re-reserve seats. Please try again.");
//            }
        }
    }

  private void handleCheckBookings() {
    System.out.println("Enter booking id, or blank to go back:");
    System.out.print("> ");
    String id = scanner.nextLine().trim();
    if (id.isEmpty()) return;
    Booking booking = hall.getBooking(id);
    if (booking == null) {
      System.out.println("Error: Booking id not found: " + id);
    } else {
      System.out.printf("Booking id: %s\nSelected seats:\n%s", id, hall.displaySeatingMap(id));
    }
  }

  private void handleExit() {
    System.out.println("Thank you for using GIC Cinemas system. Bye!");
    running = false;
    scanner.close();
  }

//  public static void main(String[] args) {
//    new CinemaBookingSystem().start();
//  }
}
