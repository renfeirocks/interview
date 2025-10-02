package com.renfei.booking.cinema.controller;

import com.renfei.booking.cinema.exception.ErrorMessageConstants;
import com.renfei.booking.cinema.exception.ErrorMessageStore;
import com.renfei.booking.cinema.model.Booking;
import com.renfei.booking.cinema.model.MovieInput;
import com.renfei.booking.cinema.service.CinemaHall;
import java.io.InputStream;
import java.util.Scanner;

public class CinemaBookingSystem {
  private Scanner scanner;
  private CinemaHall hall;
  private volatile boolean running = true;


  public CinemaBookingSystem(InputStream inputStream){
      if(inputStream==null){
          scanner = new Scanner(System.in);

      }else{
          scanner = new Scanner(inputStream);
      }
  }

  public void start() {
    while (!initializeSystem()) {
      System.out.println("Failed to initialize system. Please try again.");
    }
    while (running) {
      displayMenu();
      String selection = scanner.nextLine().trim();
      handleMenuSelection(selection);
      if (!running) {
        break;
      }
    }
  }

  boolean initializeSystem() {
    System.out.println(
        "Please define movie title and seating map in [Title] [Row] [SeatsPerRow] format:");
    System.out.print("> ");
    MovieInput input = parseMovieInput(scanner.nextLine().trim());
    if (input == null) {
      String msg = "Invalid format. Expected: [Title] [Row] [SeatsPerRow].";
      System.out.println(msg);
      ErrorMessageStore.put(ErrorMessageConstants.INPUT_FORMAT_ERROR, msg);
      return false;
    }
    try {
      hall = new CinemaHall(input.title, input.rows, input.seatsPerRow);
      return true;
    } catch (Exception e) {
      String msg = "Error in dimensions: " + e.getMessage();
      System.out.println(msg);
      ErrorMessageStore.put(ErrorMessageConstants.DIMENSION_ERROR, msg);
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

  void handleMenuSelection(String selection) {
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
      String msg = "Error: Could not reserve seats. Returning to main menu.";
      System.out.println(msg);
      ErrorMessageStore.put(ErrorMessageConstants.BOOKING_FAILED, msg);
      return;
    }
    confirmBooking(booking, tickets);
  }

  private int promptForTickets() {
    while (true) {
      System.out.println("Enter number of tickets to book, or blank to go back:");
      System.out.print("> ");
      String input = scanner.nextLine().trim();
      if (input.isEmpty()) {
        return -1;
      }
      try {
        int tickets = Integer.parseInt(input);
        if (tickets <= 0) {
          String msg = "Please enter a positive number.";
          System.out.println(msg);
          ErrorMessageStore.put(ErrorMessageConstants.NEGATIVE_TICKET_COUNT, msg);
        } else if (hall.getAvailableSeatsCount() == 0){
          String msg = "Sorry, all tickets have been booked.";
          System.out.println(msg);
          ErrorMessageStore.put(ErrorMessageConstants.ALL_TICKETS_BOOKED, msg);
        } else if (tickets > hall.getAvailableSeatsCount()) {
          String msg = String.format("Sorry, there are only %d seats available.", hall.getAvailableSeatsCount());
          System.out.print(msg);
          ErrorMessageStore.put(ErrorMessageConstants.NOT_ENOUGH_SEATS, msg);
        } else {
          return tickets;
        }
      } catch (NumberFormatException e) {
        String msg = "Invalid input. Please enter a number.";
        System.out.println(msg);
        ErrorMessageStore.put(ErrorMessageConstants.INVALID_TICKET_INPUT, msg);
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
      String msg = "Error: Booking id not found: " + id;
      System.out.println(msg);
      ErrorMessageStore.put(ErrorMessageConstants.BOOKING_ID_NOT_FOUND, msg);
    } else {
      System.out.printf("Booking id: %s\nSelected seats:\n%s", id, hall.displaySeatingMap(id));
    }
  }

  void handleExit() {
    System.out.println("Thank you for using GIC Cinemas system. Bye!");
    running = false;
    scanner.close();
  }

}
