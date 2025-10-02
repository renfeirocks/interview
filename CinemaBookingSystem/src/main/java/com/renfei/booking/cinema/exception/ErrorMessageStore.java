package com.renfei.booking.cinema.exception;

import java.util.HashMap;

public class ErrorMessageStore {
  private static final HashMap<String, String> errorMessages = new HashMap<>();

  public static HashMap<String, String> getErrorMessages() {
    return errorMessages;
  }

  public static void put(String key, String value) {
    errorMessages.put(key, value);
  }

  public static String get(String key) {
    return errorMessages.get(key);
  }
}
