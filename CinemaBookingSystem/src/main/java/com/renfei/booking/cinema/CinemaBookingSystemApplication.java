package com.renfei.booking.cinema;

import com.renfei.booking.cinema.service.CinemaBookingSystem;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CinemaBookingSystemApplication implements CommandLineRunner {

	public static void main(String[] args) {
        SpringApplication.run(CinemaBookingSystemApplication.class, args);


	}

    @Override
    public void run(String... args) throws Exception {
        // You can add any initialization code here if needed
        new CinemaBookingSystem().start();
    }
}
